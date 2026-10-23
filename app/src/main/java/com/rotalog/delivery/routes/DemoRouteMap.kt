package com.rotalog.delivery.routes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.Stop
import com.rotalog.delivery.model.StopStatus

/** A replaceable map contract: all coordinates and streets here are illustrative, with no GPS. */
@Composable
fun DemoRouteMap(
    stops: List<Stop>,
    selectedStopId: String?,
    nextStopId: String?,
    visibleHeight: Dp,
    onSelectStop: (String) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val ordered = stops.sortedBy { it.sequence }
    BoxWithConstraints(modifier.background(colors.surfaceVariant)) {
        val mapHeight = visibleHeight.coerceIn(104.dp, maxHeight)
        val compact = expanded && mapHeight < 250.dp
        val grid = !expanded && mapHeight < 250.dp
        val positions = ordered.mapIndexed { index, stop ->
            // In the compact viewport, keep the selected marker in view; other stops remain in the full map.
            val x = if (grid) .08f + (index % 4) * .28f else listOf(.16f, .38f, .74f, .83f, .59f, .29f, .17f, .64f)[index % 8]
            val y = if (grid) (index / 4) * .86f + .04f else listOf(.24f, .18f, .27f, .47f, .55f, .47f, .73f, .79f)[index % 8]
            val selectedCompact = compact && stop.id == selectedStopId
            Offset(if (selectedCompact) .5f else x, if (selectedCompact) .5f else y)
        }
        Canvas(Modifier.fillMaxSize()) {
            val visiblePx = mapHeight.toPx()
            val mapPoints = positions.mapIndexed { index, point ->
                val regionHeight = if (compact && ordered[index].id != selectedStopId) size.height else visiblePx
                Offset(24.dp.toPx() + point.x * (size.width - 48.dp.toPx()), 24.dp.toPx() + point.y * (regionHeight - 48.dp.toPx()))
            }
            val streetColor = colors.surface.copy(alpha = .8f)
            for (i in 0..7) {
                val road = Path().apply {
                    moveTo(-40f, size.height * i / 7f)
                    cubicTo(size.width * .35f, size.height * i / 7f + 65f, size.width * .6f, size.height * i / 7f - 95f, size.width + 40f, size.height * i / 7f + 30f)
                }
                drawPath(road, streetColor, style = Stroke(15.dp.toPx()))
            }
            for (i in 0..4) {
                drawLine(streetColor, Offset(size.width * i / 4, 0f), Offset(size.width * i / 4 + size.width * .2f, size.height), 10.dp.toPx())
            }
            if (mapPoints.isNotEmpty()) {
                val routePath = Path().apply {
                    moveTo(mapPoints.first().x, mapPoints.first().y)
                    mapPoints.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(routePath, colors.primary.copy(alpha = .65f), style = Stroke(4.dp.toPx()))
            }
        }
        ordered.forEachIndexed { index, stop ->
            val selected = stop.id == selectedStopId
            val completed = stop.status == StopStatus.COMPLETED
            val next = stop.id == nextStopId
            val point = positions[index]
            val regionHeight = if (compact && !selected) maxHeight else mapHeight
            val x = (maxWidth - 48.dp) * point.x
            val y = (regionHeight - 48.dp) * point.y
            val status = when { completed -> "concluída"; next -> "próxima parada"; else -> "pendente" }
            Box(
                Modifier.offset(x, y).size(48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Parada ${stop.sequence}, ${stop.name}, $status${if (selected) ", selecionada" else ""}"
                    }
                    .clickable(role = Role.Button, onClick = { onSelectStop(stop.id) }),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(if (selected) 44.dp else 36.dp)
                        .background(if (completed) colors.tertiary else if (selected || next) colors.primary else colors.surface, CircleShape)
                        .border(if (selected) 3.dp else 1.dp, if (selected) colors.onPrimary else colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (completed) "✓${stop.sequence}" else stop.sequence.toString(),
                        color = if (completed) colors.onTertiary else if (selected || next) colors.onPrimary else colors.onSurface,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Mapa demonstrativo • sem GPS", Modifier.align(Alignment.TopCenter).padding(8.dp)
            .background(colors.surface.copy(alpha = .95f), MaterialTheme.shapes.small).padding(horizontal = 10.dp, vertical = 5.dp),
            color = colors.onSurface, style = MaterialTheme.typography.labelSmall)
    }
}
