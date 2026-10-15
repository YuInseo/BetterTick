package com.bettertick.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// TickTick design tokens
//
// TickTick ships a light "기본값" (Default) theme — white pages, near-black
// text, cobalt-blue (#4772FA) accent — plus a "어둠" (Dark) theme with
// near-black pages and #1C1C1E elevated surfaces. Every surface/text token
// below resolves against the active palette in [ThemeState], so the whole
// app flips between the two without per-screen branching.
//
// The token names are kept from the original dark-only theme so existing
// call sites keep compiling; read them as "background / surface / card",
// not literally "dark".
// ─────────────────────────────────────────────────────────────────────────────

/** TickTick brand blue — default accent of both 기본값 and 어둠 themes. */
val TickBlue = Color(0xFF4772FA)

// Priority colors (TickTick list view: checkbox border + flag tint).
val PriorityHigh = Color(0xFFD52B24)
val PriorityMedium = Color(0xFFFAA80C)
val PriorityLow = Color(0xFF4772FA)

/** Checkbox / flag color for a task priority (0 none, 1 low, 2 medium, 3 high). */
fun priorityColor(level: Int): Color = when (level) {
    3 -> PriorityHigh
    2 -> PriorityMedium
    1 -> PriorityLow
    else -> TextTertiary
}

/** Surface + text palette for one TickTick appearance mode. */
data class TickPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
)

val TickLightPalette = TickPalette(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F3F5),
    card = Color(0xFFF5F6F8),
    textPrimary = Color(0xFF191919),
    textSecondary = Color(0xFF7F7F7F),
    textTertiary = Color(0xFFBFBFBF),
    divider = Color(0xFFEDEDED),
)

val TickDarkPalette = TickPalette(
    isDark = true,
    background = Color(0xFF121212),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    card = Color(0xFF1C1C1E),
    textPrimary = Color(0xFFE6E6E6),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF5C5C60),
    divider = Color(0xFF2A2A2C),
)

// Legacy accent names (still referenced by the theme picker).
val Orange = Color(0xFFFF8C00)
val OrangeDark = Color(0xFFCC7000)
val OrangeLight = Color(0xFFFFAD42)

// Background hierarchy — theme-aware.
val DarkBackground: Color get() = ThemeState.palette.background
val DarkSurface: Color get() = ThemeState.palette.surface
val DarkSurfaceVariant: Color get() = ThemeState.palette.surfaceVariant
val DarkCard: Color get() = ThemeState.palette.card
val DividerColor: Color get() = ThemeState.palette.divider

// Text — theme-aware.
val TextPrimary: Color get() = ThemeState.palette.textPrimary
val TextSecondary: Color get() = ThemeState.palette.textSecondary
val TextTertiary: Color get() = ThemeState.palette.textTertiary

// Semantic
val CompletedGreen = Color(0xFF35B36A)
val OverdueRed = Color(0xFFE5483F)
val WarningYellow = PriorityMedium
val AbandonedBlue = TickBlue

// Grouped (settings-style) surfaces. TickTick's settings / statistics pages
// put white cards on a light-gray page in light mode; in dark mode they use
// the regular page + card colors.
val GroupedBackground: Color
    get() = if (ThemeState.isDark) ThemeState.palette.background else Color(0xFFF3F4F6)
val GroupedCard: Color
    get() = if (ThemeState.isDark) ThemeState.palette.card else Color(0xFFFFFFFF)
