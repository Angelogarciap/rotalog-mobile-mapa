package com.rotalog.delivery.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small native vectors; no icon font or network asset is needed. */
object NavigationIcons {
    val Routes = ImageVector.Builder("Rotas", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 3f); lineTo(1f, 7f); lineTo(4f, 7f); lineTo(4f, 15f)
            curveTo(4f, 18.3f, 6.7f, 21f, 10f, 21f); lineTo(10f, 19f)
            curveTo(7.8f, 19f, 6f, 17.2f, 6f, 15f); lineTo(6f, 7f); lineTo(9f, 7f); close()
            moveTo(19f, 3f); lineTo(15f, 7f); lineTo(18f, 7f); lineTo(18f, 11f)
            curveTo(18f, 13.2f, 16.2f, 15f, 14f, 15f); lineTo(10f, 15f); lineTo(10f, 17f); lineTo(14f, 17f)
            curveTo(17.3f, 17f, 20f, 14.3f, 20f, 11f); lineTo(20f, 7f); lineTo(23f, 7f); close()
        }
    }.build()
    val History = ImageVector.Builder("Histórico", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f); curveTo(7.3f, 3f, 3.4f, 6.6f, 3f, 11f)
            lineTo(0f, 11f); lineTo(4f, 15f); lineTo(8f, 11f); lineTo(5f, 11f)
            curveTo(5.5f, 7.6f, 8.4f, 5f, 12f, 5f); curveTo(15.9f, 5f, 19f, 8.1f, 19f, 12f)
            curveTo(19f, 15.9f, 15.9f, 19f, 12f, 19f); curveTo(9.9f, 19f, 8f, 18.1f, 6.7f, 16.6f)
            lineTo(5.3f, 18f); curveTo(7f, 19.9f, 9.4f, 21f, 12f, 21f)
            curveTo(17f, 21f, 21f, 17f, 21f, 12f); curveTo(21f, 7f, 17f, 3f, 12f, 3f); close()
            moveTo(11f, 7f); lineTo(13f, 7f); lineTo(13f, 11.5f); lineTo(16f, 13.3f)
            lineTo(15f, 15f); lineTo(11f, 12.5f); close()
        }
    }.build()
    val Profile = ImageVector.Builder("Perfil", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 2f); curveTo(9.2f, 2f, 7f, 4.2f, 7f, 7f); curveTo(7f, 9.8f, 9.2f, 12f, 12f, 12f)
            curveTo(14.8f, 12f, 17f, 9.8f, 17f, 7f); curveTo(17f, 4.2f, 14.8f, 2f, 12f, 2f); close()
            moveTo(12f, 4f); curveTo(13.7f, 4f, 15f, 5.3f, 15f, 7f); curveTo(15f, 8.7f, 13.7f, 10f, 12f, 10f)
            curveTo(10.3f, 10f, 9f, 8.7f, 9f, 7f); curveTo(9f, 5.3f, 10.3f, 4f, 12f, 4f); close()
            moveTo(3f, 22f); lineTo(3f, 19f); curveTo(3f, 15.7f, 7f, 14f, 12f, 14f)
            curveTo(17f, 14f, 21f, 15.7f, 21f, 19f); lineTo(21f, 22f); close()
            moveTo(5f, 20f); lineTo(19f, 20f); lineTo(19f, 19f)
            curveTo(19f, 17f, 15f, 16f, 12f, 16f); curveTo(9f, 16f, 5f, 17f, 5f, 19f); close()
        }
    }.build()
}
