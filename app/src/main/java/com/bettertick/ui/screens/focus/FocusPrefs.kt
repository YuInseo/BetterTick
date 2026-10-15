package com.bettertick.ui.screens.focus

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

enum class FocusMode(val key: String, val label: String) {
    POMO("pomo", "뽀모도로"),
    STOPWATCH("stopwatch", "스톱워치")
}

enum class FocusPhase(val label: String) {
    FOCUS("집중"),
    SHORT_BREAK("짧은 휴식"),
    LONG_BREAK("긴 휴식")
}

/** 뽀모도로 > 설정 (TickTick 포모 설정), persisted. */
object FocusPrefs {
    private const val PREFS = "focus_prefs"

    var mode by mutableStateOf(FocusMode.POMO)
        private set
    var pomoMinutes by mutableStateOf(25)
        private set
    var shortBreakMinutes by mutableStateOf(5)
        private set
    var longBreakMinutes by mutableStateOf(15)
        private set
    var longBreakInterval by mutableStateOf(4)
        private set
    var autoStartNextPomo by mutableStateOf(false)
        private set
    var autoStartBreak by mutableStateOf(false)
        private set
    var dailyGoal by mutableStateOf(4)
        private set
    var keepScreenOn by mutableStateOf(true)
        private set
    var endSound by mutableStateOf(true)
        private set
    var endVibrate by mutableStateOf(true)
        private set

    /** Pomos finished today (reset when the date changes). */
    var pomosToday by mutableStateOf(0)
        private set
    private var pomosDate: String = ""

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        mode = FocusMode.entries.firstOrNull { it.key == p.getString("mode", null) } ?: FocusMode.POMO
        pomoMinutes = p.getInt("pomo", 25)
        shortBreakMinutes = p.getInt("short_break", 5)
        longBreakMinutes = p.getInt("long_break", 15)
        longBreakInterval = p.getInt("long_interval", 4)
        autoStartNextPomo = p.getBoolean("auto_pomo", false)
        autoStartBreak = p.getBoolean("auto_break", false)
        dailyGoal = p.getInt("daily_goal", 4)
        keepScreenOn = p.getBoolean("keep_screen_on", true)
        endSound = p.getBoolean("end_sound", true)
        endVibrate = p.getBoolean("end_vibrate", true)
        pomosDate = p.getString("pomos_date", "") ?: ""
        pomosToday = if (pomosDate == LocalDate.now().toString()) p.getInt("pomos_today", 0) else 0
    }

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.apply(block)?.apply()
    }

    fun updateMode(v: FocusMode) { mode = v; edit { putString("mode", v.key) } }
    fun updatePomoMinutes(v: Int) { pomoMinutes = v; edit { putInt("pomo", v) } }
    fun updateShortBreak(v: Int) { shortBreakMinutes = v; edit { putInt("short_break", v) } }
    fun updateLongBreak(v: Int) { longBreakMinutes = v; edit { putInt("long_break", v) } }
    fun updateLongInterval(v: Int) { longBreakInterval = v; edit { putInt("long_interval", v) } }
    fun updateAutoStartNextPomo(v: Boolean) { autoStartNextPomo = v; edit { putBoolean("auto_pomo", v) } }
    fun updateAutoStartBreak(v: Boolean) { autoStartBreak = v; edit { putBoolean("auto_break", v) } }
    fun updateDailyGoal(v: Int) { dailyGoal = v; edit { putInt("daily_goal", v) } }
    fun updateKeepScreenOn(v: Boolean) { keepScreenOn = v; edit { putBoolean("keep_screen_on", v) } }
    fun updateEndSound(v: Boolean) { endSound = v; edit { putBoolean("end_sound", v) } }
    fun updateEndVibrate(v: Boolean) { endVibrate = v; edit { putBoolean("end_vibrate", v) } }

    fun recordPomo() {
        val today = LocalDate.now().toString()
        if (pomosDate != today) { pomosDate = today; pomosToday = 0 }
        pomosToday += 1
        edit { putString("pomos_date", today); putInt("pomos_today", pomosToday) }
    }

    fun minutesFor(phase: FocusPhase): Int = when (phase) {
        FocusPhase.FOCUS -> pomoMinutes
        FocusPhase.SHORT_BREAK -> shortBreakMinutes
        FocusPhase.LONG_BREAK -> longBreakMinutes
    }
}
