package com.bettertick.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bettertick.R

/** Pretendard — the app's UI typeface (Korean + Latin, SIL OFL 1.1). */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

// Compact scale: 20sp bold page titles, 16sp list rows, 14sp section
// headers, 12sp meta. Titles are tracked slightly tight, small labels
// slightly loose.
private val Tight = (-0.02).em
private val Snug = (-0.01).em

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
    spacing: androidx.compose.ui.unit.TextUnit = 0.em
) = TextStyle(
    fontFamily = Pretendard,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing
)

val BetterTickTypography = Typography(
    displayLarge = style(57, 64, FontWeight.Bold, Tight),
    displayMedium = style(45, 52, FontWeight.Bold, Tight),
    displaySmall = style(36, 44, FontWeight.Bold, Tight),
    headlineLarge = style(26, 32, FontWeight.Bold, Tight),
    headlineMedium = style(22, 28, FontWeight.Bold, Tight),
    headlineSmall = style(20, 26, FontWeight.Bold, Tight),
    titleLarge = style(20, 26, FontWeight.Bold, Tight),
    titleMedium = style(16, 22, FontWeight.SemiBold, Snug),
    titleSmall = style(14, 20, FontWeight.SemiBold, Snug),
    bodyLarge = style(16, 22, spacing = Snug),
    bodyMedium = style(14, 20, spacing = Snug),
    bodySmall = style(12, 16),
    labelLarge = style(14, 20, FontWeight.Medium, Snug),
    labelMedium = style(12, 16),
    labelSmall = style(10, 14)
)
