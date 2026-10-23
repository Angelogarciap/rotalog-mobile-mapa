package com.rotalog.delivery.loading

import androidx.lifecycle.SavedStateHandle
import com.rotalog.delivery.data.*
import com.rotalog.delivery.model.*
import com.rotalog.delivery.routes.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DepartureStateTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun successfulCommandsAdvanceButReviewDoesNotUndoOperations() = runTest {
        val repo = DemoRouteRepository(); val saved = SavedStateHandle(); val vm = RoutesViewModel(repo, repo, saved)
        vm.selectDepartureStep(DepartureStep.START)
        assertEquals(DepartureStep.ACCEPTANCE, vm.uiState.value.departureStep)
        vm.acceptRoute()
        assertEquals(DepartureStep.RECEIPT, vm.uiState.value.departureStep)
        vm.confirmReceipt()
        assertEquals(DepartureStep.RECEIPT, vm.uiState.value.departureStep)
        vm.setCargoConfirmation(true); vm.confirmReceipt()
        assertEquals(DepartureStep.START, vm.uiState.value.departureStep)
        assertEquals(RouteStatus.ACCEPTED, repo.route.value!!.status)
        vm.previousDepartureStep()
        assertEquals(DepartureStep.RECEIPT, vm.uiState.value.departureStep)
        vm.previousDepartureStep()
        assertEquals(DepartureStep.ACCEPTANCE, vm.uiState.value.departureStep)
        assertTrue(repo.route.value!!.cargoReceived)
        val fresh = DemoRouteRepository(); val restored = RoutesViewModel(fresh, fresh, saved)
        assertEquals(DepartureStep.ACCEPTANCE, restored.uiState.value.departureStep)
        assertTrue(fresh.route.value!!.cargoReceived)
        restored.resetScenario()
        assertEquals(DepartureStep.ACCEPTANCE, restored.uiState.value.departureStep)
        assertFalse(fresh.route.value!!.cargoReceived)
    }
    @Test fun legacyPartialConfirmationIsClearedButCustodyAndIssuesRemain() = runTest {
        val old = DemoRoutes.assigned().copy(status = RouteStatus.ACCEPTED, checkedCargo = setOf("oleo"), cargoIssues = listOf(CargoIssue("arroz", CargoIssueReason.MISSING, "")))
        val repo = DemoRouteRepository(); val vm = RoutesViewModel(repo, repo, SavedStateHandle(mapOf("routeSnapshot" to old)))
        assertTrue(vm.uiState.value.route!!.checkedCargo.isEmpty())
        assertEquals(old.cargoIssues, vm.uiState.value.route!!.cargoIssues)
        val received = old.copy(cargoReceived = true)
        val second = DemoRouteRepository(); RoutesViewModel(second, second, SavedStateHandle(mapOf("routeSnapshot" to received)))
        assertTrue(second.route.value!!.cargoReceived)
        assertEquals(received.checkedCargo, second.route.value!!.checkedCargo)
    }
    @Test fun legacyDestinationsValidateRouteAndOpenOnlyAnAvailableStep() = runTest {
        val repo = DemoRouteRepository(); val vm = RoutesViewModel(repo, repo, SavedStateHandle())
        assertFalse(vm.openLegacyDeparture("invalid"))
        assertTrue(vm.openLegacyDeparture(repo.route.value!!.id))
        assertEquals(DepartureStep.ACCEPTANCE, vm.uiState.value.departureStep)
        vm.acceptRoute(); vm.previousDepartureStep()
        assertTrue(vm.openLegacyDeparture(repo.route.value!!.id))
        assertEquals(DepartureStep.RECEIPT, vm.uiState.value.departureStep)
        vm.setCargoConfirmation(true); vm.confirmReceipt()
        assertFalse(vm.openLegacyDeparture("invalid"))
        assertEquals(DepartureStep.START, vm.uiState.value.departureStep)
        assertTrue(vm.openLegacyDeparture(repo.route.value!!.id))
        assertEquals(DepartureStep.RECEIPT, vm.uiState.value.departureStep)
    }
    @Test fun resettingSameRouteCancelsPendingAcceptanceBeforeRepositoryMutation() = runTest {
        val repo = DemoRouteRepository()
        val release = CompletableDeferred<Unit>()
        val slow = object : RouteRepository by repo {
            override suspend fun acceptRoute(routeId: String): AcceptRouteResult {
                release.await()
                return repo.acceptRoute(routeId)
            }
        }
        val vm = RoutesViewModel(slow, repo, SavedStateHandle())
        vm.acceptRoute()
        assertTrue(vm.uiState.value.loading)
        vm.resetScenario()
        assertFalse(vm.uiState.value.loading)
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(DemoRoutes.assigned(), repo.route.value)
        assertEquals(DepartureStep.ACCEPTANCE, vm.uiState.value.departureStep)
        assertFalse(vm.uiState.value.loading)
        assertNull(vm.uiState.value.message)
        assertNull(vm.uiState.value.error)
    }
    @Test fun resettingSameRouteCancelsPendingCargoReceiptAndIssueBeforeRepositoryMutation() = runTest {
        for (operation in listOf("cargo", "receipt", "issue")) {
            val repo = DemoRouteRepository()
            val release = CompletableDeferred<Unit>()
            val slow = object : RouteRepository by repo {
                override suspend fun setCargoConfirmation(routeId: String, checked: Boolean): RouteCommandResult {
                    if (operation == "cargo") release.await()
                    return repo.setCargoConfirmation(routeId, checked)
                }
                override suspend fun confirmReceipt(routeId: String): RouteCommandResult {
                    if (operation == "receipt") release.await()
                    return repo.confirmReceipt(routeId)
                }
                override suspend fun reportCargoIssue(routeId: String, issue: CargoIssue): RouteCommandResult {
                    if (operation == "issue") release.await()
                    return repo.reportCargoIssue(routeId, issue)
                }
            }
            val vm = RoutesViewModel(slow, repo, SavedStateHandle())
            vm.acceptRoute()
            when (operation) {
                "cargo" -> vm.setCargoConfirmation(true)
                "receipt" -> { vm.setCargoConfirmation(true); vm.confirmReceipt() }
                "issue" -> {
                    vm.openCargoIssue(); vm.editCargoIssue("oleo", CargoIssueReason.DAMAGED, "Rascunho antigo")
                    vm.submitCargoIssue()
                }
            }
            vm.resetScenario()
            assertFalse(vm.uiState.value.loading)
            assertFalse(vm.uiState.value.issueDraft.saving)
            // A new operation uses the same routeId. The old command must not mutate it.
            val id = repo.route.value!!.id
            repo.acceptRoute(id)
            if (operation == "receipt") repo.setCargoConfirmation(id, true)
            val expected = repo.route.value
            release.complete(Unit)
            advanceUntilIdle()
            assertEquals("Comando antigo: $operation", expected, repo.route.value)
            assertFalse(vm.uiState.value.loading)
            assertFalse(vm.uiState.value.issueDraft.saving)
            assertFalse(vm.uiState.value.issueDraft.open)
            assertNull(vm.uiState.value.issueDraft.productId)
            assertNull(vm.uiState.value.message)
            assertNull(vm.uiState.value.error)
        }
    }
    @Test fun delayedFailureFromPreviousScenarioDoesNotPolluteTheNewOne() = runTest {
        val repo = DemoRouteRepository()
        val slow = object : RouteRepository by repo {
            override suspend fun confirmReceipt(routeId: String): RouteCommandResult {
                kotlinx.coroutines.delay(100); throw java.io.IOException()
            }
        }
        val vm = RoutesViewModel(slow, repo, SavedStateHandle())
        vm.acceptRoute(); vm.setCargoConfirmation(true); vm.confirmReceipt()
        vm.selectScenario(DemoScenario.ACTIVE); advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(RouteStatus.ACTIVE, vm.uiState.value.route!!.status)
    }
    @Test fun rejectedAndFailedCommandsKeepStepAndDraftIndependentFromSync() = runTest {
        val repo = DemoRouteRepository()
        val failed = object : RouteRepository by repo {
            override suspend fun confirmReceipt(routeId: String): RouteCommandResult = throw java.io.IOException()
            override suspend fun reportCargoIssue(routeId: String, issue: CargoIssue): RouteCommandResult = RouteCommandResult.Rejected("Falha de teste")
        }
        val vm = RoutesViewModel(failed, repo, SavedStateHandle())
        vm.acceptRoute(); vm.setCargoConfirmation(true); vm.confirmReceipt()
        assertEquals(DepartureStep.RECEIPT, vm.uiState.value.departureStep)
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.cargoConfirmed)
        vm.openCargoIssue(); vm.editCargoIssue("oleo", CargoIssueReason.DAMAGED, "Preservar")
        vm.simulateSync(SyncStatus.SENDING); vm.submitCargoIssue()
        assertTrue(vm.uiState.value.issueDraft.open)
        assertEquals("Preservar", vm.uiState.value.issueDraft.description)
        assertEquals("Falha de teste", vm.uiState.value.issueDraft.error)
        assertEquals(SyncStatus.SENDING, repo.route.value!!.syncStatus)
        vm.editCargoIssue("oleo", CargoIssueReason.DAMAGED, "a".repeat(501))
        assertEquals(500, vm.uiState.value.issueDraft.description.length)
    }
    @Test fun issueDraftSurvivesDismissAndRecreationAndClearsOnlyAfterSuccess() = runTest {
        val repo = DemoRouteRepository(); val saved = SavedStateHandle(); val vm = RoutesViewModel(repo, repo, saved)
        vm.acceptRoute(); vm.openCargoIssue()
        vm.submitCargoIssue()
        assertTrue(vm.uiState.value.issueDraft.open)
        vm.editCargoIssue("oleo", CargoIssueReason.DAMAGED, "Caixa avariada")
        vm.dismissCargoIssue(); vm.openCargoIssue()
        val restoredRepo = DemoRouteRepository(); val restored = RoutesViewModel(restoredRepo, restoredRepo, saved)
        assertTrue(restored.uiState.value.issueDraft.open)
        assertEquals("Caixa avariada", restored.uiState.value.issueDraft.description)
        restored.submitCargoIssue()
        assertFalse(restored.uiState.value.issueDraft.open)
        assertNull(restored.uiState.value.issueDraft.productId)
        assertEquals(1, restoredRepo.route.value!!.openCargoIssues.size)
    }
}
