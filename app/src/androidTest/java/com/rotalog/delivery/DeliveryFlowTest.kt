package com.rotalog.delivery

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rotalog.delivery.data.DemoRoutes
import com.rotalog.delivery.model.*
import com.rotalog.delivery.shared.ExternalActions
import com.rotalog.delivery.shared.NavigationApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeliveryFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun resetDemo() {
        compose.onNodeWithText("Explorar demonstração").performScrollTo().performClick()
        compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) }
    }
    private val container get() = (compose.activity.application as RotalogApplication).container
    private fun click(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()
    private fun scrollClick(label: String) = compose.onNodeWithText(label).performScrollTo().performClick()
    private fun activeDemo() {
        click("Perfil")
        scrollClick("Rota ativa demonstrativa")
        click("Rotas")
        compose.onNodeWithText("Mapa demonstrativo • sem GPS").assertIsDisplayed()
    }

    @Test fun explicitAcceptanceLeavesPreparationAndReceiptPending() {
        compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) }
        scrollClick("Aceitar rota")
        compose.onNodeWithText("Confirmar aceite").assertIsDisplayed()
        compose.runOnIdle { assertEquals(RouteStatus.ASSIGNED, container.routes.route.value!!.status) }
        click("Voltar")
        scrollClick("Aceitar rota")
        click("Confirmar aceite")
        compose.waitForIdle()
        compose.runOnIdle {
            val route = container.routes.route.value!!
            assertEquals(RouteStatus.ACCEPTED, route.status)
            assertFalse(route.prepared)
            assertFalse(route.cargoReceived)
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Aceitar rota").assertDoesNotExist()
    }

    @Test fun mapAndListShareSelectionAcrossRecreationAndReturnToNextStop() {
        activeDemo()
        compose.onNodeWithContentDescription("Parada 4, Distribuidora Ponta Negra, pendente").performClick()
        compose.onNodeWithText("PARADA SELECIONADA").assertExists()
        scrollClick("Voltar à próxima parada")
        compose.onNodeWithContentDescription("Lista de paradas").performClick()
        scrollClick("4. Distribuidora Ponta Negra")
        compose.onNodeWithText("PARADA SELECIONADA").assertIsDisplayed()
        compose.onNodeWithText("4. Distribuidora Ponta Negra").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Parada 4, Distribuidora Ponta Negra, pendente, selecionada").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, container.routes.route.value!!.completedStops) }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("4. Distribuidora Ponta Negra").performScrollTo().assertIsDisplayed()
        scrollClick("Voltar à próxima parada")
        compose.onNodeWithContentDescription("Parada 3, Supermercado Amazonas, próxima parada, selecionada").assertIsDisplayed()
        click("Ver detalhes")
        scrollClick("Navegar")
        compose.onNodeWithText("Google Maps").assertIsDisplayed()
        compose.onNodeWithText("Waze").assertIsDisplayed()
        click("Copiar endereço")
        compose.onNodeWithText("Endereço copiado.").assertIsDisplayed()
    }

    @Test fun panelButtonAndGestureExpandAndCollapse() {
        activeDemo()
        click("Ver detalhes")
        compose.onNodeWithText("Recolher detalhes").assertExists()
        click("Recolher detalhes")
        compose.onNodeWithText("Ver detalhes").assertExists()
        // Drag the native sheet handle upwards; the parent state follows the settled sheet state.
        compose.onNodeWithTag("route-sheet-handle", useUnmergedTree = true).performTouchInput { swipe(start = center, end = center.copy(y = center.y - 300f), durationMillis = 500) }
        compose.waitForIdle()
        compose.onNodeWithText("Recolher detalhes").assertExists()
    }

    @Test fun themesScenariosAndManualSyncRemainIndependent() {
        click("Perfil")
        scrollClick("Escuro")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Escuro").performScrollTo().assertIsSelected()
        scrollClick("Claro")
        scrollClick("Usar tema do aparelho")
        scrollClick("Rota ativa demonstrativa")
        scrollClick("Conflito")
        compose.runOnIdle { assertEquals(SyncStatus.CONFLICT, container.routes.route.value!!.syncStatus) }
        click("Rotas")
        compose.runOnIdle { assertEquals(2, container.routes.route.value!!.completedStops) }
        click("Perfil")
        scrollClick("Sem rota atribuída")
        click("Rotas")
        compose.onNodeWithText("Nenhuma rota atribuída").assertIsDisplayed()
        click("Perfil")
        scrollClick("Rota atribuída")
        click("Rotas")
        compose.runOnIdle {
            assertEquals(RouteStatus.ASSIGNED, container.routes.route.value!!.status)
            assertFalse(container.routes.route.value!!.prepared)
            assertFalse(container.routes.route.value!!.cargoReceived)
        }
    }

    @Test fun externalActionsHandleMissingApplicationsAndUseDialerOnly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stop = DemoRoutes.active().nextStop!!
        val missing = ExternalActions(context) { throw ActivityNotFoundException() }
        assertNotNull(missing.navigate(stop, NavigationApp.GOOGLE_MAPS))
        assertNotNull(missing.navigate(stop, NavigationApp.WAZE))
        assertNotNull(missing.dial(stop))
        val launched = mutableListOf<Intent>()
        val actions = ExternalActions(context) { launched += it }
        assertNull(actions.navigate(stop, NavigationApp.GOOGLE_MAPS))
        assertNull(actions.navigate(stop, NavigationApp.WAZE))
        assertNull(actions.dial(stop))
        assertEquals("com.google.android.apps.maps", launched[0].`package`)
        assertEquals(stop.address, launched[0].data!!.getQueryParameter("destination"))
        assertEquals("com.waze", launched[1].`package`)
        assertEquals(Intent.ACTION_DIAL, launched[2].action)
        assertEquals(stop.phone, launched[2].data!!.schemeSpecificPart)
        runBlocking { container.settings.setTheme(ThemeMode.SYSTEM) }
    }
}
