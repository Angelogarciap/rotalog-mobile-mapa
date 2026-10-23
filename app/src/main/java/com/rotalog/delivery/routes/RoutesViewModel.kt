package com.rotalog.delivery.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotalog.delivery.data.DemoScenarioController
import com.rotalog.delivery.data.RouteRepository
import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class RoutesViewModel(
    private val repository: RouteRepository,
    private val demo: DemoScenarioController,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val selected = savedState.getStateFlow<String?>("stop", null)
    private val detailsOpen = savedState.getStateFlow("detailsOpen", savedState.get<Boolean>("expanded") ?: false)
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val step = savedState.getStateFlow("departureStep", DepartureStep.ACCEPTANCE.name)
    private val draftOpen = savedState.getStateFlow("issueOpen", false)
    private val draftProduct = savedState.getStateFlow<String?>("issueProduct", null)
    private val draftReason = savedState.getStateFlow<String?>("issueReason", null)
    private val draftNote = savedState.getStateFlow("issueNote", "")
    private val issueSaving = MutableStateFlow(false)
    private val issueError = MutableStateFlow<String?>(null)
    private var generation = 0
    private var pendingOperation: Job? = null

    init {
        val restored = savedState.get<String>("scenario")?.let { value -> DemoScenario.entries.find { it.name == value } }
        if (restored != null && restored != demo.scenario.value) demo.selectScenario(restored)
        savedState.get<Route>("routeSnapshot")?.let { snapshot ->
            val migrated = if (savedState.get<Int>("departureFlowVersion") != 2 && !snapshot.cargoReceived && snapshot.status == RouteStatus.ACCEPTED) snapshot.copy(checkedCargo = emptySet()) else snapshot
            demo.restoreRoute(migrated)
            savedState["routeSnapshot"] = migrated
        }
        savedState["departureFlowVersion"] = 2
        // An accepted demo is restorable, but accepting never receives cargo or starts the route.
        if (savedState.get<Boolean>("accepted") == true && demo.scenario.value == DemoScenario.ASSIGNED) {
            pendingOperation = viewModelScope.launch { repository.route.value?.let { repository.acceptRoute(it.id) } }
        }
        savedState.get<String>("sync")?.let { value -> SyncStatus.entries.find { it.name == value }?.let(demo::simulateSync) }
        if (detailsOpen.value) {
            // The previous sheet could expand the next stop without persisting a marker selection.
            val route = repository.route.value
            val stopId = selected.value ?: route?.nextStop?.id
            if (stopId != null && route?.stops?.any { it.id == stopId } == true) savedState["stop"] = stopId
            else closeDetails()
        }
        viewModelScope.launch {
            var routeId = repository.route.value?.id
            var previousStatus = repository.route.value?.status
            repository.route.collect { route ->
                if (routeId != route?.id || (route?.status == RouteStatus.ASSIGNED && previousStatus != RouteStatus.ASSIGNED) || (selected.value != null && route?.stops?.none { it.id == selected.value } != false)) {
                    closeDetails()
                    savedState["departureStep"] = DepartureStep.ACCEPTANCE.name
                    clearIssueDraft()
                    invalidatePendingOperation()
                }
                routeId = route?.id
                previousStatus = route?.status
            }
        }
    }

    private val content = combine(demo.scenario, repository.route, selected, detailsOpen, message) { scenario, route, id, open, notice ->
        val validSelection = route?.stops?.find { it.id == id }?.id
        RoutesUiState(
            scenario = scenario, route = route,
            selectedStopId = validSelection ?: route?.nextStop?.id,
            detailsOpen = open && validSelection != null, loading = false, message = notice
        )
    }
    private val draft = combine(draftOpen, draftProduct, draftReason, draftNote) { open, product, reason, note ->
        CargoIssueDraft(open, product, CargoIssueReason.entries.find { it.name == reason }, note)
    }
    private val draftState = combine(draft, issueSaving, issueError) { form, saving, failure -> form.copy(saving = saving, error = failure) }
    private val operational = combine(content, busy, error) { state, loading, failure -> state.copy(loading = loading, error = failure) }
    val uiState: StateFlow<RoutesUiState> = combine(operational, step, draftState) { state, selectedStep, form ->
        val requested = DepartureStep.entries.find { it.name == selectedStep } ?: DepartureStep.ACCEPTANCE
        state.copy(departureStep = requested.takeIf { state.canView(it) } ?: DepartureStep.ACCEPTANCE, issueDraft = form)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RoutesUiState())

    fun selectDepartureStep(step: DepartureStep) {
        val route = repository.route.value
        val available = when (step) {
            DepartureStep.ACCEPTANCE -> true
            DepartureStep.RECEIPT -> route != null && route.status != RouteStatus.ASSIGNED
            DepartureStep.START -> route?.cargoReceived == true
        }
        if (available) savedState["departureStep"] = step.name
    }
    fun openLegacyDeparture(routeId: String): Boolean {
        val route = repository.route.value ?: return false
        if (route.id != routeId) return false
        selectDepartureStep(if (route.status == RouteStatus.ASSIGNED) DepartureStep.ACCEPTANCE else DepartureStep.RECEIPT)
        return true
    }
    fun previousDepartureStep() {
        val current = uiState.value.departureStep
        if (current != DepartureStep.ACCEPTANCE) selectDepartureStep(DepartureStep.entries[current.ordinal - 1])
    }
    fun setCargoConfirmation(checked: Boolean) = command { repository.setCargoConfirmation(it, checked) }
    fun openCargoIssue() { if (repository.route.value?.let { it.status == RouteStatus.ACCEPTED && !it.cargoReceived } == true) savedState["issueOpen"] = true }
    fun dismissCargoIssue() { if (!issueSaving.value) savedState["issueOpen"] = false }
    fun editCargoIssue(productId: String?, reason: CargoIssueReason?, description: String) {
        if (issueSaving.value) return
        savedState["issueProduct"] = productId
        savedState["issueReason"] = reason?.name
        savedState["issueNote"] = description.take(500)
        issueError.value = null
    }
    private fun clearIssueDraft() {
        savedState["issueOpen"] = false; savedState["issueProduct"] = null
        savedState["issueReason"] = null; savedState["issueNote"] = ""
        issueError.value = null
    }
    fun submitCargoIssue() {
        if (issueSaving.value || busy.value) return
        val product = draftProduct.value; val reason = CargoIssueReason.entries.find { it.name == draftReason.value }
        if (product == null || reason == null) { issueError.value = "Selecione o produto e o motivo."; return }
        val id = repository.route.value?.id ?: return
        val commandGeneration = generation
        issueSaving.value = true
        pendingOperation = viewModelScope.launch {
            try {
                val result = repository.reportCargoIssue(id, CargoIssue(product, reason, draftNote.value))
                if (generation != commandGeneration) return@launch
                if (result is RouteCommandResult.Rejected) issueError.value = result.reason
                else { saveSnapshot(); clearIssueDraft(); message.value = "Divergência salva localmente. Resolva com a equipe do CD antes de receber." }
            } catch (_: java.io.IOException) { if (generation == commandGeneration) issueError.value = "Não foi possível registrar. Tente novamente." }
            finally { if (generation == commandGeneration) issueSaving.value = false }
        }
    }
    private fun saveSnapshot() {
        savedState["routeSnapshot"] = repository.route.value
        savedState["sync"] = repository.route.value?.syncStatus?.name
    }
    fun selectStop(id: String) {
        if (repository.route.value?.stops?.none { it.id == id } != false) return
        savedState["stop"] = id
    }
    fun openDetails(id: String) {
        if (repository.route.value?.stops?.none { it.id == id } != false) return
        savedState["stop"] = id
        savedState["detailsOpen"] = true
    }
    fun closeDetails() { savedState["detailsOpen"] = false; savedState["expanded"] = false }
    fun nextStop() {
        savedState["stop"] = repository.route.value?.nextStop?.id
        savedState["detailsOpen"] = false
    }
    fun selectScenario(scenario: DemoScenario) {
        if (scenario != demo.scenario.value) resetToScenario(scenario)
    }
    private fun invalidatePendingOperation() {
        generation++
        // Cancel before replacing the fixture: a suspended command may otherwise target the same routeId.
        pendingOperation?.cancel()
        pendingOperation = null
        busy.value = false
        issueSaving.value = false
    }
    private fun resetToScenario(scenario: DemoScenario) {
        invalidatePendingOperation()
        savedState["departureStep"] = DepartureStep.ACCEPTANCE.name
        clearIssueDraft()
        savedState["scenario"] = scenario.name
        savedState["stop"] = null
        savedState["detailsOpen"] = false
        savedState["accepted"] = false
        savedState["routeSnapshot"] = null
        savedState["sync"] = SyncStatus.LOCAL.name
        message.value = null
        error.value = null
        demo.selectScenario(scenario)
    }
    fun resetScenario() { resetToScenario(demo.scenario.value) }
    fun simulateSync(status: SyncStatus) {
        savedState["sync"] = status.name
        demo.simulateSync(status)
    }
    fun acceptRoute() {
        if (busy.value || issueSaving.value) return
        val id = repository.route.value?.id ?: return
        busy.value = true
        val commandGeneration = generation
        pendingOperation = viewModelScope.launch {
            try {
                val result = repository.acceptRoute(id)
                if (generation != commandGeneration) return@launch
                message.value = when (result) {
                    AcceptRouteResult.Accepted -> {
                        savedState["accepted"] = true
                        savedState["routeSnapshot"] = repository.route.value
                        savedState["sync"] = repository.route.value?.syncStatus?.name
                        selectDepartureStep(DepartureStep.RECEIPT)
                        "Aceite salvo nesta demonstração. Confira a carga antes de receber."
                    }
                    AcceptRouteResult.AlreadyAccepted -> "O aceite desta rota já foi confirmado."
                    AcceptRouteResult.NotAvailable -> "A rota não está disponível para aceite."
                }
            } catch (exception: java.io.IOException) {
                if (generation == commandGeneration) error.value = "Não foi possível aceitar a rota. Tente novamente."
            } finally { if (generation == commandGeneration) busy.value = false }
        }
    }
    private fun command(success: String? = null, afterSuccess: () -> Unit = {}, action: suspend (String) -> RouteCommandResult) {
        if (busy.value || issueSaving.value) return
        val id = repository.route.value?.id ?: return
        error.value = null
        message.value = null
        busy.value = true
        val commandGeneration = generation
        pendingOperation = viewModelScope.launch {
            try {
                val result = action(id)
                if (generation != commandGeneration) return@launch
                when (result) {
                    is RouteCommandResult.Rejected -> error.value = result.reason
                    else -> {
                        savedState["routeSnapshot"] = repository.route.value
                        savedState["sync"] = repository.route.value?.syncStatus?.name
                        afterSuccess()
                        if (success != null) message.value = success
                    }
                }
            } catch (_: java.io.IOException) { if (generation == commandGeneration) error.value = "Não foi possível salvar. Tente novamente." }
            finally { if (generation == commandGeneration) busy.value = false }
        }
    }
    fun setPreparationCheck(check: PreparationCheck, checked: Boolean) = command { repository.setPreparationCheck(it, check, checked) }
    fun confirmPreparation() = command("Checklist confirmado nesta demonstração.", action = repository::confirmPreparation)
    fun checkCargo(productId: String, checked: Boolean) = command { repository.checkCargo(it, productId, checked) }
    fun reportCargoIssue(issue: CargoIssue) = command("Divergência salva localmente. Resolva com a equipe do CD antes de receber.") { repository.reportCargoIssue(it, issue) }
    fun resolveCargoIssue(productId: String) = command("Resolução registrada na demonstração. Confira novamente toda a carga, incluindo o produto afetado.") { repository.resolveCargoIssue(it, productId) }
    fun confirmReceipt() = command("Recebimento confirmado nesta demonstração.", { selectDepartureStep(DepartureStep.START) }, repository::confirmReceipt)
    fun startRoute() {
        command("Rota demonstrativa iniciada • sem GPS ou envio ao servidor.") {
            repository.startRoute(it).also { result -> if (result !is RouteCommandResult.Rejected) nextStop() }
        }
    }
    fun clearMessage() { message.value = null }
}
