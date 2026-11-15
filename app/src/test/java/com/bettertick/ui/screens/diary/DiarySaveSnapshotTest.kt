package com.bettertick.ui.screens.diary

import com.bettertick.data.model.DiaryEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DiarySaveSnapshotTest {
    private val day = LocalDate.of(2026, 11, 15)

    @Test fun editingSameDayRetainsDocumentIdentity() {
        val existing = DiaryEntry(id = "saved-id", dateStr = day.toString(), content = "before")
        val updated = diarySaveSnapshot(day, "after", 3, existing)
        assertEquals("saved-id", updated.id)
        assertEquals(existing.createdAt, updated.createdAt)
        assertEquals("after", updated.content)
        assertEquals(3, updated.mood)
    }

    @Test fun stalePreviousDayMustNotBeOverwritten() {
        val stale = DiaryEntry(id = "other-day", dateStr = day.minusDays(1).toString())
        val result = diarySaveSnapshot(day, "today", 4, stale)
        assertEquals("", result.id)
        assertEquals(day.toString(), result.dateStr)
        assertEquals("today", result.content)
    }

    @Test fun changingSelectedDateDoesNotRetargetCapturedWrite() {
        var selected = day
        val captured = diarySaveSnapshot(selected, "original day", 2, null)
        selected = day.plusDays(1)
        assertEquals(day.toString(), captured.dateStr)
        assertEquals(day.plusDays(1), selected)
    }

    @Test fun moodStaysInSupportedRange() {
        assertEquals(0, diarySaveSnapshot(day, "a", -1, null).mood)
        assertEquals(4, diarySaveSnapshot(day, "a", 9, null).mood)
    }
}
