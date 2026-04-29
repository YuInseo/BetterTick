package com.bettertick.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemeState {
    var accentColor by mutableStateOf(Orange)

    fun setTheme(color: Color) {
        accentColor = color
    }
}
