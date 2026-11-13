package com.bettertick.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 내 색상 — colors the user added with the color picker (설정 > 일반 >
 * 색상 관리, or a picker's + button). Every color list in the app shows them
 * after its own presets: 목록, 태그, 습관, 캘린더, 근무 블록, 테마 색상.
 */
object UserColors {
    private const val PREFS = "user_colors"
    private const val KEY = "colors"

    /** ARGB ints, oldest first. */
    var colors by mutableStateOf<List<Int>>(emptyList())
        private set

    private var appContext: Context? = null

    private fun parse(raw: String?): List<Int> =
        raw.orEmpty().split(",").mapNotNull { it.trim().toLongOrNull()?.toInt() }.distinct()

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        colors = if (p.contains(KEY)) parse(p.getString(KEY, null)) else {
            // First run: bring over the colors added in the calendar's 사용자 정의.
            parse(context.getSharedPreferences("calendar_view_prefs", Context.MODE_PRIVATE)
                .getString("custom_colors", null)).also { save(it) }
        }
    }

    private fun save(list: List<Int>) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY, list.joinToString(",") { (it.toLong() and 0xFFFFFFFFL).toString() })
            ?.apply()
    }

    fun add(c: Color) {
        val argb = c.copy(alpha = 1f).toArgb()
        if (argb in colors) return
        colors = colors + argb
        save(colors)
    }

    fun remove(argb: Int) {
        colors = colors - argb
        save(colors)
    }

    /** "#RRGGBB" for pickers that store hex strings. */
    fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)
}
