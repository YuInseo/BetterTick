package com.bettertick.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.bettertick.data.repository.ShiftColorStore

/** 근무 라벨 색 팔레트 — 근무 패턴 반복 대화상자에서 고른다. */
val ShiftColorPalette: List<Long> = listOf(
    0xFFCC7000, 0xFFE53935, 0xFFD81B60, 0xFF8E24AA,
    0xFF5E35B1, 0xFF1E88E5, 0xFF00ACC1, 0xFF00897B,
    0xFF43A047, 0xFFC0CA33, 0xFFFDD835, 0xFF6D4C41,
    0xFF757575, 0xFF37474F
)

@Composable
fun rememberShiftColors(): Map<String, Long> {
    val context = LocalContext.current
    val flow = remember { ShiftColorStore.colors(context) }
    val colors by flow.collectAsState()
    return colors
}

/** 달력 칩 배경: 근무 라벨 색이 있으면 그 색(완료 시 어둡게), 없으면 기본색. */
fun shiftChipColor(
    colors: Map<String, Long>,
    title: String,
    isCompleted: Boolean,
    default: Color,
    completedDefault: Color
): Color {
    val custom = ShiftColorStore.colorFor(colors, title)?.let { Color(it) }
        ?: return if (isCompleted) completedDefault else default
    return if (isCompleted) lerp(custom, Color.Black, 0.4f) else custom
}

/** 밝은 배경(노랑 등)에서도 글자가 보이도록. */
fun chipTextColor(background: Color): Color =
    if (background.luminance() > 0.6f) Color.Black else Color.White
