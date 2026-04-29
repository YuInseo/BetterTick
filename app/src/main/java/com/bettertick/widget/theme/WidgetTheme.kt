package com.bettertick.widget.theme

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

// Glance widget color providers (dark theme only)
object WidgetColors {
    val background: ColorProvider = ColorProvider(Color(0xFF121212))
    val surface: ColorProvider = ColorProvider(Color(0xFF1E1E1E))
    val card: ColorProvider = ColorProvider(Color(0xFF252525))
    val surfaceVariant: ColorProvider = ColorProvider(Color(0xFF2C2C2C))
    val accent: ColorProvider = ColorProvider(Color(0xFFFF8C00))
    val textPrimary: ColorProvider = ColorProvider(Color.White)
    val textSecondary: ColorProvider = ColorProvider(Color(0xFFB0B0B0))
    val textTertiary: ColorProvider = ColorProvider(Color(0xFF707070))
    val green: ColorProvider = ColorProvider(Color(0xFF4CAF50))
    val red: ColorProvider = ColorProvider(Color(0xFFEF5350))
    val yellow: ColorProvider = ColorProvider(Color(0xFFFFCA28))
    val blue: ColorProvider = ColorProvider(Color(0xFF42A5F5))
    val purple: ColorProvider = ColorProvider(Color(0xFFAB47BC))
    val pink: ColorProvider = ColorProvider(Color(0xFFEC407A))
    val transparent: ColorProvider = ColorProvider(Color.Transparent)
}

// Raw Color values for inline use
object WidgetColorValues {
    val background = Color(0xFF121212)
    val surface = Color(0xFF1E1E1E)
    val card = Color(0xFF252525)
    val surfaceVariant = Color(0xFF2C2C2C)
    val accent = Color(0xFFFF8C00)
    val textPrimary = Color.White
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)
    val green = Color(0xFF4CAF50)
    val red = Color(0xFFEF5350)
    val yellow = Color(0xFFFFCA28)
    val blue = Color(0xFF42A5F5)
    val purple = Color(0xFFAB47BC)
    val pink = Color(0xFFEC407A)
}
