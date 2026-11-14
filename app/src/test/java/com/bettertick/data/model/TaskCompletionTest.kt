package com.bettertick.data.model

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCompletionTest {
    private val nov14 = LocalDate.parse("2026-11-14")
    private val nov15 = LocalDate.parse("2026-11-15")
    private val repeat = Task(id = "r", title = "운동", repeatRule = "DAILY", completedDates = listOf("2026-11-14"))

    @Test fun repeatingTaskIsCheckedPerDay() {
        assertTrue(repeat.checksPerDay())
        assertTrue(repeat.completedOn(nov14))
        assertFalse(repeat.completedOn(nov15))
        assertTrue(repeat.forDay(nov14).isCompleted)
        assertFalse(repeat.forDay(nov15).isCompleted)
    }

    @Test fun wholeSeriesOptionUsesTheTaskFlag() {
        val whole = repeat.copy(checkWholeSeries = true, isCompleted = true)
        assertFalse(whole.checksPerDay())
        assertTrue(whole.completedOn(nov15))
        assertTrue(whole.forDay(nov15).isCompleted)
    }

    @Test fun oneOffTaskIsUnchanged() {
        val once = Task(id = "o", title = "x", isCompleted = true)
        assertFalse(once.checksPerDay())
        assertTrue(once.forDay(nov15).isCompleted)
    }
}
