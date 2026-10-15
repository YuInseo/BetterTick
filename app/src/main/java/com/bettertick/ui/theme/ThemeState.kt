package com.bettertick.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * App-wide appearance state, mirroring TickTick's 설정 > 외관:
 *  - a color theme (accent) — 기본값 is TickTick blue on a light page,
 *  - light vs dark (어둠) pages,
 *  - "시스템 어두운 모드 따르기" which switches to dark when the OS is dark.
 *
 * Backed by snapshot state so every token getter in Color.kt recomposes
 * when the user applies a theme, and persisted to SharedPreferences so the
 * choice survives restarts.
 */
object ThemeState {
    private const val PREFS = "bettertick_theme"
    private const val KEY_ACCENT = "accent"
    private const val KEY_DARK = "dark"
    private const val KEY_FOLLOW_SYSTEM = "follow_system"
    private const val KEY_NAME = "name"

    const val DEFAULT_THEME_NAME = "기본값"
    const val DARK_THEME_NAME = "어둠"

    var accentColor by mutableStateOf(TickBlue)
        private set
    var themeName by mutableStateOf(DEFAULT_THEME_NAME)
        private set
    /** User picked a dark theme (어둠). */
    var darkTheme by mutableStateOf(false)
        private set
    var followSystemDark by mutableStateOf(false)
        private set
    /** Updated by [BetterTickTheme] from the OS configuration. */
    var systemInDark by mutableStateOf(false)

    val isDark: Boolean get() = darkTheme || (followSystemDark && systemInDark)
    val palette: TickPalette get() = if (isDark) TickDarkPalette else TickLightPalette

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_ACCENT)) accentColor = Color(prefs.getInt(KEY_ACCENT, TickBlue.toArgb()))
        darkTheme = prefs.getBoolean(KEY_DARK, false)
        followSystemDark = prefs.getBoolean(KEY_FOLLOW_SYSTEM, false)
        themeName = prefs.getString(KEY_NAME, null)
            ?: if (darkTheme) DARK_THEME_NAME else DEFAULT_THEME_NAME
    }

    /** Apply a color theme. [dark] = true for the 어둠 theme. */
    fun applyTheme(name: String, accent: Color, dark: Boolean) {
        themeName = name
        accentColor = accent
        darkTheme = dark
        save()
    }

    fun setFollowSystemDark(enabled: Boolean) {
        followSystemDark = enabled
        save()
    }

    /** Back-compat: change only the accent, keeping the current mode. */
    fun setTheme(color: Color) {
        accentColor = color
        save()
    }

    private fun save() {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putInt(KEY_ACCENT, accentColor.toArgb())
            ?.putBoolean(KEY_DARK, darkTheme)
            ?.putBoolean(KEY_FOLLOW_SYSTEM, followSystemDark)
            ?.putString(KEY_NAME, themeName)
            ?.apply()
    }
}
