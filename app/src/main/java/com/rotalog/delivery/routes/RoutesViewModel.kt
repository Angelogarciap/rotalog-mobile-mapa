package com.rotalog.delivery.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotalog.delivery.data.DemoScenarioController
import com.rotalog.delivery.data.RouteRepository
import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoutesViewModel(
    private val repository: RouteRepository,
    private val demo: DemoScenarioController,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val selected = savedState.getStateFlow<String?>("stop", null)
    private val expanded = savedState.getStateFlow("expanded", false)
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    init {
        val restored = savedState.get<String>("scenario")?.let { value -> DemoScenario.entries.find { it.name == value } }
        if (restored != null && restored != demo.scenario.value) demo.selectScenario(restored)
        // An accepted demo is restorable, but accepting never receives cargo or starts the route.
        if (savedState.get<Boolean>("accepted") == true && demo.scenario.value == DemoScenario.ASSIGNED) {
            viewModelScope.launch { repository.route.value?.let { repository.acceptRoute(it.id) } }
        }
        savedState.get<String>("sync")?.let { value -> SyncStatus.entries.find { it.name == value }?.let(demo::simulateSync) }
    }

    private val content = combine(demo.scenario, repository.route, selected, expanded, message) { scenario, route, id, open, notice ->
        RoutesUiState(scenario, route, route?.stops?.find { it.id == id }?.id ?: route?.nextStop?.id, open, false, message = notice)
    }
    val uiState: StateFlow<RoutesUiState> = combine(content, busy, error) { state, loading, failure ->
        state.copy(loading = loading, error = failure)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RoutesUiState())

    fun selectStop(id: String) {
        if (repository.route.value?.stops?.none { it.id == id } != false) return
        savedState["stop"] = id
        savedState["expanded"] = true
    }
    fun setExpanded(value: Boolean) { savedState["expanded"] = value }
    fun nextStop() {
        savedState["stop"] = repository.route.value?.nextStop?.id
        savedState["expanded"] = false
    }
    fun selectScenario(scenario: DemoScenario) {
        savedState["scenario"] = scenario.name
        savedState["stop"] = null
        savedState["expanded"] = false
        savedState["accepted"] = false
        savedState["sync"] = SyncStatus.LOCAL.name
        message.value = null
        error.value = null
        demo.selectScenario(scenario)
    }
    fun resetScenario() { selectScenario(demo.scenario.value) }
    fun simulateSync(status: SyncStatus) {
        savedState["sync"] = status.name
        demo.simulateSync(status)
    }
    fun acceptRoute() {
        if (busy.value) return
        val id = repository.route.value?.id ?: return
        busy.value = true
        viewModelScope.launch {
            try {
                message.value = when (repository.acceptRoute(id)) {
                    AcceptRouteResult.Accepted -> {
                        savedState["accepted"] = true
                        "Aceite salvo nesta demonstração. Preparação e recebimento continuam pendentes."
                    }
                    AcceptRouteResult.AlreadyAccepted -> "O aceite desta rota já foi confirmado."
                    AcceptRouteResult.NotAvailable -> "A rota não está disponível para aceite."
                }
            } catch (exception: java.io.IOException) {
                error.value = "Não foi possível aceitar a rota. Tente novamente."
            } finally { busy.value = false }
        }
    }
    fun clearMessage() { message.value = null }
}
