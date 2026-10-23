package com.rotalog.delivery.login

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.model.ThemeMode
import com.rotalog.delivery.shared.captureFrame
import com.rotalog.delivery.shared.captureScrollPages
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private val unavailable = "Acesso indisponível nesta versão. Nenhum dado foi enviado."

    @Test fun driverAccessHasOneIdentifierFieldAndNoPasswordControls() {
        compose.onNodeWithText("Acesso do motorista").assertExists()
        compose.onNodeWithTag("login-identifier").assertExists()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        compose.onNodeWithText("Senha").assertDoesNotExist()
        compose.onNodeWithText("E-mail").assertDoesNotExist()
        compose.onNodeWithText("Esqueci minha senha").assertDoesNotExist()
        compose.onNodeWithText("Entrar no RotaLog").assertExists()
    }

    @Test fun validatesEmptyAndAcceptsIdentifiersWithoutAuthenticating() {
        compose.onNodeWithText("Rotas").assertDoesNotExist()
        click("Entrar no RotaLog")
        compose.onNodeWithText("Informe seu identificador, matrícula ou telefone.").assertExists()
        for (identifier in listOf("DRV-9082", "9082", "(92) 99999-9999")) {
            compose.onNodeWithTag("login-identifier").performScrollTo().performTextReplacement(identifier)
            compose.onNodeWithText("Informe seu identificador, matrícula ou telefone.").assertDoesNotExist()
            compose.onNodeWithTag("login-identifier").performImeAction()
            compose.onNodeWithText(unavailable).assertExists()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            compose.onNodeWithText("Rotas").assertDoesNotExist()
        }
    }

    @Test fun recreationRestoresIdentifierAndSeparateDemoEntry() {
        compose.onNodeWithTag("login-identifier").performScrollTo().performTextInput("DRV-9082")
        closeSoftKeyboard()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("login-identifier").performScrollTo().assertTextContains("DRV-9082")
        compose.onNodeWithText("Rotas").assertDoesNotExist()
        click("Explorar demonstração")
        compose.onNode(hasText("Rotas") and hasClickAction()).assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Rotas") and hasClickAction()).assertExists()
    }

    @Test fun reviewBothThemesWithKeyboardErrorsAndLastControl() {
        val settings = (compose.activity.application as RotalogApplication).container.settings
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            runBlocking { settings.setTheme(theme) }
            compose.waitForIdle()
            compose.onNodeWithText("Acesso do motorista").performScrollTo().assertIsDisplayed()
            captureScrollPages(compose, "${theme.name}-driver-access")
            compose.onNodeWithTag("login-identifier").performScrollTo().performTextReplacement("DRV-9082")
            captureFrame(compose, "${theme.name}-driver-keyboard")
            compose.onNodeWithTag("login-identifier").performImeAction()
            compose.onNodeWithText(unavailable).performScrollTo().assertIsDisplayed()
            captureFrame(compose, "${theme.name}-driver-unavailable")
            compose.onNodeWithTag("login-identifier").performScrollTo().performTextClearance()
            closeSoftKeyboard()
            click("Entrar no RotaLog")
            compose.onNodeWithText("Informe seu identificador, matrícula ou telefone.").performScrollTo().assertIsDisplayed()
            captureFrame(compose, "${theme.name}-driver-validation")
            compose.onNodeWithText("Explorar demonstração").performScrollTo().assertIsDisplayed()
        }
    }
}
