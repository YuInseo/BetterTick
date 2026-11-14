package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** How often a salary item is paid. */
enum class SalaryRepeat(val key: String, val label: String) {
    MONTHLY("monthly", "매달"),
    QUARTERLY("quarterly", "분기마다"),
    YEARLY("yearly", "매년 정한 달"),
    ONCE("once", "한 번만")
}

/**
 * 월급 항목: 기본급, 수당, 상여, 공제(세금·보험) …
 *
 * - 매달: every month between [from] and [to] (기간별: either end may be open).
 * - 분기마다: the [quarterMonth]-th month of each quarter (3 = 3·6·9·12월).
 * - 매년 정한 달: the [months] of every year (예: 명절 상여 2·9월).
 * - 한 번만: only in [from] (달별 one-off, 예: 2026년 12월 성과급).
 */
data class SalaryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Always positive; [deduction] says whether it is paid or taken off. */
    val amount: Long,
    val deduction: Boolean = false,
    val repeat: SalaryRepeat = SalaryRepeat.MONTHLY,
    val quarterMonth: Int = 3,
    val months: Set<Int> = emptySet(),
    val from: YearMonth? = null,
    val to: YearMonth? = null,
    /** Shown instead of the schedule (자동 항목, 예: 군 월급). */
    val note: String? = null
) {
    fun paysIn(month: YearMonth): Boolean {
        if (repeat == SalaryRepeat.ONCE) return from == month
        if (from != null && month < from) return false
        if (to != null && month > to) return false
        return when (repeat) {
            SalaryRepeat.MONTHLY -> true
            SalaryRepeat.QUARTERLY -> (month.monthValue - 1) % 3 == (quarterMonth.coerceIn(1, 3) - 1)
            SalaryRepeat.YEARLY -> month.monthValue in months
            SalaryRepeat.ONCE -> false
        }
    }

    val signed: Long get() = if (deduction) -amount else amount

    /** "매달 · 2026년 1월부터", "분기 마지막 달 (3·6·9·12월)", "2026년 12월 한 번" */
    val scheduleLabel: String
        get() {
            note?.let { return it }
            fun ym(m: YearMonth) = "${m.year}년 ${m.monthValue}월"
            val base = when (repeat) {
                SalaryRepeat.MONTHLY -> "매달"
                SalaryRepeat.QUARTERLY -> {
                    val ms = (0..3).joinToString("·") { (it * 3 + quarterMonth.coerceIn(1, 3)).toString() }
                    "분기마다 (${ms}월)"
                }
                SalaryRepeat.YEARLY ->
                    if (months.isEmpty()) "매년 (달 미설정)" else "매년 ${months.sorted().joinToString("·")}월"
                SalaryRepeat.ONCE -> return from?.let { "${ym(it)} 한 번" } ?: "한 번 (달 미설정)"
            }
            val range = when {
                from != null && to != null -> " · ${ym(from)} ~ ${ym(to)}"
                from != null -> " · ${ym(from)}부터"
                to != null -> " · ${ym(to)}까지"
                else -> ""
            }
            return base + range
        }
}

/** One workplace's salary rules. */
/** 계급 하나: 이름, 그 계급으로 지내는 개월 수, 한 달 월급. */
data class MilitaryRank(val name: String, val months: Int, val pay: Long)

/** 이병 2 · 일병 6 · 상병 6 · 병장 4개월 (육군 18개월), 2025년 병 봉급. 모두 수정 가능. */
val DefaultMilitaryRanks = listOf(
    MilitaryRank("이병", 2, 750_000),
    MilitaryRank("일병", 6, 900_000),
    MilitaryRank("상병", 6, 1_200_000),
    MilitaryRank("병장", 4, 1_500_000)
)

/**
 * 군 복무 월급 자동 계산. The 입대한 달 counts as the first month of 이병;
 * each next rank starts on the 1st of the month after the previous one's
 * months are used up (예: 1월 15일 입대 → 3월 1일 일병). 전역일 = 입대일 +
 * 총 개월 − 1일. With [prorate], the 입대·전역 달 are paid by days served.
 */
data class MilitaryPay(
    val enabled: Boolean = false,
    val enlist: LocalDate? = null,
    val ranks: List<MilitaryRank> = DefaultMilitaryRanks,
    val prorate: Boolean = true
) {
    val totalMonths: Int get() = ranks.sumOf { it.months.coerceAtLeast(0) }
    val discharge: LocalDate? get() = enlist?.plusMonths(totalMonths.toLong())?.minusDays(1)

    /** 계급별 시작일 (첫 계급은 입대일, 나머지는 그 달 1일). */
    fun rankStarts(): List<Pair<MilitaryRank, LocalDate>> {
        val start = enlist ?: return emptyList()
        var month = YearMonth.from(start)
        return ranks.mapIndexed { i, r ->
            val day = if (i == 0) start else month.atDay(1)
            month = month.plusMonths(r.months.coerceAtLeast(0).toLong())
            r to day
        }
    }

    /** Rank held at the start of [month] (or on 입대일 in the 입대한 달). */
    fun rankIn(month: YearMonth): MilitaryRank? {
        val end = discharge ?: return null
        if (month < YearMonth.from(enlist!!) || month > YearMonth.from(end)) return null
        return rankStarts().lastOrNull { YearMonth.from(it.second) <= month }?.first
    }

    /** That month's pay (일할 for the 입대·전역 달 when [prorate]); null outside service. */
    fun payIn(month: YearMonth): Long? {
        if (!enabled) return null
        val rank = rankIn(month) ?: return null
        if (!prorate) return rank.pay
        val first = maxOf(month.atDay(1), enlist!!)
        val last = minOf(month.atEndOfMonth(), discharge!!)
        val served = java.time.temporal.ChronoUnit.DAYS.between(first, last) + 1
        return rank.pay * served / month.lengthOfMonth()
    }
}

data class SalaryRules(
    /** 월급날 (1~31; a short month pays on its last day). */
    val payday: Int = 25,
    val items: List<SalaryItem> = emptyList(),
    /** 달별 총액 직접 입력: replaces the items for that month. */
    val monthOverrides: Map<YearMonth, Long> = emptyMap(),
    val military: MilitaryPay = MilitaryPay()
) {
    val isEmpty: Boolean get() = items.isEmpty() && monthOverrides.isEmpty() && !military.enabled

    /** Items paid in [month], with the 군 월급 for that month first. */
    fun itemsIn(month: YearMonth): List<SalaryItem> {
        val own = items.filter { it.paysIn(month) }
        val pay = military.payIn(month) ?: return own
        val rank = military.rankIn(month)?.name.orEmpty()
        val partial = pay != military.rankIn(month)?.pay
        return listOf(
            SalaryItem(
                id = "military", name = "군 월급 · $rank", amount = pay,
                note = "입대일로 자동 계산" + if (partial) " (일할)" else ""
            )
        ) + own
    }
    fun autoTotal(month: YearMonth): Long = itemsIn(month).sumOf { it.signed }
    fun override(month: YearMonth): Long? = monthOverrides[month]
    /** 실수령: the typed-in amount for the month, else paid − deducted. */
    fun total(month: YearMonth): Long = override(month) ?: autoTotal(month)
    fun gross(month: YearMonth): Long = itemsIn(month).filter { !it.deduction }.sumOf { it.amount }
    fun deducted(month: YearMonth): Long = itemsIn(month).filter { it.deduction }.sumOf { it.amount }
    fun payDate(month: YearMonth): LocalDate = month.atDay(payday.coerceIn(1, month.lengthOfMonth()))
}

fun quarterOf(month: YearMonth): Int = (month.monthValue - 1) / 3 + 1
fun quarterMonths(year: Int, quarter: Int): List<YearMonth> =
    (0..2).map { YearMonth.of(year, (quarter - 1) * 3 + 1 + it) }

/** "2,450,000원" (minus sign for negatives). */
fun won(amount: Long): String = "%,d원".format(amount)

/**
 * 월급 (설정 > 일반 > 월급). Rules are kept per 근무지 ([LeavePrefs.workplaces]);
 * a month belongs to the workplace that covers its 월급날 (falling back to the
 * month's first day), so moving jobs keeps each company's pay as history.
 */
object SalaryPrefs {
    private const val PREFS = "salary_prefs"

    /** workplace id → rules. */
    var rules by mutableStateOf<Map<String, SalaryRules>>(emptyMap())
        private set
    /** 캘린더 화면 왼쪽 아래 '월급' 버튼 (기본 켬). */
    var showButton by mutableStateOf(true)
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        rules = p.getString("rules", null)?.let { decode(it) }.orEmpty()
        showButton = p.getBoolean("show_button", true)
    }

    fun rulesFor(workplaceId: String): SalaryRules = rules[workplaceId] ?: SalaryRules()

    /** The workplace [month]'s pay belongs to; null between jobs. */
    fun workplaceFor(month: YearMonth): Workplace? {
        val byPayday = LeavePrefs.workplaces.firstNotNullOfOrNull { wp ->
            val day = rulesFor(wp.id).payDate(month)
            wp.takeIf { LeavePrefs.workplaceOn(day)?.id == wp.id }
        }
        return byPayday ?: LeavePrefs.workplaceOn(month.atDay(1)) ?: LeavePrefs.workplaceOn(month.atEndOfMonth())
    }

    /** Rules that apply to [month] (empty when no workplace or nothing set). */
    fun rulesIn(month: YearMonth): SalaryRules = workplaceFor(month)?.let { rulesFor(it.id) } ?: SalaryRules()

    fun total(month: YearMonth): Long = rulesIn(month).total(month)
    fun quarterTotal(year: Int, quarter: Int): Long = quarterMonths(year, quarter).sumOf { total(it) }
    fun yearTotal(year: Int): Long = (1..12).sumOf { total(YearMonth.of(year, it)) }

    /** True only the first time: the 가계부 tab gets added to the user's tab bar once. */
    fun claimMoneyTabMigration(): Boolean {
        val p = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return false
        if (p.getBoolean("money_tab_added", false)) return false
        p.edit().putBoolean("money_tab_added", true).apply()
        return true
    }

    fun updateShowButton(on: Boolean) {
        showButton = on
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putBoolean("show_button", on)?.apply()
    }

    private fun update(workplaceId: String, transform: (SalaryRules) -> SalaryRules) {
        rules = rules + (workplaceId to transform(rulesFor(workplaceId)))
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString("rules", encode(rules))?.apply()
    }

    /**
     * 군 복무 설정 바꾸기. With an 입대일, the workplace's 근무 기간 follows
     * 입대일 ~ 전역일 so each month's pay lands on this workplace.
     */
    fun updateMilitary(workplaceId: String, transform: (MilitaryPay) -> MilitaryPay) {
        val before = rulesFor(workplaceId)
        val next = transform(before.military)
        update(workplaceId) { r ->
            // First switch-on with nothing else set: 군 월급날은 매달 10일.
            val payday = if (next.enabled && !before.military.enabled && r.items.isEmpty() && r.payday == 25) 10 else r.payday
            r.copy(military = next, payday = payday)
        }
        if (next.enabled && next.enlist != null) {
            LeavePrefs.updateWorkplaceDates(workplaceId, next.enlist, next.discharge)
        }
    }

    fun updatePayday(workplaceId: String, day: Int) = update(workplaceId) { it.copy(payday = day.coerceIn(1, 31)) }

    /** Adds [item] or replaces the one with the same id. */
    fun saveItem(workplaceId: String, item: SalaryItem) = update(workplaceId) { r ->
        val exists = r.items.any { it.id == item.id }
        r.copy(items = if (exists) r.items.map { if (it.id == item.id) item else it } else r.items + item)
    }

    fun deleteItem(workplaceId: String, id: String) = update(workplaceId) { r -> r.copy(items = r.items.filterNot { it.id == id }) }

    fun updateOverride(workplaceId: String, month: YearMonth, amount: Long?) = update(workplaceId) { r ->
        r.copy(monthOverrides = if (amount == null) r.monthOverrides - month else r.monthOverrides + (month to amount))
    }

    // ── JSON ───────────────────────────────────────────────────────────────

    internal fun encode(all: Map<String, SalaryRules>): String = org.json.JSONObject().apply {
        all.forEach { (wpId, r) ->
            put(wpId, org.json.JSONObject().apply {
                put("payday", r.payday)
                put("items", org.json.JSONArray().apply {
                    r.items.forEach { i ->
                        put(org.json.JSONObject().apply {
                            put("id", i.id)
                            put("name", i.name)
                            put("amount", i.amount)
                            put("deduction", i.deduction)
                            put("repeat", i.repeat.key)
                            put("quarterMonth", i.quarterMonth)
                            put("months", org.json.JSONArray().apply { i.months.sorted().forEach { put(it) } })
                            i.from?.let { put("from", it.toString()) }
                            i.to?.let { put("to", it.toString()) }
                        })
                    }
                })
                put("overrides", org.json.JSONObject().apply {
                    r.monthOverrides.forEach { (m, a) -> put(m.toString(), a) }
                })
                put("military", org.json.JSONObject().apply {
                    put("enabled", r.military.enabled)
                    r.military.enlist?.let { put("enlist", it.toString()) }
                    put("prorate", r.military.prorate)
                    put("ranks", org.json.JSONArray().apply {
                        r.military.ranks.forEach { k ->
                            put(org.json.JSONObject().apply { put("name", k.name); put("months", k.months); put("pay", k.pay) })
                        }
                    })
                })
            })
        }
    }.toString()

    internal fun decode(raw: String): Map<String, SalaryRules> = runCatching {
        val root = org.json.JSONObject(raw)
        fun ym(s: String?): YearMonth? = s?.takeIf { it.isNotBlank() }?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        root.keys().asSequence().associateWith { wpId ->
            val o = root.getJSONObject(wpId)
            val arr = o.optJSONArray("items") ?: org.json.JSONArray()
            val items = (0 until arr.length()).mapNotNull { idx ->
                val i = arr.optJSONObject(idx) ?: return@mapNotNull null
                val months = i.optJSONArray("months")
                SalaryItem(
                    id = i.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = i.optString("name"),
                    amount = i.optLong("amount"),
                    deduction = i.optBoolean("deduction"),
                    repeat = SalaryRepeat.entries.firstOrNull { it.key == i.optString("repeat") } ?: SalaryRepeat.MONTHLY,
                    quarterMonth = i.optInt("quarterMonth", 3),
                    months = months?.let { m -> (0 until m.length()).map { m.optInt(it) }.toSet() }.orEmpty(),
                    from = ym(i.optString("from", "")),
                    to = ym(i.optString("to", ""))
                )
            }
            val ov = o.optJSONObject("overrides")
            val mil = o.optJSONObject("military")
            val military = if (mil == null) MilitaryPay() else {
                val ra = mil.optJSONArray("ranks")
                MilitaryPay(
                    enabled = mil.optBoolean("enabled"),
                    enlist = mil.optString("enlist", "").takeIf { it.isNotBlank() }
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    prorate = mil.optBoolean("prorate", true),
                    ranks = ra?.let { a ->
                        (0 until a.length()).mapNotNull { idx ->
                            val k = a.optJSONObject(idx) ?: return@mapNotNull null
                            MilitaryRank(k.optString("name"), k.optInt("months"), k.optLong("pay"))
                        }
                    }?.takeIf { it.isNotEmpty() } ?: DefaultMilitaryRanks
                )
            }
            SalaryRules(
                military = military,
                payday = o.optInt("payday", 25),
                items = items,
                monthOverrides = ov?.keys()?.asSequence()?.mapNotNull { k -> ym(k)?.let { it to ov.optLong(k) } }?.toMap().orEmpty()
            )
        }
    }.getOrDefault(emptyMap())
}
