package com.rotalog.delivery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rotalog.delivery.R

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = 0.sp,
)

/** All sizes use sp so system accessibility settings apply throughout the app. */
val Typography = Typography(
    displayLarge = type(48, 56, FontWeight.Bold),
    displayMedium = type(40, 48, FontWeight.Bold),
    displaySmall = type(36, 44, FontWeight.Bold),
    headlineLarge = type(32, 40, FontWeight.Bold),
    headlineMedium = type(28, 36, FontWeight.Bold),
    headlineSmall = type(24, 32, FontWeight.Bold),
    titleLarge = type(22, 30, FontWeight.SemiBold),
    titleMedium = type(18, 26, FontWeight.SemiBold),
    titleSmall = type(16, 24, FontWeight.SemiBold),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 22),
    bodySmall = type(12, 18),
    labelLarge = type(16, 24, FontWeight.SemiBold),
    labelMedium = type(12, 18, FontWeight.SemiBold),
    labelSmall = type(11, 16, FontWeight.SemiBold),
)
