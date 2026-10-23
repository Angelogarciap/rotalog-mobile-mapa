package com.rotalog.delivery.loading

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.rotalog.delivery.data.DemoRoutes
import com.rotalog.delivery.routes.LegacyDepartureRedirect
import com.rotalog.delivery.routes.RoutesUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LegacyDepartureRedirectTest {
    @get:Rule val compose = createComposeRule()

    @Test fun obsoleteDestinationReturnsWhenEmptyScenarioFinishesLoading() {
        val state = mutableStateOf(RoutesUiState())
        var returns = 0
        compose.setContent {
            LegacyDepartureRedirect(state.value, "removed-route", { false }, { returns++ })
        }
        compose.runOnIdle { assertEquals(0, returns); state.value = state.value.copy(loading = false) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, returns) }
    }

    @Test fun obsoleteDestinationWaitsThenValidatesTheRequestedRouteBeforeReturning() {
        val state = mutableStateOf(RoutesUiState())
        val opened = mutableListOf<String>()
        var returns = 0
        compose.setContent {
            LegacyDepartureRedirect(state.value, "requested-route", { opened += it; false }, { returns++ })
        }
        compose.runOnIdle {
            assertEquals(emptyList<String>(), opened)
            state.value = state.value.copy(route = DemoRoutes.assigned(), loading = false)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf("requested-route"), opened)
            assertEquals(1, returns)
        }
    }
}
