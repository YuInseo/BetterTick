package com.bettertick.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.DayOfWeek
import java.time.LocalTime

/** 설정 > 날짜 & 시간 > 시간 형식. */
enum class TimeFormat(val key: String, val label: String) {
    SYSTEM("system", "시스템 설정 따르기"),
    H12("12h", "12시간"),
    H24("24h", "24시간")
}

/** 설정 > 일반 > 새 할일의 기본 날짜. */
enum class DefaultDueDate(val key: String, val label: String) {
    TODAY("today", "오늘"),
    TOMORROW("tomorrow", "내일"),
    NONE("none", "없음")
}

/**
 * App-wide preferences from 설정 (날짜 & 시간 / 사운드 & 알림 / 일반),
 * persisted and observable like [ThemeState].
 */
object AppPrefs {
    private const val PREFS = "app_prefs"

    var weekStart by mutableStateOf(DayOfWeek.SUNDAY)
        private set
    var timeFormat by mutableStateOf(TimeFormat.SYSTEM)
        private set
    var defaultDueDate by mutableStateOf(DefaultDueDate.TODAY)
        private set
    var newTaskOnTop by mutableStateOf(false)
        private set
    var completionSound by mutableStateOf(true)
        private set
    var completionVibrate by mutableStateOf(true)
        private set
    /** Route of the tab opened on launch ("tasks", "calendar", …). */
    var startTab by mutableStateOf("tasks")
        private set

    /** null = system default notification sound, "" = silent. */
    var reminderRingtone by mutableStateOf<String?>(null)
        private set
    var dailySummary by mutableStateOf(false)
        private set
    /** Minutes after midnight for 매일 알림. */
    var dailySummaryMinutes by mutableStateOf(8 * 60)
        private set

    /** 기본 알림 for new timed tasks: minutes before start, -1 = 없음. */
    var defaultReminder by mutableStateOf(0)
        private set
    /** Remind new all-day tasks on the day at [allDayReminderMinutes]. */
    var allDayReminder by mutableStateOf(false)
        private set
    /** Time of day (minutes after midnight) all-day task reminders fire at. */
    var allDayReminderMinutes by mutableStateOf(9 * 60)
        private set

    /** Resolved at init from the device setting for [TimeFormat.SYSTEM]. */
    private var system24h = false
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        weekStart = runCatching { DayOfWeek.valueOf(p.getString("week_start", "SUNDAY")!!) }.getOrDefault(DayOfWeek.SUNDAY)
        timeFormat = TimeFormat.entries.firstOrNull { it.key == p.getString("time_format", null) } ?: TimeFormat.SYSTEM
        defaultDueDate = DefaultDueDate.entries.firstOrNull { it.key == p.getString("default_due", null) } ?: DefaultDueDate.TODAY
        newTaskOnTop = p.getBoolean("new_on_top", false)
        completionSound = p.getBoolean("completion_sound", true)
        completionVibrate = p.getBoolean("completion_vibrate", true)
        startTab = p.getString("start_tab", "tasks") ?: "tasks"
        reminderRingtone = if (p.contains("reminder_ringtone")) p.getString("reminder_ringtone", null) else null
        dailySummary = p.getBoolean("daily_summary", false)
        dailySummaryMinutes = p.getInt("daily_summary_minutes", 8 * 60)
        defaultReminder = p.getInt("default_reminder", 0)
        allDayReminder = p.getBoolean("all_day_reminder", false)
        allDayReminderMinutes = p.getInt("all_day_reminder_minutes", 9 * 60)
        system24h = runCatching { android.text.format.DateFormat.is24HourFormat(context) }.getOrDefault(false)
    }

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.apply(block)?.apply()
    }

    fun updateWeekStart(v: DayOfWeek) { weekStart = v; edit { putString("week_start", v.name) } }
    fun updateTimeFormat(v: TimeFormat) { timeFormat = v; edit { putString("time_format", v.key) } }
    fun updateDefaultDueDate(v: DefaultDueDate) { defaultDueDate = v; edit { putString("default_due", v.key) } }
    fun updateNewTaskOnTop(v: Boolean) { newTaskOnTop = v; edit { putBoolean("new_on_top", v) } }
    fun updateCompletionSound(v: Boolean) { completionSound = v; edit { putBoolean("completion_sound", v) } }
    fun updateCompletionVibrate(v: Boolean) { completionVibrate = v; edit { putBoolean("completion_vibrate", v) } }
    fun updateStartTab(v: String) { startTab = v; edit { putString("start_tab", v) } }
    fun updateReminderRingtone(v: String?) {
        reminderRingtone = v
        edit { if (v == null) remove("reminder_ringtone") else putString("reminder_ringtone", v) }
    }
    fun updateDailySummary(v: Boolean) { dailySummary = v; edit { putBoolean("daily_summary", v) } }
    fun updateDailySummaryMinutes(v: Int) { dailySummaryMinutes = v; edit { putInt("daily_summary_minutes", v) } }

    fun updateDefaultReminder(v: Int) { defaultReminder = v; edit { putInt("default_reminder", v) } }
    fun updateAllDayReminder(v: Boolean) { allDayReminder = v; edit { putBoolean("all_day_reminder", v) } }
    fun updateAllDayReminderMinutes(v: Int) { allDayReminderMinutes = v; edit { putInt("all_day_reminder_minutes", v) } }

    /** Reminder offsets a new task gets when the user didn't pick any. */
    fun defaultRemindersFor(hasDate: Boolean, hasTime: Boolean): List<Int> = when {
        hasTime -> if (defaultReminder >= 0) listOf(defaultReminder) else emptyList()
        hasDate && allDayReminder -> listOf(0)
        else -> emptyList()
    }

    val is24h: Boolean
        get() = when (timeFormat) {
            TimeFormat.H24 -> true
            TimeFormat.H12 -> false
            TimeFormat.SYSTEM -> system24h
        }

    /** "오후 3:05" or "15:05" following 시간 형식. */
    fun formatTime(t: LocalTime): String =
        if (is24h) "%d:%02d".format(t.hour, t.minute)
        else {
            val ampm = if (t.hour < 12) "오전" else "오후"
            val h = when {
                t.hour == 0 -> 12
                t.hour > 12 -> t.hour - 12
                else -> t.hour
            }
            "$ampm $h:%02d".format(t.minute)
        }

    /** Korean single-letter weekday labels in the user's week order. */
    fun weekdayLabels(): List<String> {
        val all = listOf("일", "월", "화", "수", "목", "금", "토") // Sunday-first
        val shift = weekStart.value % 7 // SUNDAY(7)→0, MONDAY(1)→1, SATURDAY(6)→6
        return all.drop(shift) + all.take(shift)
    }

    /** Offset of [day] from the start of its week (0..6). */
    fun dayIndexInWeek(day: DayOfWeek): Int = ((day.value % 7) - (weekStart.value % 7) + 7) % 7
}
