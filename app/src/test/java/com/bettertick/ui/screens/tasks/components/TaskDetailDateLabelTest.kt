package com.bettertick.ui.screens.tasks.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDetailDateLabelTest {
    private val clock: (LocalTime) -> String = { it.toString() }

    @Test fun selectedRepetitionUsesSelectedDayAndSeriesTime() {
        val anchor = LocalDateTime.of(2026, 10, 5, 19, 0)
        assertEquals("11월 14일 (토) · 19:00–20:00", formatTaskDateLabel(
            anchor, 60, LocalDate.of(2026, 11, 14), clock))
        assertEquals(LocalDateTime.of(2026, 10, 5, 19, 0), anchor)
    }

    @Test fun normalTaskKeepsItsScheduledDate() {
        assertEquals("10월 5일 (월) · 19:00", formatTaskDateLabel(
            LocalDateTime.of(2026, 10, 5, 19, 0), 0, formatTime = clock))
    }

    @Test fun overnightEndMakesNextDayExplicit() {
        assertEquals("11월 14일 (토) · 21:00–03:00 (+1일)", formatTaskDateLabel(
            LocalDateTime.of(2026, 11, 14, 21, 0), 360, formatTime = clock))
    }

    @Test fun allDayAndUnscheduledLabelsOmitTimes() {
        assertEquals("날짜 설정", formatTaskDateLabel(null, 60, formatTime = clock))
        assertEquals("11월 14일 (토)", formatTaskDateLabel(
            LocalDateTime.of(2026, 11, 14, 0, 0), 0, formatTime = clock))
    }
}
