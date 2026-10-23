package com.rotalog.delivery.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.rotalog.delivery.model.Stop

/** Renderer boundary for future Maps Compose integration; viewport excludes the sheet. */
interface RouteMap {
    @Composable
    fun Content(
        stops: List<Stop>, selectedStopId: String?, nextStopId: String?,
        visibleHeight: Dp, expanded: Boolean, onSelectStop: (String) -> Unit,
        modifier: Modifier
    )
}

object DemonstrationRouteMap : RouteMap {
    @Composable
    override fun Content(
        stops: List<Stop>, selectedStopId: String?, nextStopId: String?,
        visibleHeight: Dp, expanded: Boolean, onSelectStop: (String) -> Unit, modifier: Modifier
    ) {
        DemoRouteMap(stops, selectedStopId, nextStopId, visibleHeight, onSelectStop, modifier, expanded)
    }
}
