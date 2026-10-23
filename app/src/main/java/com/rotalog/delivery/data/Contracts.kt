package com.rotalog.delivery.data

import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RouteRepository {
    val route: StateFlow<Route?>
    val history: StateFlow<List<Route>>
    suspend fun acceptRoute(routeId: String): AcceptRouteResult
    suspend fun setPreparationCheck(routeId: String, check: PreparationCheck, checked: Boolean): RouteCommandResult
    suspend fun confirmPreparation(routeId: String): RouteCommandResult
    suspend fun checkCargo(routeId: String, productId: String, checked: Boolean): RouteCommandResult
    suspend fun reportCargoIssue(routeId: String, issue: CargoIssue): RouteCommandResult
    suspend fun resolveCargoIssue(routeId: String, productId: String): RouteCommandResult
    suspend fun confirmReceipt(routeId: String): RouteCommandResult
    suspend fun startRoute(routeId: String): RouteCommandResult
}
interface DemoScenarioController {
    val scenario: StateFlow<DemoScenario>
    fun selectScenario(scenario: DemoScenario)
    fun restoreRoute(snapshot: Route)
    fun resetScenario()
    fun simulateSync(status: SyncStatus)
}
interface SettingsRepository {
    val theme: Flow<ThemeMode>
    suspend fun setTheme(mode: ThemeMode)
}
