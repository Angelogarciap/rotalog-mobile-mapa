package com.rotalog.delivery.routes

import com.rotalog.delivery.model.*

enum class DepartureStep(val label: String) { ACCEPTANCE("Aceite"), RECEIPT("Recebimento"), START("Início") }

data class CargoIssueDraft(
    val open: Boolean = false, val productId: String? = null,
    val reason: CargoIssueReason? = null, val description: String = "",
    val saving: Boolean = false, val error: String? = null
)

data class RoutesUiState(
    val departureStep: DepartureStep = DepartureStep.ACCEPTANCE,
    val issueDraft: CargoIssueDraft = CargoIssueDraft(),
    val scenario: DemoScenario = DemoScenario.ASSIGNED,
    val route: Route? = null,
    val selectedStopId: String? = null,
    val detailsOpen: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null
) {
    val cargoConfirmed: Boolean get() = route?.let { it.manifest.isNotEmpty() && it.manifest.all { item -> item.productId in it.checkedCargo } } == true
    fun canView(step: DepartureStep): Boolean = when (step) {
        DepartureStep.ACCEPTANCE -> true
        DepartureStep.RECEIPT -> route?.status?.let { it != RouteStatus.ASSIGNED } == true
        DepartureStep.START -> route?.cargoReceived == true
    }
    val selectedStop: Stop? get() = route?.stops?.find { it.id == selectedStopId } ?: route?.nextStop
}
