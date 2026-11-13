package com.bettertick.data.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ServiceYearTest {
    private fun day(value: String) = LocalDate.parse(value)

    @Test fun anniversaryStartsNewYearRatherThanJanuaryFirst() {
        val start = day("2025-09-10")
        assertEquals(0, serviceYearOn(start, day("2025-09-09")))
        assertEquals(1, serviceYearOn(start, start))
        assertEquals(1, serviceYearOn(start, day("2026-01-01")))
        assertEquals(1, serviceYearOn(start, day("2026-09-09")))
        assertEquals(2, serviceYearOn(start, day("2026-09-10")))
        assertEquals(day("2026-09-09"), serviceYearRange(start, 1).endInclusive)
        assertEquals(day("2026-09-10"), serviceYearRange(start, 2).start)
    }

    @Test fun leapDayAnniversariesHaveNoGapOrOverlap() {
        val start = day("2024-02-29")
        assertEquals(1, serviceYearOn(start, day("2025-02-27")))
        assertEquals(2, serviceYearOn(start, day("2025-02-28")))
        assertEquals(4, serviceYearOn(start, day("2028-02-28")))
        assertEquals(5, serviceYearOn(start, day("2028-02-29")))
        for (year in 1..8) {
            val range = serviceYearRange(start, year)
            assertEquals(year, serviceYearOn(start, range.start))
            assertEquals(year, serviceYearOn(start, range.endInclusive))
            assertEquals(range.endInclusive.plusDays(1), serviceYearRange(start, year + 1).start)
        }
    }
}
