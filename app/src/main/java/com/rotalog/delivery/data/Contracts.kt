package com.rotalog.delivery.data

import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RouteRepository {
    val route: StateFlow<Route?>
    val history: StateFlow<List<Route>>
    suspend fun acceptRoute(routeId: String): AcceptRouteResult
}
interface DemoScenarioController {
    val scenario: StateFlow<DemoScenario>
    fun selectScenario(scenario: DemoScenario)
    fun resetScenario()
    fun simulateSync(status: SyncStatus)
}
interface SettingsRepository {
    val theme: Flow<ThemeMode>
    suspend fun setTheme(mode: ThemeMode)
}
