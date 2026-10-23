package com.rotalog.delivery.profile

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
class ProfileRulesTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun activeFixtureAndResetNeverCompleteAssignedRoute() = runTest {
        val repo = DemoRouteRepository()
        repo.acceptRoute(repo.route.value!!.id)
        repo.selectScenario(DemoScenario.ACTIVE)
        assertNotEquals(DemoRoutes.assigned().id, repo.route.value!!.id)
        assertEquals(2, repo.route.value!!.completedStops)
        repo.selectScenario(DemoScenario.ASSIGNED)
        assertEquals(DemoRoutes.assigned(), repo.route.value)
        repo.selectScenario(DemoScenario.EMPTY)
        assertNull(repo.route.value)
        assertEquals(AcceptRouteResult.NotAvailable, repo.acceptRoute("absent"))
        repo.resetScenario()
        assertNull(repo.route.value)
    }

    @Test fun manualSyncHasNoAutomaticConfirmationOrOperationalEffect() = runTest {
        val repo = DemoRouteRepository()
        val original = repo.route.value!!
        SyncStatus.entries.forEach { status ->
            repo.simulateSync(status)
            advanceTimeBy(60_000)
            assertEquals(original.copy(syncStatus = status), repo.route.value)
        }
    }

    @Test fun reselectingScenarioPreservesDraftAndExplicitResetClearsIt() = runTest {
        val repo = DemoRouteRepository()
        val vm = RoutesViewModel(repo, repo, SavedStateHandle())
        vm.acceptRoute()
        vm.setPreparationCheck(PreparationCheck.FUEL, true)
        vm.selectScenario(DemoScenario.ASSIGNED)
        assertEquals(setOf(PreparationCheck.FUEL), repo.route.value!!.checklist)
        vm.resetScenario()
        assertEquals(DemoRoutes.assigned(), repo.route.value)
    }
}
