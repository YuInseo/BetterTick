package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bettertick.data.model.LEAVE_TYPES
import com.bettertick.data.model.Task
import com.bettertick.data.model.leaveEntries
import java.time.LocalDate

/** How often a leave allowance resets. */
enum class LeavePeriod(val label: String) { MONTH("매달"), YEAR("매년") }

/**
 * 휴가 일수 (설정 > 일반 > 휴가 일수): 지휴 is granted per month, 연차 and
 * 병가 per year. 0 = 설정 안 함 (no remaining-days display).
 * [usage] is recounted from every task whenever the task list changes.
 */
object LeavePrefs {
    private const val PREFS = "leave_prefs"

    fun periodOf(type: String): LeavePeriod = if (type == "지휴") LeavePeriod.MONTH else LeavePeriod.YEAR

    var quotas by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** "type|period" → days used; period is "yyyy-MM" (월) or "yyyy" (년). */
    var usage by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        quotas = LEAVE_TYPES.associateWith { p.getInt("quota_$it", 0) }
    }

    fun quota(type: String): Int = quotas[type] ?: 0

    fun updateQuota(type: String, days: Int) {
        quotas = quotas + (type to days)
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putInt("quota_$type", days)?.apply()
    }

    private fun periodKey(type: String, day: LocalDate): String = when (periodOf(type)) {
        LeavePeriod.MONTH -> "%04d-%02d".format(day.year, day.monthValue)
        LeavePeriod.YEAR -> day.year.toString()
    }

    /** Recount usage from the full task list (one count per day and kind). */
    fun recount(tasks: List<Task>) {
        usage = countUsage(tasks)
    }

    fun countUsage(tasks: List<Task>): Map<String, Int> =
        tasks.flatMap { it.leaveEntries() }
            .distinct()
            .groupingBy { (day, type) -> "$type|${periodKey(type, day)}" }
            .eachCount()

    fun used(type: String, day: LocalDate): Int = usage["$type|${periodKey(type, day)}"] ?: 0

    /** Days left in the period containing [day]; null when no allowance is set. */
    fun remaining(type: String, day: LocalDate, extraUsed: Int = 0): Int? {
        val q = quota(type)
        if (q <= 0) return null
        return q - used(type, day) - extraUsed
    }

    /** "3일 남음" / "1일 초과" for the chip; null when no allowance is set. */
    fun remainingLabel(type: String, day: LocalDate, extraUsed: Int = 0): String? {
        val r = remaining(type, day, extraUsed) ?: return null
        return if (r >= 0) "${r}일 남음" else "${-r}일 초과"
    }
}
