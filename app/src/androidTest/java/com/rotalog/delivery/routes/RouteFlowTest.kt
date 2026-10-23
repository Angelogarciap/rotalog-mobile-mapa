package com.rotalog.delivery.routes

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
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
class RouteFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun resetDemo() {
        compose.onNodeWithText("Explorar demonstração").performScrollTo().performClick()
        compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) }
    }
    private val container get() = (compose.activity.application as RotalogApplication).container
    private fun click(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()
    private fun scrollClick(label: String) = compose.onNodeWithText(label).performScrollTo().performClick()
    private fun activeDemo() {
        compose.runOnIdle { container.routes.selectScenario(DemoScenario.ACTIVE) }
        compose.onNodeWithText("Mapa demonstrativo • sem GPS").assertIsDisplayed()
    }

    @Test fun mapMarkersStayBelowLegendAndAboveTheSummaryWithoutOverlap() {
        activeDemo()
        val legend = compose.onNodeWithText("Mapa demonstrativo • sem GPS").fetchSemanticsNode().boundsInRoot
        val summary = compose.onNodeWithText("PRÓXIMA PARADA").fetchSemanticsNode().boundsInRoot
        val markers = compose.onAllNodes(hasContentDescription("Parada ", substring = true)).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(8, markers.size)
        markers.forEach { marker ->
            assertTrue("Marker below legend", marker.top >= legend.bottom)
            assertTrue("Marker above summary", marker.bottom <= summary.top)
        }
        markers.forEachIndexed { i, marker ->
            markers.drop(i + 1).forEach { other -> assertFalse("Marker targets must not overlap: $marker and $other", marker.overlaps(other)) }
        }
    }

    @Test fun emptyRouteOffersProfileScenariosWithoutAssigningRoute() {
        compose.runOnIdle { container.routes.selectScenario(DemoScenario.EMPTY) }
        compose.onNodeWithText("Explorar cenários no Perfil").performScrollTo().performClick()
        compose.onNodeWithText("Sua identidade e suas preferências.").assertIsDisplayed()
        compose.runOnIdle { assertNull(container.routes.route.value) }
    }

    @Test fun departureShowsThreeStagesAndFutureStagesRemainUnavailable() {
        compose.onNodeWithContentDescription("Etapa 3, Início, pendente").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Etapa 2, Recebimento, pendente").assertIsNotEnabled()
        scrollClick("Aceitar rota"); click("Confirmar aceite")
        compose.onNodeWithContentDescription("Etapa 2, Recebimento, atual").assertIsSelected()
        compose.onNodeWithContentDescription("Etapa 3, Início, pendente").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Etapa 1, Aceite, confirmada").performScrollTo().performClick()
        compose.onNodeWithText("Aceite confirmado").performScrollTo().assertIsDisplayed()
        scrollClick("Continuar para recebimento")
        scrollClick("Conferi as mercadorias e confirmo que os 70 volumes estão corretos.")
        scrollClick("Confirmar recebimento"); click("Confirmar carga recebida")
        compose.onNodeWithContentDescription("Etapa 3, Início, atual").assertIsSelected()
        compose.onNodeWithText("Iniciar rota").performScrollTo().assertIsEnabled()
        compose.runOnIdle {
            assertFalse(container.routes.route.value!!.prepared)
            assertEquals(RouteStatus.ACCEPTED, container.routes.route.value!!.status)
        }
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
        compose.onNodeWithText("Detalhes da parada").assertIsDisplayed()
        compose.onNodeWithText("4. Distribuidora Ponta Negra").performScrollTo().assertIsDisplayed()

        compose.runOnIdle { assertEquals(2, container.routes.route.value!!.completedStops) }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("4. Distribuidora Ponta Negra").performScrollTo().assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        scrollClick("Voltar à próxima parada")
        compose.onNodeWithContentDescription("Parada 3, Supermercado Amazonas, próxima parada, selecionada").assertIsDisplayed()
        click("Ver detalhes")
        scrollClick("Navegar")
        compose.onNodeWithText("Google Maps").assertIsDisplayed()
        compose.onNodeWithText("Waze").assertIsDisplayed()
        click("Copiar endereço")
        compose.onNodeWithText("Endereço copiado.").assertIsDisplayed()
    }

    @Test fun detailsAreModalAndKeepMapGeometryOnReturn() {
        activeDemo()
        val marker = compose.onNodeWithContentDescription("Parada 3, Supermercado Amazonas, próxima parada, selecionada")
        val before = marker.fetchSemanticsNode().boundsInRoot
        click("Ver detalhes")
        compose.onNodeWithText("Detalhes da parada").assertIsDisplayed()
        compose.onNodeWithText("Endereço completo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Envio simulado:", substring = true).performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Detalhes da parada").assertExists()
        compose.onNodeWithText("Envio simulado:", substring = true).assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            compose.onNodeWithText("Ver detalhes").fetchSemanticsNode().config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Focused) { false }
        }
        compose.onNodeWithText("Ver detalhes").assertIsDisplayed().assertIsFocused()
        assertEquals(before, marker.fetchSemanticsNode().boundsInRoot)
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
