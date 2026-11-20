package com.timecure.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp
)

/**
 * Semantic roles only. The alarm time is the hero: displayLarge on the ringing screen,
 * displayMedium in the list (large enough to read at 3 AM, small enough to survive 200% text scale).
 */
val TimecureTypography = Typography(
    displayLarge = style(FontWeight.Bold, 72, 80),
    displayMedium = style(FontWeight.SemiBold, 44, 52),
    headlineMedium = style(FontWeight.SemiBold, 28, 36),
    headlineSmall = style(FontWeight.SemiBold, 24, 32),
    titleLarge = style(FontWeight.Medium, 22, 28),
    titleMedium = style(FontWeight.Medium, 16, 24),
    titleSmall = style(FontWeight.Medium, 14, 20),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.Medium, 14, 20),
    labelMedium = style(FontWeight.Medium, 12, 16),
    labelSmall = style(FontWeight.Medium, 11, 16)
)
