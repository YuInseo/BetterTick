package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bettertick.data.model.LEAVE_TYPES
import com.bettertick.data.model.Task
import com.bettertick.data.model.leaveEntries
import java.time.LocalDate

/** How often a leave allowance refills — chosen by the user per kind. */
enum class LeavePeriod(val key: String, val label: String) {
    MONTH("month", "매달"),
    YEAR("year", "매년"),
    NONE("none", "리셋 없음")
}

/**
 * 휴가 일수 (설정 > 일반 > 휴가 일수). For each kind (지휴 / 연차 / 병가) the
 * user picks how often it refills ([LeavePeriod]), a default number of days
 * and, optionally, a different number for particular years (연도별 일수).
 * 0 days = 설정 안 함 (no remaining-days display).
 * [usage] is recounted from every task whenever the task list changes.
 */
object LeavePrefs {
    private const val PREFS = "leave_prefs"

    private fun defaultPeriod(type: String) = if (type == "지휴") LeavePeriod.MONTH else LeavePeriod.YEAR

    var periods by mutableStateOf<Map<String, LeavePeriod>>(emptyMap())
        private set
    /** Default days per period. */
    var quotas by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    /** 연도별 일수: type → (year → days), overriding [quotas] for that year. */
    var yearQuotas by mutableStateOf<Map<String, Map<Int, Int>>>(emptyMap())
        private set

    /** "type|period" → days used; period is "yyyy-MM", "yyyy" or "all". */
    var usage by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private var lastTasks: List<Task> = emptyList()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        periods = LEAVE_TYPES.associateWith { t ->
            LeavePeriod.entries.firstOrNull { it.key == p.getString("period_$t", null) } ?: defaultPeriod(t)
        }
        quotas = LEAVE_TYPES.associateWith { p.getInt("quota_$it", 0) }
        yearQuotas = LEAVE_TYPES.associateWith { t -> parseYears(p.getString("years_$t", null)) }
    }

    private fun parseYears(raw: String?): Map<Int, Int> =
        raw.orEmpty().split(',').mapNotNull { e ->
            val (y, d) = e.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val year = y.trim().toIntOrNull() ?: return@mapNotNull null
            val days = d.trim().toIntOrNull() ?: return@mapNotNull null
            year to days
        }.toMap()

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.apply(block)?.apply()
    }

    fun periodOf(type: String): LeavePeriod = periods[type] ?: defaultPeriod(type)

    fun updatePeriod(type: String, period: LeavePeriod) {
        periods = periods + (type to period)
        edit { putString("period_$type", period.key) }
        recount(lastTasks)
    }

    /** Default days (0 = 설정 안 함). */
    fun baseQuota(type: String): Int = quotas[type] ?: 0

    fun updateQuota(type: String, days: Int) {
        quotas = quotas + (type to days.coerceAtLeast(0))
        edit { putInt("quota_$type", days.coerceAtLeast(0)) }
    }

    /** Days for [year] if the user set one for that year, else null. */
    fun yearQuota(type: String, year: Int): Int? = yearQuotas[type]?.get(year)

    /** Set (or with null, clear) the 연도별 일수 for [year]. */
    fun updateYearQuota(type: String, year: Int, days: Int?) {
        val cur = yearQuotas[type].orEmpty()
        val next = if (days == null) cur - year else cur + (year to days.coerceAtLeast(0))
        yearQuotas = yearQuotas + (type to next)
        edit { putString("years_$type", next.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }) }
    }

    /** Allowance that applies on [day]: that year's own number, else the default. */
    fun quota(type: String, day: LocalDate = LocalDate.now()): Int =
        if (periodOf(type) == LeavePeriod.NONE) baseQuota(type)
        else yearQuota(type, day.year) ?: baseQuota(type)

    private fun periodKey(type: String, day: LocalDate): String = when (periodOf(type)) {
        LeavePeriod.MONTH -> "%04d-%02d".format(day.year, day.monthValue)
        LeavePeriod.YEAR -> day.year.toString()
        LeavePeriod.NONE -> "all"
    }

    /** Recount usage from the full task list (one count per day and kind). */
    fun recount(tasks: List<Task>) {
        lastTasks = tasks
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
        val q = quota(type, day)
        if (q <= 0) return null
        return q - used(type, day) - extraUsed
    }

    /** "3일 남음" / "1일 초과" for the chip; null when no allowance is set. */
    fun remainingLabel(type: String, day: LocalDate, extraUsed: Int = 0): String? {
        val r = remaining(type, day, extraUsed) ?: return null
        return if (r >= 0) "${r}일 남음" else "${-r}일 초과"
    }

    /** "이번 달" / "올해" / "전체" — the period [day] falls in, for labels. */
    fun periodName(type: String): String = when (periodOf(type)) {
        LeavePeriod.MONTH -> "이번 달"
        LeavePeriod.YEAR -> "올해"
        LeavePeriod.NONE -> "전체"
    }
}
