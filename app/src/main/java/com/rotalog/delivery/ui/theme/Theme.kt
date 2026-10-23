package com.rotalog.delivery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = RotaLogColors.BlueLight,
    onPrimary = RotaLogColors.DarkBackground,
    primaryContainer = Color(0xFF16386B),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = RotaLogColors.DarkMuted,
    onSecondary = RotaLogColors.DarkBackground,
    secondaryContainer = RotaLogColors.DarkSurfaceVariant,
    onSecondaryContainer = RotaLogColors.DarkText,
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF052E24),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = RotaLogColors.DarkBackground,
    onBackground = RotaLogColors.DarkText,
    surface = RotaLogColors.DarkSurface,
    onSurface = RotaLogColors.DarkText,
    surfaceVariant = RotaLogColors.DarkSurfaceVariant,
    onSurfaceVariant = RotaLogColors.DarkMuted,
    surfaceTint = RotaLogColors.BlueLight,
    outline = Color(0xFF64748B),
    outlineVariant = RotaLogColors.DarkBorder,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    surfaceDim = RotaLogColors.DarkBackground,
    surfaceBright = RotaLogColors.DarkSurfaceVariant,
    surfaceContainerLowest = RotaLogColors.DarkBackground,
    surfaceContainerLow = RotaLogColors.DarkSurface,
    surfaceContainer = RotaLogColors.DarkSurface,
    surfaceContainerHigh = RotaLogColors.DarkSurfaceVariant,
    surfaceContainerHighest = RotaLogColors.DarkBorder,
)

private val LightColorScheme = lightColorScheme(
    primary = RotaLogColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = RotaLogColors.LightMuted,
    onSecondary = Color.White,
    secondaryContainer = RotaLogColors.LightSurfaceVariant,
    onSecondaryContainer = RotaLogColors.LightText,
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = RotaLogColors.LightBackground,
    onBackground = RotaLogColors.LightText,
    surface = RotaLogColors.LightSurface,
    onSurface = RotaLogColors.LightText,
    surfaceVariant = RotaLogColors.LightSurfaceVariant,
    onSurfaceVariant = RotaLogColors.LightMuted,
    surfaceTint = RotaLogColors.Blue,
    outline = Color(0xFF64748B),
    outlineVariant = RotaLogColors.LightBorder,
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    surfaceDim = RotaLogColors.LightSurfaceVariant,
    surfaceBright = RotaLogColors.LightSurface,
    surfaceContainerLowest = RotaLogColors.LightSurface,
    surfaceContainerLow = RotaLogColors.LightBackground,
    surfaceContainer = RotaLogColors.LightSurfaceVariant,
    surfaceContainerHigh = RotaLogColors.LightSurfaceVariant,
    surfaceContainerHighest = RotaLogColors.LightBorder,
)

private val RotaLogShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun RotalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = RotaLogShapes,
        content = content,
    )
}
