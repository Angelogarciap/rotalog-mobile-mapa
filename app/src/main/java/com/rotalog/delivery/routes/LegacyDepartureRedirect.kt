package com.rotalog.delivery.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Both obsolete loading destinations return to the single departure screen. */
@Composable
internal fun LegacyDepartureRedirect(
    state: RoutesUiState,
    routeId: String?,
    onOpen: (String) -> Boolean,
    onReturnToRoute: () -> Unit
) {
    LaunchedEffect(state.route?.id, state.loading, routeId) {
        if (state.loading && state.route == null) return@LaunchedEffect
        routeId?.let(onOpen)
        onReturnToRoute()
    }
}
