package com.bettertick.ui.screens.tasks

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryRulesTest {
    private fun ym(s: String) = YearMonth.parse(s)

    private val rules = SalaryRules(
        payday = 25,
        items = listOf(
            SalaryItem(id = "base", name = "기본급", amount = 2_500_000),
            SalaryItem(id = "raise", name = "인상분", amount = 100_000, from = ym("2026-07")),
            SalaryItem(id = "meal", name = "식대", amount = 200_000, to = ym("2026-06")),
            SalaryItem(id = "q", name = "분기 상여", amount = 500_000, repeat = SalaryRepeat.QUARTERLY, quarterMonth = 3),
            SalaryItem(id = "y", name = "명절 상여", amount = 300_000, repeat = SalaryRepeat.YEARLY, months = setOf(2, 9)),
            SalaryItem(id = "once", name = "성과급", amount = 1_000_000, repeat = SalaryRepeat.ONCE, from = ym("2026-12")),
            SalaryItem(id = "tax", name = "4대보험", amount = 250_000, deduction = true)
        ),
        monthOverrides = mapOf(ym("2026-05") to 2_000_000L)
    )

    @Test fun eachRepeatKindPaysInItsMonths() {
        // Jan: 기본급 + 식대 − 보험
        assertEquals(2_500_000L + 200_000 - 250_000, rules.total(ym("2026-01")))
        // Mar: + 분기 상여
        assertEquals(2_500_000L + 200_000 + 500_000 - 250_000, rules.total(ym("2026-03")))
        // Sep: 인상분(7월부터) + 분기 상여 + 명절 상여, 식대 끝남(6월까지)
        assertEquals(2_500_000L + 100_000 + 500_000 + 300_000 - 250_000, rules.total(ym("2026-09")))
        // Dec: + 성과급 한 번
        assertEquals(2_500_000L + 100_000 + 500_000 + 1_000_000 - 250_000, rules.total(ym("2026-12")))
        assertFalse(rules.items.first { it.id == "once" }.paysIn(ym("2027-12")))
    }

    @Test fun typedInMonthReplacesTheItems() {
        assertEquals(2_000_000L, rules.total(ym("2026-05")))
        assertEquals(2_450_000L, rules.autoTotal(ym("2026-05")))
    }

    @Test fun quarterlyCanUseAnyMonthOfTheQuarter() {
        val first = SalaryItem(name = "x", amount = 1, repeat = SalaryRepeat.QUARTERLY, quarterMonth = 1)
        assertTrue(first.paysIn(ym("2026-04")))
        assertFalse(first.paysIn(ym("2026-06")))
        assertEquals(listOf(4, 5, 6), quarterMonths(2026, 2).map { it.monthValue })
        assertEquals(4, quarterOf(ym("2026-11")))
    }

    @Test fun paydayFallsOnTheLastDayOfShortMonths() {
        val r = SalaryRules(payday = 31)
        assertEquals(LocalDate.parse("2026-02-28"), r.payDate(ym("2026-02")))
        assertEquals(LocalDate.parse("2026-11-30"), r.payDate(ym("2026-11")))
    }

    @Test fun grossAndDeductionsAreSplit() {
        val m = ym("2026-01")
        assertEquals(2_700_000L, rules.gross(m))
        assertEquals(250_000L, rules.deducted(m))
    }

    private val army = MilitaryPay(enabled = true, enlist = LocalDate.parse("2025-01-15"))

    @Test fun ranksFollowTheEnlistMonth() {
        // 1월 15일 입대 → 3월 1일 일병, 9월 1일 상병, 2026년 3월 1일 병장, 7월 14일 전역.
        assertEquals(
            listOf("2025-01-15", "2025-03-01", "2025-09-01", "2026-03-01"),
            army.rankStarts().map { it.second.toString() }
        )
        assertEquals(LocalDate.parse("2026-07-14"), army.discharge)
        assertEquals("이병", army.rankIn(ym("2025-02"))?.name)
        assertEquals("일병", army.rankIn(ym("2025-03"))?.name)
        assertEquals("상병", army.rankIn(ym("2026-02"))?.name)
        assertEquals("병장", army.rankIn(ym("2026-07"))?.name)
        assertEquals(null, army.rankIn(ym("2026-08")))
    }

    @Test fun enlistAndDischargeMonthsArePaidByDays() {
        // 1월: 15~31일 = 17일 / 31일
        assertEquals(750_000L * 17 / 31, army.payIn(ym("2025-01")))
        assertEquals(900_000L, army.payIn(ym("2025-03")))
        // 7월: 1~14일 = 14일 / 31일
        assertEquals(1_500_000L * 14 / 31, army.payIn(ym("2026-07")))
        assertEquals(750_000L, army.copy(prorate = false).payIn(ym("2025-01")))
        assertEquals(null, army.copy(enabled = false).payIn(ym("2025-03")))
    }

    @Test fun editedRanksChangeTheSchedule() {
        // 공군 21개월: 병장 7개월.
        val airForce = army.copy(ranks = DefaultMilitaryRanks.map { if (it.name == "병장") it.copy(months = 7) else it })
        assertEquals(LocalDate.parse("2026-10-14"), airForce.discharge)
        val rules = SalaryRules(military = airForce, items = listOf(SalaryItem(name = "내일준비지원금", amount = 550_000)))
        assertEquals(1_500_000L + 550_000, rules.total(ym("2026-09")))
        assertEquals("군 월급 · 병장", rules.itemsIn(ym("2026-09")).first().name)
    }
}
