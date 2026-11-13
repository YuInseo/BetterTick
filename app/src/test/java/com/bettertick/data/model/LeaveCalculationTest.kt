package com.bettertick.data.model

import com.bettertick.ui.screens.tasks.LeavePeriod
import com.bettertick.ui.screens.tasks.LeavePrefs
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class LeaveCalculationTest {
    private fun day(value: String) = LocalDate.parse(value)
    private fun task(title: String, date: String, leave: String? = null): Task {
        val timestamp = Timestamp(Date.from(day(date).atStartOfDay(ZoneId.systemDefault()).toInstant()))
        return Task(title = title, dueDate = timestamp, createdAt = timestamp, updatedAt = timestamp,
            leaves = if (leave == null) emptyMap() else mapOf(date to leave))
    }

    @Test fun serviceYearQuotasAndUsageFollowTheAnniversary() {
        LeavePrefs.updateAutoJihyu(false)
        LeavePrefs.recount(emptyList())
        LeavePrefs.updateWorkStartDate(day("2025-09-10"))
        LeavePrefs.updatePeriod("연차", LeavePeriod.SERVICE_YEAR)
        LeavePrefs.updateQuota("연차", 10)
        LeavePrefs.updateServiceQuota("연차", 1, 12)
        LeavePrefs.updateServiceQuota("연차", 2, 15)
        LeavePrefs.updateYearQuota("연차", 2026, 99)
        LeavePrefs.recount(listOf(
            task("일근", "2025-09-09", "연차"),
            task("일근", "2026-01-01", "연차"),
            task("주간", "2026-09-09", "연차"),
            task("일근", "2026-09-10", "연차")
        ))
        assertEquals(10, LeavePrefs.remaining("연차", day("2026-09-09")))
        assertEquals(14, LeavePrefs.remaining("연차", day("2026-09-10")))
        assertEquals(0, LeavePrefs.used("연차", day("2025-09-09")))
        assertNull(LeavePrefs.remaining("연차", day("2025-09-09")))
        LeavePrefs.updateServiceQuota("연차", 2, 0)
        assertEquals(-1, LeavePrefs.remaining("연차", day("2026-09-10")))
        LeavePrefs.updateServiceQuota("연차", 2, null)
        assertEquals(9, LeavePrefs.remaining("연차", day("2026-09-10")))
        LeavePrefs.updatePeriod("연차", LeavePeriod.YEAR)
        assertEquals(96, LeavePrefs.remaining("연차", day("2026-09-10")))
        LeavePrefs.updatePeriod("연차", LeavePeriod.MONTH)
        assertEquals(97, LeavePrefs.remaining("연차", day("2026-09-10")))
        LeavePrefs.updatePeriod("연차", LeavePeriod.NONE)
        assertEquals(6, LeavePrefs.remaining("연차", day("2026-09-10")))
    }

    @Test fun dayDutyIsAWorkShiftAndEarnsItsOwnWeight() {
        val duty = task("일근", "2026-09-10")
        assertTrue(duty.isShiftTask())
        assertTrue(duty.isWorkShift())
        assertTrue(duty.isWorkOn(day("2026-09-10")))
        assertEquals(listOf(day("2026-09-10") to "일근"),
            redDaysWorked(listOf(duty), day("2026-09-10"), day("2026-09-10")) { true })
        assertTrue(redDaysWorked(listOf(duty.withLeaveToggled(day("2026-09-10"), "병가")),
            day("2026-09-10"), day("2026-09-10")) { true }.isEmpty())
        val switched = task("휴무", "2026-09-10").withDayShift(day("2026-09-10"), "일근")
        assertTrue(switched.isWorkOn(day("2026-09-10")))
        assertEquals("일근", switched.shiftTitleOn(day("2026-09-10")))
    }
}
