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
import com.bettertick.data.model.serviceYearOn
import com.bettertick.data.model.serviceYearRange

/** How often a leave allowance refills — chosen by the user per kind. */
enum class LeavePeriod(val key: String, val label: String) {
    MONTH("month", "매달"),
    YEAR("year", "매년 (1월 1일)"),
    SERVICE_YEAR("service_year", "근무 시작일 기준"),
    NONE("none", "리셋 없음")
}

/**
 * 근무지: a company (or post) with its own leave rules. Each leave record
 * belongs to the workplace whose period contains its day, so moving jobs keeps
 * "how leave worked at the old place" as history.
 *
 * Coverage of a day: the workplace that has started (start ≤ day) and not yet
 * ended (end ≥ day), latest start first. Days before every workplace started
 * belong to the earliest one, so a single workplace covers everything — as
 * before workplaces existed. Gaps between jobs belong to none.
 */
data class Workplace(
    val id: String,
    val name: String,
    /** 근무 시작일 — also the anchor for 근무 시작일 기준 periods. */
    val start: LocalDate? = null,
    /** 퇴사일; null = 재직 중. */
    val end: LocalDate? = null,
    val periods: Map<String, LeavePeriod> = emptyMap(),
    /** Default days per period. */
    val quotas: Map<String, Int> = emptyMap(),
    /** 연도별 일수: type → (year → days). */
    val yearQuotas: Map<String, Map<Int, Int>> = emptyMap(),
    /** 근속연차별 일수: type → (service year → days). */
    val serviceQuotas: Map<String, Map<Int, Int>> = emptyMap()
) {
    fun covers(day: LocalDate): Boolean =
        (start == null || !day.isBefore(start)) && (end == null || !day.isAfter(end))

    /** "2025-09-10 ~ 재직 중" */
    val rangeLabel: String
        get() = "${start?.toString() ?: "처음"} ~ ${end?.toString() ?: "재직 중"}"
}

/** One period of one leave kind at one workplace, for the 기록 screen. */
data class LeaveHistoryPeriod(
    val type: String,
    /** "2026년", "2026년 9월", "2년차 (2026-09-11 ~ 2027-09-10)", "전체" */
    val label: String,
    /** Days given for the period; null when no allowance is set. */
    val allowance: Int?,
    val used: Int,
    /** Days the leave was taken, oldest first. */
    val days: List<LocalDate>,
    val firstDay: LocalDate
)

/**
 * 휴가 일수 (설정 > 일반 > 휴가 일수). For each kind (지휴 / 연차 / 병가) the
 * user picks how often it refills ([LeavePeriod]), a default number of days
 * and, optionally, a different number for particular years (연도별 일수).
 * 0 days = 설정 안 함 (no remaining-days display).
 * [usage] is recounted from every task whenever the task list changes.
 *
 * 지휴 can instead be earned automatically ([autoJihyu]): every red day of the
 * Korean calendar (공휴일·대체공휴일, optionally Sundays) with a work shift
 * (주간 / 야간 / 당직 / 일근) and no leave taken gives one day in that period. The
 * user's own days are then added on top (이월 등).
 */
object LeavePrefs {
    private const val PREFS = "leave_prefs"
    private const val AUTO_TYPE = "지휴"
    /** Shift kinds that earn 지휴 on a red day, in display order. */
    val WEIGHT_KINDS = listOf("주", "야", "당직", "일근")
    /** Shift kinds with their own block color, in display order. */
    val COLOR_KINDS = listOf("주", "야", "비", "휴", "당직", "일근")
    private val DEFAULT_WEIGHTS = mapOf("주" to 1, "야" to 2, "당직" to 1, "일근" to 1)

    private fun defaultPeriod(type: String) = if (type == "지휴") LeavePeriod.MONTH else LeavePeriod.YEAR

    private const val DEFAULT_WORKPLACE_ID = "default"

    /** 근무지 목록, oldest start first. Never empty. */
    var workplaces by mutableStateOf(listOf(Workplace(DEFAULT_WORKPLACE_ID, "내 회사")))
        private set
    /** The workplace whose rules the settings screens are editing. */
    var editingId by mutableStateOf(DEFAULT_WORKPLACE_ID)
        private set

    val editing: Workplace get() = workplaces.firstOrNull { it.id == editingId } ?: workplaces.first()

    /** The workplace [day] belongs to; null for a gap between jobs. */
    fun workplaceOn(day: LocalDate): Workplace? {
        workplaces.filter { it.covers(day) }.maxByOrNull { it.start ?: LocalDate.MIN }?.let { return it }
        val earliest = workplaces.minByOrNull { it.start ?: LocalDate.MIN } ?: return null
        val start = earliest.start ?: return null
        return if (day.isBefore(start)) earliest else null
    }

    /** Workplace that covers today (or, between jobs, the latest one). */
    val currentWorkplace: Workplace
        get() = workplaceOn(LocalDate.now()) ?: workplaces.maxByOrNull { it.start ?: LocalDate.MIN }!!

    /** A day inside the editing workplace — today when it's the current one. */
    fun referenceDay(): LocalDate {
        val today = LocalDate.now()
        val wp = editing
        return if (workplaceOn(today)?.id == wp.id) today else wp.end ?: wp.start ?: today
    }

    // Views of the editing workplace's rules (read by the settings screens).
    val periods: Map<String, LeavePeriod> get() = editing.periods
    val quotas: Map<String, Int> get() = editing.quotas
    val yearQuotas: Map<String, Map<Int, Int>> get() = editing.yearQuotas
    /** 근무 시작일 of the editing workplace. */
    val workStartDate: LocalDate? get() = editing.start
    val serviceQuotas: Map<String, Map<Int, Int>> get() = editing.serviceQuotas

    /** "type|period" → days used; period is "yyyy-MM", "yyyy" or "all". */
    var usage by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** 지휴 자동 계산: earned from shifts worked on red days. */
    var autoJihyu by mutableStateOf(true)
        private set
    /** Count Sundays as red days too (공휴일만 세려면 끔). */
    var includeSunday by mutableStateOf(true)
        private set
    /** Count Saturdays as red days too (off by default). */
    var includeSaturday by mutableStateOf(false)
        private set
    /** Days the user marked by hand: date → true (빨간날로) / false (빨간날 아님). */
    var redOverrides by mutableStateOf<Map<LocalDate, Boolean>>(emptyMap())
        private set
    /** 지휴 days one red-day shift earns, by kind: 주 1 · 야 2 · 당직 1 by default. */
    var shiftWeights by mutableStateOf(DEFAULT_WEIGHTS)
        private set
    /** 근무 블록 색상: shift kind → ARGB the user picked (absent = default). */
    var shiftColors by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    /** Red days worked with the shift kind worked, oldest first. */
    var earnedDays by mutableStateOf<List<Pair<LocalDate, String>>>(emptyList())
        private set
    /** periodKey → 지휴 earned in that period. */
    private var earned by mutableStateOf<Map<String, Int>>(emptyMap())

    private var lastTasks: List<Task> = emptyList()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = p.getString("workplaces", null)?.let { decodeWorkplaces(it) }.orEmpty()
        workplaces = stored.ifEmpty {
            // First run with 근무지: the one set of rules becomes the first workplace.
            listOf(
                Workplace(
                    id = DEFAULT_WORKPLACE_ID,
                    name = "내 회사",
                    start = p.getString("work_start_date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    periods = LEAVE_TYPES.associateWith { t ->
                        LeavePeriod.entries.firstOrNull { it.key == p.getString("period_$t", null) } ?: defaultPeriod(t)
                    },
                    quotas = LEAVE_TYPES.associateWith { p.getInt("quota_$it", 0) },
                    yearQuotas = LEAVE_TYPES.associateWith { t -> parseYears(p.getString("years_$t", null)) },
                    serviceQuotas = LEAVE_TYPES.associateWith { t -> parseYears(p.getString("service_years_$t", null)) }
                )
            ).also { saveWorkplaces(it) }
        }.sortedBy { it.start ?: LocalDate.MIN }
        editingId = currentWorkplace.id
        autoJihyu = p.getBoolean("auto_jihyu", true)
        shiftWeights = WEIGHT_KINDS.associateWith { p.getInt("weight_$it", DEFAULT_WEIGHTS[it] ?: 1) }
        includeSunday = p.getBoolean("red_sunday", true)
        includeSaturday = p.getBoolean("red_saturday", false)
        shiftColors = COLOR_KINDS.filter { p.contains("shift_color_$it") }
            .associateWith { p.getInt("shift_color_$it", 0) }
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

    fun updateIncludeSaturday(on: Boolean) {
        includeSaturday = on
        edit { putBoolean("red_saturday", on) }
        recount(lastTasks)
    }

    /** Paint [kind] blocks with [argb]; null goes back to the default color. */
    fun updateShiftColor(kind: String, argb: Int?) {
        shiftColors = if (argb == null) shiftColors - kind else shiftColors + (kind to argb)
        edit { if (argb == null) remove("shift_color_$kind") else putInt("shift_color_$kind", argb) }
    }

    /** 지휴 days a red-day [kind] shift earns. */
    fun weightOf(kind: String): Int = shiftWeights[kind] ?: DEFAULT_WEIGHTS[kind] ?: 1

    fun updateWeight(kind: String, days: Int) {
        shiftWeights = shiftWeights + (kind to days.coerceAtLeast(0))
        edit { putInt("weight_$kind", days.coerceAtLeast(0)) }
        recount(lastTasks)
    }

    /** Red on the Korean calendar, after the user's own corrections. */
    fun isRedDay(day: LocalDate): Boolean =
        redOverrides[day] ?: KoreanHolidays.isRedDay(day, includeSunday, includeSaturday)

    /** Mark [day] red / not red by hand; null goes back to the calendar. */
    fun setRedOverride(day: LocalDate, red: Boolean?) {
        redOverrides = if (red == null || red == KoreanHolidays.isRedDay(day, includeSunday, includeSaturday)) redOverrides - day
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
        includeSaturday && day.dayOfWeek == java.time.DayOfWeek.SATURDAY -> "토요일"
        else -> null
    }

    /** 지휴 earned in the period containing [day]. */
    fun earnedIn(day: LocalDate): Int = periodKey(AUTO_TYPE, day)?.let { earned[it] } ?: 0

    /** The red days worked that fall in the period containing [day]. */
    fun earnedDaysIn(day: LocalDate): List<Pair<LocalDate, String>> {
        val key = periodKey(AUTO_TYPE, day) ?: return emptyList()
        return earnedDays.filter { periodKey(AUTO_TYPE, it.first) == key }
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

    // ── 근무지 저장 ───────────────────────────────────────────────────────────

    private fun saveWorkplaces(list: List<Workplace>) {
        edit { putString("workplaces", encodeWorkplaces(list)) }
    }

    private fun encodeYears(m: Map<String, Map<Int, Int>>) = org.json.JSONObject().apply {
        m.forEach { (type, years) ->
            put(type, org.json.JSONObject().apply { years.forEach { (y, d) -> put(y.toString(), d) } })
        }
    }

    private fun decodeYears(o: org.json.JSONObject?): Map<String, Map<Int, Int>> {
        if (o == null) return emptyMap()
        return o.keys().asSequence().associateWith { type ->
            val inner = o.optJSONObject(type) ?: return@associateWith emptyMap<Int, Int>()
            inner.keys().asSequence().mapNotNull { y -> y.toIntOrNull()?.let { it to inner.optInt(y) } }.toMap()
        }
    }

    private fun encodeWorkplaces(list: List<Workplace>): String = org.json.JSONArray().apply {
        list.forEach { w ->
            put(org.json.JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                w.start?.let { put("start", it.toString()) }
                w.end?.let { put("end", it.toString()) }
                put("periods", org.json.JSONObject().apply { w.periods.forEach { (t, p) -> put(t, p.key) } })
                put("quotas", org.json.JSONObject().apply { w.quotas.forEach { (t, d) -> put(t, d) } })
                put("years", encodeYears(w.yearQuotas))
                put("serviceYears", encodeYears(w.serviceQuotas))
            })
        }
    }.toString()

    private fun decodeWorkplaces(raw: String): List<Workplace> = runCatching {
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            fun date(key: String) = o.optString(key, "").takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val periodsObj = o.optJSONObject("periods")
            val quotasObj = o.optJSONObject("quotas")
            Workplace(
                id = o.optString("id").ifBlank { return@mapNotNull null },
                name = o.optString("name").ifBlank { "회사" },
                start = date("start"),
                end = date("end"),
                periods = periodsObj?.keys()?.asSequence()?.mapNotNull { t ->
                    LeavePeriod.entries.firstOrNull { it.key == periodsObj.optString(t) }?.let { t to it }
                }?.toMap().orEmpty(),
                quotas = quotasObj?.keys()?.asSequence()?.associateWith { quotasObj.optInt(it) }.orEmpty(),
                yearQuotas = decodeYears(o.optJSONObject("years")),
                serviceQuotas = decodeYears(o.optJSONObject("serviceYears"))
            )
        }
    }.getOrDefault(emptyList())

    // ── 근무지 관리 ───────────────────────────────────────────────────────────

    private fun setWorkplaces(list: List<Workplace>) {
        workplaces = list.sortedBy { it.start ?: LocalDate.MIN }
        saveWorkplaces(workplaces)
        recount(lastTasks)
    }

    private fun updateEditing(transform: (Workplace) -> Workplace) {
        val id = editing.id
        setWorkplaces(workplaces.map { if (it.id == id) transform(it) else it })
    }

    /** Which workplace the 휴가 규칙 screens edit. */
    fun selectEditing(id: String) {
        if (workplaces.any { it.id == id }) editingId = id
    }

    /** Adds a workplace with default rules and returns it. */
    fun addWorkplace(name: String, start: LocalDate?, end: LocalDate?): Workplace {
        val wp = Workplace(
            id = "wp" + System.currentTimeMillis(),
            name = name.trim().ifBlank { "새 회사" },
            start = start,
            end = end,
            periods = LEAVE_TYPES.associateWith { defaultPeriod(it) }
        )
        setWorkplaces(workplaces + wp)
        return wp
    }

    fun renameWorkplace(id: String, name: String) {
        if (name.isBlank()) return
        setWorkplaces(workplaces.map { if (it.id == id) it.copy(name = name.trim()) else it })
    }

    /** Sets a workplace's 근무 시작일 / 퇴사일 (null end = 재직 중). */
    fun updateWorkplaceDates(id: String, start: LocalDate?, end: LocalDate?) {
        setWorkplaces(workplaces.map { if (it.id == id) it.copy(start = start, end = end) else it })
    }

    /** Removes a workplace; the last one can't go. Its days move to whichever workplace covers them. */
    fun deleteWorkplace(id: String) {
        if (workplaces.size <= 1) return
        setWorkplaces(workplaces.filterNot { it.id == id })
        if (editingId == id) editingId = currentWorkplace.id
    }

    /** "매년 (1월 1일) · 15일" — one line about [wp]'s rule for [type]. */
    fun ruleSummary(wp: Workplace, type: String): String {
        val base = wp.quotas[type] ?: 0
        val days = when {
            isAuto(type) -> "빨간날 근무 자동" + (if (base > 0) " + ${base}일" else "")
            base > 0 -> "${base}일"
            else -> "일수 설정 안 함"
        }
        return "${wp.periodOf(type).label} · $days"
    }

    // ── 규칙 (editing workplace) ──────────────────────────────────────────────

    private fun Workplace.periodOf(type: String): LeavePeriod = periods[type] ?: defaultPeriod(type)

    fun periodOf(type: String): LeavePeriod = editing.periodOf(type)

    fun updatePeriod(type: String, period: LeavePeriod) {
        updateEditing { it.copy(periods = it.periods + (type to period)) }
    }

    /** Default days (0 = 설정 안 함). */
    fun baseQuota(type: String): Int = editing.quotas[type] ?: 0

    fun updateQuota(type: String, days: Int) {
        updateEditing { it.copy(quotas = it.quotas + (type to days.coerceAtLeast(0))) }
    }

    /** Days for [year] if the user set one for that year, else null. */
    fun yearQuota(type: String, year: Int): Int? = editing.yearQuotas[type]?.get(year)

    /** Set (or with null, clear) the 연도별 일수 for [year]. */
    fun updateYearQuota(type: String, year: Int, days: Int?) {
        updateEditing { w ->
            val cur = w.yearQuotas[type].orEmpty()
            val next = if (days == null) cur - year else cur + (year to days.coerceAtLeast(0))
            w.copy(yearQuotas = w.yearQuotas + (type to next))
        }
    }

    fun updateWorkStartDate(day: LocalDate) {
        updateEditing { it.copy(start = day) }
    }

    private fun Workplace.serviceYearOn(day: LocalDate): Int = start?.let { serviceYearOn(it, day) } ?: 0

    /** 근속연차 on [day] at the workplace [day] belongs to (0 before it started). */
    fun serviceYear(day: LocalDate = LocalDate.now()): Int = workplaceOn(day)?.serviceYearOn(day) ?: 0

    fun serviceQuota(type: String, year: Int): Int? = editing.serviceQuotas[type]?.get(year)

    fun updateServiceQuota(type: String, year: Int, days: Int?) {
        require(year >= 1)
        updateEditing { w ->
            val current = w.serviceQuotas[type].orEmpty()
            val next = if (days == null) current - year else current + (year to days.coerceAtLeast(0))
            w.copy(serviceQuotas = w.serviceQuotas + (type to next))
        }
    }

    fun serviceRangeLabel(year: Int): String? = editing.start?.let {
        val range = serviceYearRange(it, year)
        "${range.start} ~ ${range.endInclusive}"
    }

    // ── 날짜별 계산 (the workplace each day belongs to) ─────────────────────────

    private fun Workplace.ownQuota(type: String, day: LocalDate): Int? = when (periodOf(type)) {
        LeavePeriod.NONE -> quotas[type] ?: 0
        LeavePeriod.SERVICE_YEAR -> {
            val year = serviceYearOn(day)
            if (year == 0) null else serviceQuotas[type]?.get(year) ?: quotas[type] ?: 0
        }
        else -> yearQuotas[type]?.get(day.year) ?: quotas[type] ?: 0
    }

    /** Allowance that applies on [day], using that day's workplace and reset boundary. */
    fun quota(type: String, day: LocalDate = LocalDate.now()): Int {
        val wp = workplaceOn(day) ?: return 0
        val own = wp.ownQuota(type, day) ?: return 0
        return if (isAuto(type)) earnedIn(day) + own else own
    }

    private fun periodKey(type: String, day: LocalDate): String? {
        val wp = workplaceOn(day) ?: return null
        val period = when (wp.periodOf(type)) {
            LeavePeriod.MONTH -> "%04d-%02d".format(day.year, day.monthValue)
            LeavePeriod.YEAR -> day.year.toString()
            LeavePeriod.SERVICE_YEAR -> "service:${wp.start}:${wp.serviceYearOn(day)}"
            LeavePeriod.NONE -> "all"
        }
        return "${wp.id}/$period"
    }

    /** "2026년 9월" / "2026년" / "2년차 (…)" / "전체" — the period [day] falls in. */
    private fun periodLabel(type: String, day: LocalDate): String {
        val wp = workplaceOn(day) ?: return "근무지 없음"
        return when (wp.periodOf(type)) {
            LeavePeriod.MONTH -> "${day.year}년 ${day.monthValue}월"
            LeavePeriod.YEAR -> "${day.year}년"
            LeavePeriod.SERVICE_YEAR -> {
                val year = wp.serviceYearOn(day)
                val range = wp.start?.let { serviceYearRange(it, year) }
                if (range == null || year == 0) "근무 시작 전" else "${year}년차 (${range.start} ~ ${range.endInclusive})"
            }
            LeavePeriod.NONE -> "전체"
        }
    }

    /**
     * 기록: every period of every leave kind with leave taken at [workplaceId],
     * newest first — what was given, how much was used and on which days.
     */
    fun history(workplaceId: String): List<LeaveHistoryPeriod> =
        lastTasks.flatMap { it.leaveEntries() }
            .distinct()
            .filter { (day, _) -> workplaceOn(day)?.id == workplaceId }
            .groupBy { (day, type) -> type to periodKey(type, day) }
            .map { (key, entries) ->
                val type = key.first
                val days = entries.map { it.first }.sorted()
                val first = days.first()
                LeaveHistoryPeriod(
                    type = type,
                    label = periodLabel(type, first),
                    allowance = remaining(type, first)?.let { quota(type, first) },
                    used = days.size,
                    days = days,
                    firstDay = first
                )
            }
            .sortedWith(compareByDescending<LeaveHistoryPeriod> { it.firstDay }.thenBy { LEAVE_TYPES.indexOf(it.type) })

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
        val to = LocalDate.of(today.year + 1, 12, 31)
        // Back three years, or to the earliest 근무 시작일 if that is older.
        val threeYears = today.minusYears(3).withDayOfYear(1)
        val earliestStart = workplaces.mapNotNull { it.start }.minOrNull()
        val from = maxOf(first, if (earliestStart != null && earliestStart.isBefore(threeYears)) earliestStart else threeYears)
        val days = redDaysWorked(tasks, from, to, ::isRedDay).filter { (d, _) ->
            val wp = workplaceOn(d) ?: return@filter false
            when (wp.periodOf(AUTO_TYPE)) {
                LeavePeriod.NONE -> !d.isAfter(today)
                LeavePeriod.SERVICE_YEAR -> wp.serviceYearOn(d) > 0
                else -> true
            }
        }
        earnedDays = days
        earned = days.mapNotNull { e -> periodKey(AUTO_TYPE, e.first)?.let { it to e } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.sumOf { weightOf(it.second) } }
    }

    fun countUsage(tasks: List<Task>): Map<String, Int> =
        tasks.flatMap { it.leaveEntries() }
            .filter { (day, type) ->
                val wp = workplaceOn(day)
                wp != null && (wp.periodOf(type) != LeavePeriod.SERVICE_YEAR || wp.serviceYearOn(day) > 0)
            }
            .distinct()
            .groupingBy { (day, type) -> "$type|${periodKey(type, day)}" }
            .eachCount()

    fun used(type: String, day: LocalDate): Int =
        periodKey(type, day)?.let { usage["$type|$it"] } ?: 0

    /** Days left in the period containing [day]; null when no allowance is set. */
    fun remaining(type: String, day: LocalDate, extraUsed: Int = 0): Int? {
        val wp = workplaceOn(day) ?: return null
        val q = quota(type, day)
        val serviceBased = wp.periodOf(type) == LeavePeriod.SERVICE_YEAR
        if (serviceBased && wp.serviceYearOn(day) == 0) return null
        if (q <= 0 && !isAuto(type) &&
            !(serviceBased && wp.serviceQuotas[type]?.get(wp.serviceYearOn(day)) != null)) return null
        return q - used(type, day) - extraUsed
    }

    /** "3일 남음" / "1일 초과" for the chip; null when no allowance is set. */
    fun remainingLabel(type: String, day: LocalDate, extraUsed: Int = 0): String? {
        val r = remaining(type, day, extraUsed) ?: return null
        return if (r >= 0) "${r}일 남음" else "${-r}일 초과"
    }

    /** "이번 달" / "올해" / "전체" — the period [day] falls in, for labels. */
    fun periodName(type: String, day: LocalDate = referenceDay()): String {
        val wp = workplaceOn(day) ?: return "근무지 없음"
        val current = day == LocalDate.now()
        return when (wp.periodOf(type)) {
            LeavePeriod.MONTH -> if (current) "이번 달" else "${day.year}년 ${day.monthValue}월"
            LeavePeriod.YEAR -> if (current) "올해" else "${day.year}년"
            LeavePeriod.SERVICE_YEAR -> when {
                wp.start == null -> "근무 시작일 미설정"
                wp.serviceYearOn(day) == 0 -> "근무 시작 전"
                else -> "${wp.serviceYearOn(day)}년차"
            }
            LeavePeriod.NONE -> "전체"
        }
    }
}
