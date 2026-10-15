package com.bettertick.ui.screens.habits

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bettertick.data.model.Habit
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 습관 > 설정 (TickTick 습관 설정), persisted. */
object HabitPrefs {
    private const val PREFS = "habit_prefs"
    val DEFAULT_GROUPS = listOf("기타", "오전", "오후", "밤")

    /** Notification tone for habit reminders: null = 기본값, "" = 무음. */
    var ringtone by mutableStateOf<String?>(null)
        private set
    var sortByCheckIn by mutableStateOf(false)
        private set
    var showInTodayNext7 by mutableStateOf(true)
        private set
    /** 열 관리: ordered habit sections. */
    var groups by mutableStateOf(DEFAULT_GROUPS)
        private set
    var collapsedGroups by mutableStateOf<Set<String>>(emptySet())
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ringtone = if (p.contains("ringtone")) p.getString("ringtone", null) else null
        sortByCheckIn = p.getBoolean("sort_check_in", false)
        showInTodayNext7 = p.getBoolean("show_in_today", true)
        groups = p.getString("groups", null)?.split('\n')?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() } ?: DEFAULT_GROUPS
        collapsedGroups = p.getStringSet("collapsed", emptySet()).orEmpty().toSet()
    }

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.apply(block)?.apply()
    }

    fun updateRingtone(v: String?) {
        ringtone = v
        edit { if (v == null) remove("ringtone") else putString("ringtone", v) }
    }
    fun updateSortByCheckIn(v: Boolean) { sortByCheckIn = v; edit { putBoolean("sort_check_in", v) } }
    fun updateShowInTodayNext7(v: Boolean) { showInTodayNext7 = v; edit { putBoolean("show_in_today", v) } }

    private fun saveGroups(list: List<String>) {
        groups = list
        edit { putString("groups", list.joinToString("\n")) }
    }
    fun addGroup(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || n in groups) return false
        saveGroups(groups + n)
        return true
    }
    fun removeGroup(name: String) {
        if (groups.size <= 1) return
        saveGroups(groups - name)
    }
    fun renameGroup(old: String, new: String): Boolean {
        val n = new.trim()
        if (n.isEmpty() || (n != old && n in groups)) return false
        saveGroups(groups.map { if (it == old) n else it })
        return true
    }
    fun moveGroup(name: String, delta: Int) {
        val i = groups.indexOf(name)
        val j = i + delta
        if (i < 0 || j !in groups.indices) return
        val list = groups.toMutableList()
        list[i] = list[j].also { list[j] = list[i] }
        saveGroups(list)
    }
    fun toggleCollapsed(name: String) {
        collapsedGroups = if (name in collapsedGroups) collapsedGroups - name else collapsedGroups + name
        edit { putStringSet("collapsed", collapsedGroups) }
    }

    /** Section a habit is shown under; unknown names fall into the first group. */
    fun groupOf(habit: Habit): String = habit.group.takeIf { it in groups } ?: groups.first()
}

/** Whether [this] habit should be checked in on [date] (매일 요일 / 매주 N회 / N일마다). */
fun Habit.isDueOn(date: LocalDate): Boolean {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
        ?: createdAt.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    if (date.isBefore(start)) return false
    return when (frequency) {
        "interval" -> ChronoUnit.DAYS.between(start, date) % intervalDays.coerceAtLeast(1) == 0L
        "weekly" -> true
        else -> targetDays.isEmpty() || date.dayOfWeek.value in targetDays
    }
}
