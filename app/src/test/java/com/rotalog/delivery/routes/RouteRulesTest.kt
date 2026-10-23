package com.rotalog.delivery.routes

import androidx.lifecycle.SavedStateHandle
import com.rotalog.delivery.data.DemoRouteRepository
import com.rotalog.delivery.data.DemoRoutes
import com.rotalog.delivery.model.*
import com.rotalog.delivery.routes.RoutesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRulesTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun acceptanceIsExplicitIdempotentAndLeavesPrerequisitesPending() = runTest {
        val repo = DemoRouteRepository()
        val id = repo.route.value!!.id
        assertEquals(AcceptRouteResult.NotAvailable, repo.acceptRoute("stale-id"))
        assertEquals(RouteStatus.ASSIGNED, repo.route.value!!.status)
        assertEquals(AcceptRouteResult.Accepted, repo.acceptRoute(id))
        assertEquals(AcceptRouteResult.AlreadyAccepted, repo.acceptRoute(id))
        with(repo.route.value!!) {
            assertEquals(RouteStatus.ACCEPTED, status)
            assertFalse(cargoReceived)
            assertFalse(prepared)
            assertEquals(0, completedStops)
            assertEquals(SyncStatus.LOCAL, syncStatus)
        }
    }


    @Test fun quantitiesAndProgressDistinguishCollectionAndDelivery() {
        val route = DemoRoutes.active()
        assertEquals((1..8).toList(), route.stops.map { it.sequence })
        assertEquals(70, route.collectionVolumes)
        assertEquals(70, route.deliveryVolumes)
        assertEquals(70, route.collectedVolumes)
        assertEquals(8, route.deliveredVolumes)
        assertEquals(.25f, route.progress)
        assertEquals("amazonas", route.nextStop!!.id)
        val supermarket = route.nextStop!!
        assertEquals(listOf(8, 4, 2), supermarket.items.map { it.quantity })
        assertEquals(14, supermarket.volumes)
        assertEquals("Roberto (gerente)", supermarket.contact)
    }


    @Test fun selectionAndExpansionRestoreWithoutChangingOperationalState() = runTest {
        val repo = DemoRouteRepository()
        val saved = SavedStateHandle()
        val vm = RoutesViewModel(repo, repo, saved)
        vm.selectScenario(DemoScenario.ACTIVE)
        val original = repo.route.value
        vm.selectStop("ponta-negra")
        assertEquals("ponta-negra", vm.uiState.value.selectedStopId)
        assertTrue(vm.uiState.value.expanded)
        assertEquals(original, repo.route.value)
        vm.selectStop("invalid")
        assertEquals("ponta-negra", vm.uiState.value.selectedStopId)
        val restoredRepo = DemoRouteRepository()
        val restored = RoutesViewModel(restoredRepo, restoredRepo, saved)
        assertEquals(DemoScenario.ACTIVE, restored.uiState.value.scenario)
        assertEquals("ponta-negra", restored.uiState.value.selectedStopId)
        assertTrue(restored.uiState.value.expanded)
        restored.nextStop()
        assertEquals("amazonas", restored.uiState.value.selectedStopId)
        assertFalse(restored.uiState.value.expanded)
    }

    @Test fun restoredAcceptanceDoesNotStartRoute() = runTest {
        val repo = DemoRouteRepository()
        val saved = SavedStateHandle(mapOf("scenario" to "ASSIGNED", "accepted" to true, "sync" to "WAITING"))
        val vm = RoutesViewModel(repo, repo, saved)
        assertEquals(RouteStatus.ACCEPTED, vm.uiState.value.route!!.status)
        assertEquals(SyncStatus.WAITING, vm.uiState.value.route!!.syncStatus)
        assertFalse(vm.uiState.value.route!!.cargoReceived)
        assertFalse(vm.uiState.value.route!!.prepared)
    }
}
