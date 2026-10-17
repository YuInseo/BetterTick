package com.bettertick.data.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 한국 달력의 빨간날 (공휴일 + 대체공휴일). Fixed-date holidays and the rules
 * for 대체공휴일 are computed; lunar holidays (설날 / 부처님오신날 / 추석),
 * election days and 임시공휴일 come from tables below — extend them as new
 * years are announced. Sundays are red as well but are not listed here; see
 * [isRedDay].
 */
object KoreanHolidays {

    /** Lunar holidays by year: 설날, 부처님오신날, 추석 (the day itself). */
    private val LUNAR: Map<Int, Triple<String, String, String>> = mapOf(
        2023 to Triple("2023-01-22", "2023-05-27", "2023-09-29"),
        2024 to Triple("2024-02-10", "2024-05-15", "2024-09-17"),
        2025 to Triple("2025-01-29", "2025-05-05", "2025-10-06"),
        2026 to Triple("2026-02-17", "2026-05-24", "2026-09-25"),
        2027 to Triple("2027-02-07", "2027-05-13", "2027-09-15"),
        2028 to Triple("2028-01-27", "2028-05-02", "2028-10-03"),
        2029 to Triple("2029-02-13", "2029-05-20", "2029-09-22"),
        2030 to Triple("2030-02-03", "2030-05-09", "2030-09-12"),
        2031 to Triple("2031-01-23", "2031-05-28", "2031-10-01"),
        2032 to Triple("2032-02-11", "2032-05-16", "2032-09-19"),
    )

    /** 선거일 and 임시공휴일 — announced one at a time. */
    private val EXTRA: Map<String, String> = mapOf(
        "2023-10-02" to "임시공휴일",
        "2024-04-10" to "국회의원 선거일",
        "2024-10-01" to "국군의 날",
        "2025-01-27" to "임시공휴일",
        "2025-06-03" to "대통령 선거일",
        "2026-06-03" to "지방선거일",
        "2028-04-12" to "국회의원 선거일",
        "2030-06-05" to "지방선거일",
        "2032-04-14" to "국회의원 선거일",
    )

    private val cache = HashMap<Int, Map<LocalDate, String>>()

    /** Holiday name on [date] ("추석", "대체공휴일" …), or null. Sundays alone are not named. */
    fun name(date: LocalDate): String? = holidays(date.year)[date]

    /** A red day: a Sunday (when [includeSunday]) or any public holiday. */
    fun isRedDay(date: LocalDate, includeSunday: Boolean = true): Boolean =
        (includeSunday && date.dayOfWeek == DayOfWeek.SUNDAY) || name(date) != null

    /** Every holiday in [year], 대체공휴일 included. */
    @Synchronized
    fun holidays(year: Int): Map<LocalDate, String> = cache.getOrPut(year) { build(year) }

    private fun build(year: Int): Map<LocalDate, String> {
        val out = LinkedHashMap<LocalDate, String>()
        fun d(m: Int, day: Int) = LocalDate.of(year, m, day)

        // Fixed holidays; the flag says whether a 대체공휴일 applies.
        val fixed = buildList {
            add(Triple(d(1, 1), "신정", false))
            add(Triple(d(3, 1), "삼일절", true))
            if (year >= 2026) add(Triple(d(5, 1), "노동절", true))
            add(Triple(d(5, 5), "어린이날", true))
            add(Triple(d(6, 6), "현충일", false))
            if (year >= 2026) add(Triple(d(7, 17), "제헌절", true))
            add(Triple(d(8, 15), "광복절", true))
            add(Triple(d(10, 3), "개천절", true))
            add(Triple(d(10, 9), "한글날", true))
            add(Triple(d(12, 25), "성탄절", year >= 2023))
        }
        val lunar = LUNAR[year]?.let { (s, b, c) ->
            Triple(LocalDate.parse(s), LocalDate.parse(b), LocalDate.parse(c))
        }
        val seollal = lunar?.first?.let { listOf(it.minusDays(1), it, it.plusDays(1)) }.orEmpty()
        val chuseok = lunar?.third?.let { listOf(it.minusDays(1), it, it.plusDays(1)) }.orEmpty()
        val buddha = lunar?.second

        // Names; on a shared day the first one wins.
        fixed.forEach { (date, n, _) -> out.putIfAbsent(date, n) }
        seollal.forEach { out.putIfAbsent(it, "설날") }
        chuseok.forEach { out.putIfAbsent(it, "추석") }
        buddha?.let { out.putIfAbsent(it, "부처님오신날") }
        EXTRA.forEach { (k, v) -> LocalDate.parse(k).takeIf { it.year == year }?.let { out.putIfAbsent(it, v) } }

        // How many holidays land on each day — >1 means they overlap.
        val load = HashMap<LocalDate, Int>()
        (fixed.map { it.first } + seollal + chuseok + listOfNotNull(buddha)).forEach {
            load[it] = (load[it] ?: 0) + 1
        }
        val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        // Each trigger asks for one 대체공휴일 on the first free weekday after `from`.
        val triggers = mutableListOf<LocalDate>()
        fun block(days: List<LocalDate>) {
            if (days.isEmpty()) return
            if (days.any { it.dayOfWeek == DayOfWeek.SUNDAY || (load[it] ?: 0) > 1 }) triggers += days.last()
        }
        block(seollal)
        block(chuseok)
        val inBlock = (seollal + chuseok).toSet()
        val seen = HashSet<LocalDate>()
        (fixed.filter { it.third }.map { it.first } + listOfNotNull(buddha)).sorted().forEach { day ->
            // A holiday inside 설/추석 is already covered by that block's own substitute.
            if (day in inBlock || !seen.add(day)) return@forEach
            if (day.dayOfWeek in weekend || (load[day] ?: 0) > 1) triggers += day
        }
        triggers.sorted().forEach { from ->
            var s = from.plusDays(1)
            while (s.dayOfWeek in weekend || out.containsKey(s)) s = s.plusDays(1)
            out[s] = "대체공휴일"
        }
        return out
    }
}
