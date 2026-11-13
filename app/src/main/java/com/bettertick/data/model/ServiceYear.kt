package com.bettertick.data.model

import java.time.LocalDate

/** Anniversary-based years; plusYears consistently clamps February 29 to February 28. */
fun serviceYearOn(start: LocalDate, day: LocalDate): Int {
    if (day.isBefore(start)) return 0
    val years = day.year - start.year
    return years + if (day.isBefore(start.plusYears(years.toLong()))) 0 else 1
}

fun serviceYearRange(start: LocalDate, year: Int): ClosedRange<LocalDate> {
    require(year >= 1)
    return start.plusYears(year.toLong() - 1)..start.plusYears(year.toLong()).minusDays(1)
}
