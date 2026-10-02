package com.bettertick.data.repository

import android.content.Context
import com.bettertick.data.model.ShiftHours
import com.bettertick.data.model.normalizeShiftLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 근무 라벨(주/야간/비/휴 …)별 달력 칩 색. 라벨은 [normalizeShiftLabel]로
 * 정규화해 저장하므로 "주 (병가)"도 "주"의 색을 따른다.
 * 기기 로컬(SharedPreferences) 저장 — 값은 ARGB Long.
 * 근무 시간([ShiftHours])도 같은 방식으로 라벨별 저장한다.
 */
object ShiftColorStore {
    private const val PREFS = "shift_colors"

    private val _colors = MutableStateFlow<Map<String, Long>>(emptyMap())
    @Volatile private var loaded = false

    fun colors(context: Context): StateFlow<Map<String, Long>> {
        if (!loaded) {
            synchronized(this) {
                if (!loaded) {
                    val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    _colors.value = prefs.all.mapNotNull { (k, v) -> (v as? Long)?.let { k to it } }.toMap()
                    loaded = true
                }
            }
        }
        return _colors.asStateFlow()
    }

    /** [argb]가 null이면 기본색으로 되돌린다. */
    fun setColor(context: Context, label: String, argb: Long?) {
        val key = normalizeShiftLabel(label)
        if (key.isEmpty()) return
        colors(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (argb == null) remove(key) else putLong(key, argb)
        }.apply()
        _colors.update { if (argb == null) it - key else it + (key to argb) }
    }

    fun colorFor(colors: Map<String, Long>, title: String): Long? =
        colors[normalizeShiftLabel(title)]

    // ── 근무 시간 ────────────────────────────────────────────────
    private const val HOURS_PREFS = "shift_hours"
    private val _hours = MutableStateFlow<Map<String, ShiftHours>>(emptyMap())
    @Volatile private var hoursLoaded = false

    fun hours(context: Context): StateFlow<Map<String, ShiftHours>> {
        if (!hoursLoaded) {
            synchronized(this) {
                if (!hoursLoaded) {
                    val prefs = context.applicationContext.getSharedPreferences(HOURS_PREFS, Context.MODE_PRIVATE)
                    _hours.value = prefs.all.mapNotNull { (k, v) ->
                        ShiftHours.parse(v as? String)?.let { k to it }
                    }.toMap()
                    hoursLoaded = true
                }
            }
        }
        return _hours.asStateFlow()
    }

    /** [value]가 null이면 종일(시간 없음)로 되돌린다. */
    fun setHours(context: Context, label: String, value: ShiftHours?) {
        val key = normalizeShiftLabel(label)
        if (key.isEmpty()) return
        hours(context)
        val prefs = context.applicationContext.getSharedPreferences(HOURS_PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value.serialize())
        }.apply()
        _hours.update { if (value == null) it - key else it + (key to value) }
    }

    fun hoursFor(hours: Map<String, ShiftHours>, title: String): ShiftHours? =
        hours[normalizeShiftLabel(title)]
}
