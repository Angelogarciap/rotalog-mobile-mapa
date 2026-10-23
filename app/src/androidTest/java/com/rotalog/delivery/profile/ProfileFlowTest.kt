package com.rotalog.delivery.profile

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) = compose.onNode(hasText(text) and hasClickAction()).performClick()
    private val container get() = (compose.activity.application as RotalogApplication).container
    private fun scrollClick(text: String) {
        val demoControls = com.rotalog.delivery.model.DemoScenario.entries.map { it.label } +
            com.rotalog.delivery.model.SyncStatus.entries.map { it.label } + "Reiniciar cenário atual"
        if (text in demoControls && compose.onAllNodesWithText("Reiniciar cenário atual").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithText("Ambiente de demonstração").performScrollTo().performClick()
        }
        compose.onNodeWithText(text).performScrollTo().performClick()
    }
    @Before fun openProfile() {
        scrollClick("Explorar demonstração")
        click("Perfil")
    }
    @Test fun demoStartsCollapsedAndToggleOnlyChangesPresentation() {
        val container = (compose.activity.application as RotalogApplication).container
        val before = container.routes.route.value
        compose.onNodeWithText("Reiniciar cenário atual").assertDoesNotExist()
        scrollClick("Ambiente de demonstração")
        compose.onNodeWithText("Reiniciar cenário atual").assertExists()
        scrollClick("Ambiente de demonstração")
        compose.onNodeWithText("Reiniciar cenário atual").assertDoesNotExist()
        compose.runOnIdle { assertEquals(before, container.routes.route.value) }
    }
    @Test fun expansionAndScrollSurviveRecreationAndTabReturn() {
        scrollClick("Ambiente de demonstração")
        compose.onNodeWithText("Conflito").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Conflito").assertIsDisplayed()
        click("Histórico")
        click("Perfil")
        compose.onNodeWithText("Conflito").assertIsDisplayed()
        scrollClick("Ambiente de demonstração")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Reiniciar cenário atual").assertDoesNotExist()
    }
    @Test fun themeOptionsHaveAccessibleAdaptiveLayout() {
        val configuration = compose.activity.resources.configuration
        compose.onNodeWithText("Usar tema do aparelho").performScrollTo()
        val bounds = listOf("Usar tema do aparelho", "Claro", "Escuro").map {
            compose.onNodeWithText(it).getUnclippedBoundsInRoot()
        }
        bounds.forEach { assertTrue("Alvo de toque mínimo de 48 dp", it.bottom - it.top >= 48.dp) }
        if (configuration.screenWidthDp >= 360 && configuration.fontScale < 1.5f) {
            val heights = bounds.map { it.bottom - it.top }
            assertEquals("Opções lado a lado devem ter a mesma altura", heights[0], heights[1])
            assertEquals("Opções lado a lado devem ter a mesma altura", heights[0], heights[2])
            assertEquals(bounds[0].top, bounds[1].top)
        } else {
            assertTrue("Opções empilhadas sem sobreposição", bounds[1].top >= bounds[0].bottom)
            assertTrue("Opções empilhadas sem sobreposição", bounds[2].top >= bounds[1].bottom)
            assertEquals(bounds[0].left, bounds[1].left)
        }
    }
    @Test fun themesExposeSingleAccessibleSelectionAndCheckmark() {
        for (theme in listOf("Claro", "Escuro", "Usar tema do aparelho")) {
            scrollClick(theme)
            compose.onNodeWithText(theme).assertIsSelected().assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
            compose.onAllNodesWithContentDescription("Tema selecionado").assertCountEquals(1)
            listOf("Claro", "Escuro", "Usar tema do aparelho").filterNot { it == theme }.forEach {
                compose.onNodeWithText(it).assertIsNotSelected()
            }
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithText(theme).performScrollTo().assertIsSelected()
        }
    }
    @Test fun themesScenariosAndManualSyncRemainIndependent() {
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

}
