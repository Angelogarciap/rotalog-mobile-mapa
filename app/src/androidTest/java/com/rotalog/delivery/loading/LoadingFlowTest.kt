package com.rotalog.delivery.loading

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.model.*
import com.rotalog.delivery.shared.captureFrame
import com.rotalog.delivery.shared.captureScrollPages
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val container get() = (compose.activity.application as RotalogApplication).container
    private fun click(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()
    private fun scrollClick(label: String) = compose.onNodeWithText(label).performScrollTo().performClick()
    private val declaration = "Conferi as mercadorias e confirmo que os 70 volumes estão corretos."
    @Before fun reset() {
        scrollClick("Explorar demonstração")
        compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) }
    }
    private fun accept() { scrollClick("Aceitar rota"); click("Confirmar aceite") }
    @Test fun acceptanceAutomaticallyOpensCompactReceiptWithOneGlobalCheckbox() {
        accept()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithText("Mercadoria").assertIsDisplayed()
        compose.onNodeWithText("Quantidade").assertIsDisplayed()
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        val scale = compose.activity.resources.configuration.fontScale
        container.routes.route.value!!.manifest.forEach {
            if (scale == 1f) compose.onNodeWithText(it.description).assertIsDisplayed()
            else compose.onNodeWithText(it.description).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Confirmar recebimento").performScrollTo().assertIsNotEnabled()
        scrollClick(declaration); scrollClick("Confirmar recebimento"); click("Cancelar")
        compose.onNodeWithText(declaration).performScrollTo().assertIsOn()
        compose.runOnIdle { assertFalse(container.routes.route.value!!.cargoReceived) }
        scrollClick("Confirmar recebimento"); click("Confirmar carga recebida")
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithText("Iniciar rota").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(RouteStatus.ACCEPTED, container.routes.route.value!!.status) }
        scrollClick("Voltar"); compose.onNodeWithText("Carga recebida confirmada").performScrollTo().assertIsDisplayed()
        pressBack(); compose.onNodeWithText("Aceite confirmado").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithText("Aceite confirmado").performScrollTo().assertIsDisplayed()
        scrollClick("Continuar para recebimento"); scrollClick("Continuar para início")
        scrollClick("Iniciar rota"); click("Cancelar")
        compose.runOnIdle { assertEquals(RouteStatus.ACCEPTED, container.routes.route.value!!.status) }
        scrollClick("Iniciar rota"); click("Iniciar agora")
        compose.onNodeWithContentDescription("Parada 2, Drogaria Rio Negro, próxima parada, selecionada").assertIsDisplayed()
    }
    @Test fun discrepancyDraftRestoresBlocksReceiptAndResolutionRequiresGlobalRecheck() {
        accept(); scrollClick(declaration); scrollClick("Indicar divergência")
        compose.onNodeWithText("Registrar divergência").performScrollTo().assertIsNotEnabled()
        scrollClick("Selecionar produto"); click("Óleo • 8 caixas")
        scrollClick("Selecionar motivo"); click("Embalagem avariada")
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Caixa amassada na doca")
        pressBack() // Android closes the keyboard before the dialog.
        compose.onNode(hasSetTextAction()).assertTextContains("Caixa amassada na doca")
        compose.onNodeWithText("Cancelar").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertTextContains("Caixa amassada na doca")
        pressBack(); scrollClick("Indicar divergência")
        compose.onNode(hasSetTextAction()).assertTextContains("Caixa amassada na doca")
        scrollClick("Registrar divergência")
        compose.onNodeWithText(declaration).performScrollTo().assertIsOff().assertIsNotEnabled()
        scrollClick("Registrar resolução"); click("Confirmar resolução")
        compose.onNodeWithText(declaration).performScrollTo().assertIsOff().assertIsEnabled()
        scrollClick(declaration); scrollClick("Confirmar recebimento"); click("Confirmar carga recebida")
        scrollClick("Voltar")
        compose.onAllNodesWithText("Indicar divergência").assertCountEquals(0)
        compose.onNodeWithText(declaration).performScrollTo().assertIsNotEnabled()
    }
    @Test fun departureScreensAndPartialDialogFollowBothThemes() {
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            kotlinx.coroutines.runBlocking { container.settings.setTheme(theme) }
            compose.runOnIdle { container.routes.selectScenario(DemoScenario.ASSIGNED) }
            captureScrollPages(compose, "${theme.name}-aceite")
            scrollClick("Aceitar rota"); captureFrame(compose, "${theme.name}-confirmar-aceite"); click("Confirmar aceite")
            captureFrame(compose, "${theme.name}-manifesto-inicial")
            captureScrollPages(compose, "${theme.name}-manifesto")
            scrollClick("Indicar divergência"); captureScrollPages(compose, "${theme.name}-divergencia")
            compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Conferência na doca")
            captureFrame(compose, "${theme.name}-divergencia-teclado")
            compose.onNodeWithText("Cancelar").performScrollTo().assertIsDisplayed()
            captureFrame(compose, "${theme.name}-divergencia-teclado-acoes")
            closeSoftKeyboard(); scrollClick("Cancelar")
            scrollClick(declaration); scrollClick("Confirmar recebimento"); captureFrame(compose, "${theme.name}-confirmar-recebimento"); click("Confirmar carga recebida")
            captureScrollPages(compose, "${theme.name}-inicio")
            scrollClick("Iniciar rota"); captureFrame(compose, "${theme.name}-confirmar-inicio"); click("Cancelar")
        }
    }
}
