package com.bettertick.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun BetterTickTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    if (ThemeState.systemInDark != systemDark) ThemeState.systemInDark = systemDark
    val accent = ThemeState.accentColor
    val p = ThemeState.palette

    val colorScheme = if (p.isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.24f),
            onPrimaryContainer = p.textPrimary,
            secondary = accent,
            onSecondary = Color.White,
            secondaryContainer = accent.copy(alpha = 0.18f),
            onSecondaryContainer = p.textPrimary,
            tertiary = accent,
            background = p.background,
            onBackground = p.textPrimary,
            surface = p.surface,
            onSurface = p.textPrimary,
            surfaceVariant = p.surfaceVariant,
            onSurfaceVariant = p.textSecondary,
            surfaceContainerLowest = p.background,
            surfaceContainerLow = p.surface,
            surfaceContainer = p.surface,
            surfaceContainerHigh = p.surface,
            surfaceContainerHighest = p.surfaceVariant,
            inverseSurface = Color(0xFFE6E6E6),
            inverseOnSurface = Color(0xFF191919),
            outline = p.textTertiary,
            outlineVariant = p.divider,
            error = OverdueRed,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.12f),
            onPrimaryContainer = p.textPrimary,
            secondary = accent,
            onSecondary = Color.White,
            secondaryContainer = accent.copy(alpha = 0.10f),
            onSecondaryContainer = p.textPrimary,
            tertiary = accent,
            background = p.background,
            onBackground = p.textPrimary,
            surface = p.surface,
            onSurface = p.textPrimary,
            surfaceVariant = p.surfaceVariant,
            onSurfaceVariant = p.textSecondary,
            surfaceContainerLowest = p.background,
            surfaceContainerLow = p.surface,
            surfaceContainer = p.surface,
            surfaceContainerHigh = p.surface,
            surfaceContainerHighest = p.surfaceVariant,
            inverseSurface = Color(0xFF323232),
            inverseOnSurface = Color.White,
            outline = p.textTertiary,
            outlineVariant = p.divider,
            error = OverdueRed,
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Overlay services host Compose in a non-Activity context — only
            // touch window chrome when we actually own an Activity window.
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = p.background.toArgb()
            window.navigationBarColor = p.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !p.isDark
            controller.isAppearanceLightNavigationBars = !p.isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BetterTickTypography,
        content = content
    )
}
