package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bettertick.data.model.KoreanHolidays
import com.bettertick.data.model.LEAVE_TYPES
import com.bettertick.data.model.Task
import com.bettertick.data.model.isWorkShift
import com.bettertick.data.model.leaveEntries
import com.bettertick.data.model.redDaysWorked
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
 *
 * 지휴 can instead be earned automatically ([autoJihyu]): every red day of the
 * Korean calendar (공휴일·대체공휴일, optionally Sundays) with a work shift
 * (주간 / 야간 / 당직) and no leave taken gives one day in that period. The
 * user's own days are then added on top (이월 등).
 */
object LeavePrefs {
    private const val PREFS = "leave_prefs"
    private const val AUTO_TYPE = "지휴"

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

    /** 지휴 자동 계산: earned from shifts worked on red days. */
    var autoJihyu by mutableStateOf(true)
        private set
    /** Count Sundays as red days too (공휴일만 세려면 끔). */
    var includeSunday by mutableStateOf(true)
        private set
    /** Days the user marked by hand: date → true (빨간날로) / false (빨간날 아님). */
    var redOverrides by mutableStateOf<Map<LocalDate, Boolean>>(emptyMap())
        private set
    /** Red days worked (each one a 지휴), oldest first. */
    var earnedDays by mutableStateOf<List<LocalDate>>(emptyList())
        private set
    /** periodKey → 지휴 earned in that period. */
    private var earned by mutableStateOf<Map<String, Int>>(emptyMap())

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
        autoJihyu = p.getBoolean("auto_jihyu", true)
        includeSunday = p.getBoolean("red_sunday", true)
        redOverrides = p.getString("red_overrides", null).orEmpty().split(',').mapNotNull { e ->
            val (d, v) = e.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val day = runCatching { LocalDate.parse(d.trim()) }.getOrNull() ?: return@mapNotNull null
            day to (v.trim() == "1")
        }.toMap()
    }

    /** Is [type] earned from red-day work rather than a fixed allowance? */
    fun isAuto(type: String): Boolean = type == AUTO_TYPE && autoJihyu

    fun updateAutoJihyu(on: Boolean) {
        autoJihyu = on
        edit { putBoolean("auto_jihyu", on) }
        recount(lastTasks)
    }

    fun updateIncludeSunday(on: Boolean) {
        includeSunday = on
        edit { putBoolean("red_sunday", on) }
        recount(lastTasks)
    }

    /** Red on the Korean calendar, after the user's own corrections. */
    fun isRedDay(day: LocalDate): Boolean = redOverrides[day] ?: KoreanHolidays.isRedDay(day, includeSunday)

    /** Mark [day] red / not red by hand; null goes back to the calendar. */
    fun setRedOverride(day: LocalDate, red: Boolean?) {
        redOverrides = if (red == null || red == KoreanHolidays.isRedDay(day, includeSunday)) redOverrides - day
        else redOverrides + (day to red)
        edit {
            putString("red_overrides", redOverrides.entries.sortedBy { it.key }
                .joinToString(",") { "${it.key}:${if (it.value) 1 else 0}" })
        }
        recount(lastTasks)
    }

    /** "추석", "일요일", "직접 지정" … — why [day] is red; null when it is not. */
    fun redDayName(day: LocalDate): String? = when {
        redOverrides[day] == false -> null
        KoreanHolidays.name(day) != null -> KoreanHolidays.name(day)
        redOverrides[day] == true -> "직접 지정"
        includeSunday && day.dayOfWeek == java.time.DayOfWeek.SUNDAY -> "일요일"
        else -> null
    }

    /** 지휴 earned in the period containing [day]. */
    fun earnedIn(day: LocalDate): Int = earned[periodKey(AUTO_TYPE, day)] ?: 0

    /** The red days worked that fall in the period containing [day]. */
    fun earnedDaysIn(day: LocalDate): List<LocalDate> {
        val key = periodKey(AUTO_TYPE, day)
        return earnedDays.filter { periodKey(AUTO_TYPE, it) == key }
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
    fun quota(type: String, day: LocalDate = LocalDate.now()): Int {
        val own = if (periodOf(type) == LeavePeriod.NONE) baseQuota(type)
        else yearQuota(type, day.year) ?: baseQuota(type)
        // Auto 지휴: what red-day work earned, plus the user's own days on top.
        return if (isAuto(type)) earnedIn(day) + own else own
    }

    private fun periodKey(type: String, day: LocalDate): String = when (periodOf(type)) {
        LeavePeriod.MONTH -> "%04d-%02d".format(day.year, day.monthValue)
        LeavePeriod.YEAR -> day.year.toString()
        LeavePeriod.NONE -> "all"
    }

    /** Recount usage from the full task list (one count per day and kind). */
    fun recount(tasks: List<Task>) {
        lastTasks = tasks
        usage = countUsage(tasks)
        recountEarned(tasks)
    }

    private fun recountEarned(tasks: List<Task>) {
        if (!autoJihyu) {
            earnedDays = emptyList(); earned = emptyMap(); return
        }
        val today = LocalDate.now()
        val starts = tasks.filter { it.isWorkShift() }.mapNotNull {
            it.dueDate?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
        }
        val first = starts.minOrNull() ?: run { earnedDays = emptyList(); earned = emptyMap(); return }
        // 리셋 없음 only counts what has been worked; a monthly/yearly period also
        // counts the shifts already scheduled in it.
        val to = if (periodOf(AUTO_TYPE) == LeavePeriod.NONE) today else LocalDate.of(today.year + 1, 12, 31)
        val from = maxOf(first, today.minusYears(3).withDayOfYear(1))
        val days = redDaysWorked(tasks, from, to, ::isRedDay)
        earnedDays = days
        earned = days.groupingBy { periodKey(AUTO_TYPE, it) }.eachCount()
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
        if (q <= 0 && !isAuto(type)) return null
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
