package com.bettertick.data.model

import com.bettertick.ui.screens.tasks.LeavePeriod
import com.bettertick.ui.screens.tasks.LeavePrefs
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkplaceLeaveTest {
    private fun day(value: String) = LocalDate.parse(value)
    private fun sickDay(date: String): Task {
        val ts = Timestamp(Date.from(day(date).atStartOfDay(ZoneId.systemDefault()).toInstant()))
        return Task(title = "주간", dueDate = ts, createdAt = ts, updatedAt = ts, leaves = mapOf(date to "병가"))
    }

    @Test fun eachDayCountsTowardTheWorkplaceItFallsIn() {
        // Earlier workplaces (e.g. the migrated '내 회사', still 재직 중) end
        // before 2030 for this test, so 2030 is only A and B.
        val before = LeavePrefs.workplaces.toList()
        before.forEach { LeavePrefs.updateWorkplaceDates(it.id, it.start, day("2029-12-31")) }
        val a = LeavePrefs.addWorkplace("A회사", day("2030-01-01"), day("2030-06-30"))
        val b = LeavePrefs.addWorkplace("B회사", day("2030-07-01"), null)
        try {
            LeavePrefs.selectEditing(a.id)
            LeavePrefs.updatePeriod("병가", LeavePeriod.YEAR)
            LeavePrefs.updateQuota("병가", 5)
            LeavePrefs.selectEditing(b.id)
            LeavePrefs.updatePeriod("병가", LeavePeriod.YEAR)
            LeavePrefs.updateQuota("병가", 10)
            LeavePrefs.recount(listOf(sickDay("2030-03-02"), sickDay("2030-03-03"), sickDay("2030-08-01")))

            assertEquals(a.id, LeavePrefs.workplaceOn(day("2030-03-02"))?.id)
            assertEquals(b.id, LeavePrefs.workplaceOn(day("2030-12-31"))?.id)
            // Same calendar year, but each company keeps its own count and allowance.
            assertEquals(3, LeavePrefs.remaining("병가", day("2030-03-02")))
            assertEquals(9, LeavePrefs.remaining("병가", day("2030-08-01")))

            val historyA = LeavePrefs.history(a.id)
            assertEquals(1, historyA.size)
            assertEquals("2030년", historyA[0].label)
            assertEquals(2, historyA[0].used)
            assertEquals(5, historyA[0].allowance)
            assertEquals(listOf(day("2030-03-02"), day("2030-03-03")), historyA[0].days)
            assertEquals(1, LeavePrefs.history(b.id).single().used)

            // A gap between jobs belongs to no workplace.
            LeavePrefs.updateWorkplaceDates(a.id, day("2030-01-01"), day("2030-05-31"))
            assertNull(LeavePrefs.workplaceOn(day("2030-06-15")))
            assertNull(LeavePrefs.remaining("병가", day("2030-06-15")))
        } finally {
            LeavePrefs.deleteWorkplace(a.id)
            LeavePrefs.deleteWorkplace(b.id)
            before.forEach { LeavePrefs.updateWorkplaceDates(it.id, it.start, it.end) }
            LeavePrefs.selectEditing(LeavePrefs.workplaces.first().id)
            LeavePrefs.recount(emptyList())
        }
    }

    @Test fun typedInMonthTotalReplacesTheRuleForThatMonthOnly() {
        val before = LeavePrefs.workplaces.toList()
        before.forEach { LeavePrefs.updateWorkplaceDates(it.id, it.start, day("2030-12-31")) }
        val c = LeavePrefs.addWorkplace("C회사", day("2031-01-01"), null)
        try {
            LeavePrefs.selectEditing(c.id)
            LeavePrefs.updatePeriod("병가", LeavePeriod.MONTH)
            LeavePrefs.updateQuota("병가", 5)
            LeavePrefs.recount(listOf(sickDay("2031-03-10")))
            assertEquals(4, LeavePrefs.remaining("병가", day("2031-03-01")))

            LeavePrefs.updateMonthTotal("병가", day("2031-03-15"), 1)
            assertEquals(1, LeavePrefs.monthTotal("병가", day("2031-03-01")))
            assertEquals(1, LeavePrefs.quota("병가", day("2031-03-01")))
            assertEquals(0, LeavePrefs.remaining("병가", day("2031-03-01")))
            // Other months keep the rule.
            assertEquals(5, LeavePrefs.remaining("병가", day("2031-04-01")))
            // 0 typed in is still an allowance (→ 1일 초과).
            LeavePrefs.updateMonthTotal("병가", day("2031-03-15"), 0)
            assertEquals(-1, LeavePrefs.remaining("병가", day("2031-03-01")))

            LeavePrefs.updateMonthTotal("병가", day("2031-03-15"), null)
            assertNull(LeavePrefs.monthTotal("병가", day("2031-03-01")))
            assertEquals(4, LeavePrefs.remaining("병가", day("2031-03-01")))
        } finally {
            LeavePrefs.deleteWorkplace(c.id)
            before.forEach { LeavePrefs.updateWorkplaceDates(it.id, it.start, it.end) }
            LeavePrefs.selectEditing(LeavePrefs.workplaces.first().id)
            LeavePrefs.recount(emptyList())
        }
    }
}
