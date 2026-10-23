package com.rotalog.delivery

import android.app.Activity
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.rotalog.delivery.login.LoginScreen
import com.rotalog.delivery.login.LoginViewModel
import com.rotalog.delivery.history.HistoryScreen
import com.rotalog.delivery.model.*
import com.rotalog.delivery.profile.ProfileScreen
import com.rotalog.delivery.routes.*
import com.rotalog.delivery.shared.*
import com.rotalog.delivery.ui.theme.RotalogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RotalogApp((application as RotalogApplication).container) }
    }
}

@Composable
fun RotalogApp(container: AppContainer) {
    val settings: SettingsViewModel = viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(container.settings) }
    })
    val theme by settings.theme.collectAsStateWithLifecycle()
    val dark = when (theme) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; else -> isSystemInDarkTheme() }
    val activity = LocalContext.current as Activity
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    RotalogTheme(darkTheme = dark) {
        if (theme == null) {
            Surface(Modifier.fillMaxSize()) { Box(Modifier.safeDrawingPadding()) { CircularProgressIndicator() } }
        } else {
            val login: LoginViewModel = viewModel(factory = viewModelFactory {
                initializer { LoginViewModel(createSavedStateHandle()) }
            })
            val loginState by login.uiState.collectAsStateWithLifecycle()
            if (loginState.exploringDemo) {
                AppNavigation(container, settings, theme!!)
            } else {
                LoginScreen(loginState, login::setEmail, login::setPassword, login::togglePassword,
                    login::submit, login::recoverPassword, login::exploreDemo)
            }
        }
    }
}

@Composable
private fun AppNavigation(container: AppContainer, settings: SettingsViewModel, theme: ThemeMode) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val actions = remember(context) { ExternalActions(context) }
    var navigateStopId by rememberSaveable { mutableStateOf<String?>(null) }
    var navigationFailure by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(navigateStopId) { navigationFailure = null }
    val settingsError by settings.error.collectAsStateWithLifecycle()
    LaunchedEffect(settingsError) { settingsError?.let { snackbar.showSnackbar(it); settings.clearError() } }
    fun notify(message: String?) { message?.let { scope.launch { snackbar.showSnackbar(it) } } }
    val tabs = listOf("routes" to "Rotas", "history" to "Histórico", "profile" to "Perfil")
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, (destination, label) ->
                    NavigationBarItem(
                        selected = entry?.destination?.hierarchy?.any { it.route == destination } == true,
                        onClick = {
                            nav.navigate(destination) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(listOf(NavigationIcons.Routes, NavigationIcons.History, NavigationIcons.Profile)[index], contentDescription = null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "routes", route = "main", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            navigation(startDestination = "route", route = "routes") {
                composable("route") { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    RoutesScreen(state, vm::acceptRoute, vm::selectStop, vm::setExpanded,
                        { id -> nav.navigate("stops/$id") }, { navigateStopId = it.id }, { notify(actions.dial(it)) }, vm::nextStop,
                        { nav.navigate("preparation/$it") }, { nav.navigate("receipt/$it") }, vm::startRoute)
                }
                composable("preparation/{routeId}", arguments = listOf(navArgument("routeId") { type = NavType.StringType })) { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    val validState = state.copy(route = state.route?.takeIf { it.id == backStack.arguments?.getString("routeId") })
                    PreparationScreen(validState, vm::setPreparationCheck, vm::confirmPreparation,
                        { nav.navigate("receipt/${state.route?.id}") }, { nav.popBackStack("route", false) })
                }
                composable("receipt/{routeId}", arguments = listOf(navArgument("routeId") { type = NavType.StringType })) { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    val validState = state.copy(route = state.route?.takeIf { it.id == backStack.arguments?.getString("routeId") })
                    ReceiptScreen(validState, vm::checkCargo, vm::reportCargoIssue, vm::resolveCargoIssue, vm::confirmReceipt,
                        { nav.popBackStack("route", false) }, {
                            nav.navigate("preparation/${state.route?.id}") { popUpTo("route") }
                        })
                }
                composable("stops/{routeId}", arguments = listOf(navArgument("routeId") { type = NavType.StringType })) { backStack ->
                    val vm = routeViewModel(nav, backStack, container)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    val route = state.route?.takeIf { it.id == backStack.arguments?.getString("routeId") }
                    StopsScreen(route, { vm.selectStop(it); nav.popBackStack() }, { nav.popBackStack() })
                }
            }
            composable("history") {
                val history by container.routes.history.collectAsStateWithLifecycle()
                HistoryScreen(history)
            }
            composable("profile") { backStack ->
                val vm = routeViewModel(nav, backStack, container)
                val state by vm.uiState.collectAsStateWithLifecycle()
                ProfileScreen(theme, state.scenario, state.route?.syncStatus ?: SyncStatus.LOCAL,
                    settings::setTheme, vm::selectScenario, vm::simulateSync, vm::resetScenario)
            }
        }
    }
    val stop = container.routes.route.collectAsStateWithLifecycle().value?.stops?.find { it.id == navigateStopId }
    if (stop != null) {
        AlertDialog(
            onDismissRequest = { navigateStopId = null },
            title = { Text("Escolha como navegar") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stop.address)
                    navigationFailure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    OutlinedButton(onClick = { navigationFailure = actions.navigate(stop, NavigationApp.GOOGLE_MAPS); if (navigationFailure == null) navigateStopId = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Google Maps") }
                    OutlinedButton(onClick = { navigationFailure = actions.navigate(stop, NavigationApp.WAZE); if (navigationFailure == null) navigateStopId = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Waze") }
                    OutlinedButton(onClick = { notify(actions.copyAddress(stop)); navigateStopId = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Copiar endereço") }
                }
            },
            confirmButton = { TextButton(onClick = { navigateStopId = null }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun routeViewModel(nav: NavHostController, entry: NavBackStackEntry, container: AppContainer): RoutesViewModel {
    val owner = remember(entry) { nav.getBackStackEntry("main") }
    return viewModel(owner, factory = viewModelFactory {
        initializer { RoutesViewModel(container.routes, container.routes, createSavedStateHandle()) }
    })
}
