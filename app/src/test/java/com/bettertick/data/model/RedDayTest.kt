package com.bettertick.data.model

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedDayTest {
    private val saturday = LocalDate.parse("2026-11-14")
    private val sunday = LocalDate.parse("2026-11-15")
    private val monday = LocalDate.parse("2026-11-16")

    @Test fun saturdayCountsOnlyWhenTurnedOn() {
        assertFalse(KoreanHolidays.isRedDay(saturday))
        assertTrue(KoreanHolidays.isRedDay(saturday, includeSaturday = true))
        assertTrue(KoreanHolidays.isRedDay(sunday, includeSaturday = true))
        assertFalse(KoreanHolidays.isRedDay(sunday, includeSunday = false))
        assertFalse(KoreanHolidays.isRedDay(monday, includeSunday = true, includeSaturday = true))
        // 개천절 (Sat 2026-10-03) is red either way.
        assertTrue(KoreanHolidays.isRedDay(LocalDate.parse("2026-10-03")))
    }
}
