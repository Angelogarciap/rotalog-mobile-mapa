package com.rotalog.delivery

import androidx.lifecycle.SavedStateHandle
import com.rotalog.delivery.data.*
import com.rotalog.delivery.model.*
import com.rotalog.delivery.routes.RoutesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingRulesTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }
    private suspend fun prepared(repo: DemoRouteRepository): String {
        val id = repo.route.value!!.id
        repo.acceptRoute(id)
        PreparationCheck.entries.forEach { repo.setPreparationCheck(id, it, true) }
        assertEquals(RouteCommandResult.Applied, repo.confirmPreparation(id))
        return id
    }
    @Test fun checklistRequiresAcceptanceAndDoesNotReceiveOrStart() = runTest {
        val repo = DemoRouteRepository()
        val id = repo.route.value!!.id
        assertTrue(repo.setPreparationCheck(id, PreparationCheck.FUEL, true) is RouteCommandResult.Rejected)
        repo.acceptRoute(id)
        assertTrue(repo.confirmPreparation(id) is RouteCommandResult.Rejected)
        assertTrue(repo.confirmReceipt(id) is RouteCommandResult.Rejected)
        assertTrue(repo.startRoute(id) is RouteCommandResult.Rejected)
        PreparationCheck.entries.forEach { repo.setPreparationCheck(id, it, true) }
        assertFalse(repo.route.value!!.prepared)
        repo.confirmPreparation(id)
        assertEquals(RouteCommandResult.AlreadyApplied, repo.confirmPreparation(id))
        assertTrue(repo.route.value!!.prepared)
        assertFalse(repo.route.value!!.cargoReceived)
        assertEquals(0, repo.route.value!!.completedStops)
        assertEquals(RouteStatus.ACCEPTED, repo.route.value!!.status)
    }
    @Test fun everyManifestItemMustBeCheckedBeforeExplicitReceipt() = runTest {
        val repo = DemoRouteRepository(); val id = prepared(repo)
        val manifest = repo.route.value!!.manifest
        assertEquals(70, manifest.sumOf { it.quantity })
        assertEquals(8, manifest.size)
        assertTrue(repo.checkCargo(id, "foreign-product", true) is RouteCommandResult.Rejected)
        manifest.dropLast(1).forEach { repo.checkCargo(id, it.productId, true) }
        assertTrue(repo.confirmReceipt(id) is RouteCommandResult.Rejected)
        repo.checkCargo(id, manifest.last().productId, true)
        assertFalse(repo.route.value!!.cargoReceived)
        assertEquals(RouteCommandResult.Applied, repo.confirmReceipt(id))
        assertEquals(RouteCommandResult.AlreadyApplied, repo.confirmReceipt(id))
        val route = repo.route.value!!
        assertTrue(route.cargoReceived)
        assertEquals(RouteStatus.ACCEPTED, route.status)
        assertEquals(70, route.collectedVolumes)
        assertEquals(0, route.deliveredVolumes)
        assertEquals(1, route.completedStops)
        assertEquals("rio-negro", route.nextStop!!.id)
    }
    @Test fun unresolvedIssueInvalidatesCheckAndResolutionRequiresRecheck() = runTest {
        val repo = DemoRouteRepository(); val id = prepared(repo)
        repo.route.value!!.manifest.forEach { repo.checkCargo(id, it.productId, true) }
        val issue = CargoIssue("oleo", CargoIssueReason.DAMAGED, "Caixa amassada")
        repo.reportCargoIssue(id, issue)
        assertFalse("oleo" in repo.route.value!!.checkedCargo)
        assertTrue(repo.checkCargo(id, "oleo", true) is RouteCommandResult.Rejected)
        assertTrue(repo.confirmReceipt(id) is RouteCommandResult.Rejected)
        repo.resolveCargoIssue(id, "oleo")
        assertTrue(repo.route.value!!.openCargoIssues.isEmpty())
        assertFalse("oleo" in repo.route.value!!.checkedCargo)
        assertTrue(repo.confirmReceipt(id) is RouteCommandResult.Rejected)
        repo.checkCargo(id, "oleo", true)
        repo.confirmReceipt(id)
        assertTrue(repo.reportCargoIssue(id, issue) is RouteCommandResult.Rejected)
        assertTrue(repo.checkCargo(id, "oleo", false) is RouteCommandResult.Rejected)
        assertTrue(repo.route.value!!.cargoReceived)
    }
    @Test fun revokingPreparationKeepsCustodyButBlocksDepartureUntilReconfirmed() = runTest {
        val repo = DemoRouteRepository(); val id = prepared(repo)
        repo.route.value!!.manifest.forEach { repo.checkCargo(id, it.productId, true) }
        repo.confirmReceipt(id)
        repo.setPreparationCheck(id, PreparationCheck.TIRES, false)
        assertFalse(repo.route.value!!.prepared)
        assertTrue(repo.route.value!!.cargoReceived)
        assertTrue(repo.startRoute(id) is RouteCommandResult.Rejected)
        repo.setPreparationCheck(id, PreparationCheck.TIRES, true)
        assertTrue(repo.startRoute(id) is RouteCommandResult.Rejected)
        repo.confirmPreparation(id)
        assertEquals(RouteCommandResult.Applied, repo.startRoute(id))
        assertEquals(RouteCommandResult.AlreadyApplied, repo.startRoute(id))
        assertEquals(RouteStatus.ACTIVE, repo.route.value!!.status)
        assertEquals(0, repo.route.value!!.deliveredVolumes)
        assertEquals("rio-negro", repo.route.value!!.nextStop!!.id)
        assertTrue(repo.setPreparationCheck(id, PreparationCheck.TIRES, false) is RouteCommandResult.Rejected)
    }
    @Test fun snapshotSurvivesSerializationAndRestoresPartialLoadingWithoutAutoConfirmation() = runTest {
        val repo = DemoRouteRepository(); val saved = SavedStateHandle()
        val vm = RoutesViewModel(repo, repo, saved)
        vm.acceptRoute()
        PreparationCheck.entries.forEach { vm.setPreparationCheck(it, true) }
        vm.confirmPreparation()
        vm.checkCargo("oleo", true)
        vm.reportCargoIssue(CargoIssue("arroz", CargoIssueReason.MISSING, "Falta um fardo"))
        val snapshot = saved.get<Route>("routeSnapshot")!!
        val bytes = ByteArrayOutputStream().also { output -> ObjectOutputStream(output).use { it.writeObject(snapshot) } }.toByteArray()
        val restoredSnapshot = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as Route }
        val fresh = DemoRouteRepository()
        val restored = RoutesViewModel(fresh, fresh, SavedStateHandle(mapOf("routeSnapshot" to restoredSnapshot)))
        assertEquals(snapshot, restored.uiState.value.route)
        assertFalse(restored.uiState.value.route!!.cargoReceived)
        assertEquals(1, restored.uiState.value.route!!.openCargoIssues.size)
        restored.selectScenario(DemoScenario.ACTIVE)
        restored.selectScenario(DemoScenario.ASSIGNED)
        assertEquals(DemoRoutes.assigned(), fresh.route.value)
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
