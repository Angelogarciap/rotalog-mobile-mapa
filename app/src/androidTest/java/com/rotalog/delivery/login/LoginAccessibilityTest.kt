package com.rotalog.delivery.login

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.EspressoKey
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginAccessibilityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance()
        val second = b.luminance()
        return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
    }

    private fun bothThemes(check: (ThemeMode) -> Unit) {
        val settings = (compose.activity.application as RotalogApplication).container.settings
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            runBlocking { settings.setTheme(theme) }
            compose.waitForIdle()
            compose.waitUntil(5_000) {
                val color = compose.onRoot().captureToImage().toPixelMap()[1, 1]
                (color.luminance() < .1f) == (theme == ThemeMode.DARK)
            }
            check(theme)
        }
    }

    @Test fun identifierLabelStaysAboveFieldWhileEditingInBothThemes() = bothThemes {
        val field = compose.onNodeWithTag("login-identifier")
        fun assertLabelAboveField() {
            field.performScrollTo()
            val label = compose.onNodeWithText("Identificador / Matrícula ou Telefone", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            assertTrue("Rótulo deve permanecer acima do campo, sem sobreposição",
                label.bottom <= field.fetchSemanticsNode().boundsInRoot.top)
        }
        assertLabelAboveField()
        field.performTextReplacement("DRV-9082")
        assertLabelAboveField()
        field.performTextClearance()
        assertLabelAboveField()
    }

    @Test fun keyboardFocusFollowsFormOrderInBothThemes() = bothThemes {
        val field = compose.onNodeWithTag("login-identifier")
        val submit = compose.onNodeWithText("Entrar no RotaLog")
        val demo = compose.onNodeWithText("Explorar demonstração")
        field.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        field.assertIsFocused()
        fun tab(backwards: Boolean = false) {
            val key = EspressoKey.Builder().withKeyCode(KeyEvent.KEYCODE_TAB)
                .withShiftPressed(backwards).build()
            onView(isRoot()).perform(pressKey(key))
        }
        tab()
        submit.assertIsFocused()
        tab()
        demo.assertIsFocused()
        tab(backwards = true)
        submit.assertIsFocused()
        tab(backwards = true)
        field.assertIsFocused()
    }

    @Test fun textHasAtLeastFourPointFiveContrastInBothThemes() = bothThemes { theme ->
        // Read the theme actually drawn, which can differ from the device preference.
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val canvas = pixels[1, 1]
        for (text in listOf("RotaLog", "Entregador", "Acesso do motorista",
            "Informe o acesso fornecido pela sua operação.", "Acesso para entregadores cadastrados.",
            "Explorar demonstração", "Versão demonstrativa • autenticação não conectada")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performScrollTo()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val color = layouts.single().layoutInput.style.color
            val ratio = contrast(color, canvas)
            assertTrue("Texto '$text': contraste $ratio (mínimo 4.5)", ratio >= 4.5f)
        }
        val fieldBackground = if (theme == ThemeMode.DARK) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        fun checkText(text: String, background: Color) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val ratio = contrast(layouts.single().layoutInput.style.color, background)
            assertTrue("Texto '$text': contraste $ratio (mínimo 4.5)", ratio >= 4.5f)
        }
        checkText("Identificador / Matrícula ou Telefone", fieldBackground)
        val field = compose.onNodeWithTag("login-identifier")
        field.performScrollTo().performTextReplacement("DRV-9082")
        checkText("Identificador / Matrícula ou Telefone", fieldBackground)
        checkText("DRV-9082", fieldBackground)
        field.performTextClearance()
        checkText("DRV-9082 ou (92) 99999-9999", fieldBackground)
        compose.onNodeWithText("Entrar no RotaLog").performScrollTo().performClick()
        checkText("Informe seu identificador, matrícula ou telefone.", canvas)
        checkText("Identificador / Matrícula ou Telefone", canvas)
        field.performScrollTo().performTextReplacement("DRV-9082")
        field.performImeAction()
        checkText("Acesso indisponível nesta versão. Nenhum dado foi enviado.", fieldBackground)
        field.performScrollTo().performTextReplacement(" ")
        field.performTextClearance()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Entrar no RotaLog").performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(contrast(layouts.single().layoutInput.style.color, Color(0xFF2563EB)) >= 4.5f)
    }
}
