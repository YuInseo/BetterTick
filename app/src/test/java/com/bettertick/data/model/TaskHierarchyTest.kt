package com.bettertick.data.model

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskHierarchyTest {
    private val zone = ZoneId.systemDefault()
    private fun at(value: String) = Timestamp(Date.from(LocalDateTime.parse(value).atZone(zone).toInstant()))
    private fun Task.day(): LocalDate = dueDate!!.toDate().toInstant().atZone(zone).toLocalDate()
    private fun Task.time(): LocalTime = dueDate!!.toDate().toInstant().atZone(zone).toLocalTime()

    // 주간 shift: every 4 days from Nov 6, 09:00–17:00, Nov 10 skipped.
    private val shift = Task(
        id = "shift",
        title = "주간",
        dueDate = at("2026-11-06T09:00"),
        durationMinutes = 480,
        repeatRule = "CUSTOM|ByDueDate|4|Day||",
        repeatEnd = "DATE:2026-12-31",
        exceptions = listOf("2026-11-10")
    )

    // 수면: a one-off at 23:00 on Nov 14, 7 hours.
    private val sleep = Task(id = "sleep", title = "수면", dueDate = at("2026-11-14T23:00"), durationMinutes = 420)

    @Test fun nestedTaskRepeatsWithParentAtItsOwnTime() {
        val child = sleep.followingSchedule(shift, fresh = true)
        assertEquals("shift", child.parentId)
        assertEquals(LocalDate.parse("2026-11-06"), child.day())
        assertEquals(LocalTime.of(23, 0), child.time())
        assertEquals(420, child.durationMinutes)
        assertEquals(shift.repeatRule, child.repeatRule)
        assertEquals(shift.repeatEnd, child.repeatEnd)
        assertTrue(child.occursOn(LocalDate.parse("2026-11-14")))
        assertTrue(child.occursOn(LocalDate.parse("2026-11-18")))
        assertFalse(child.occursOn(LocalDate.parse("2026-11-15")))
        // Skipped with the parent, and stops when the parent's series ends.
        assertFalse(child.occursOn(LocalDate.parse("2026-11-10")))
        assertTrue(child.occursOn(LocalDate.parse("2026-12-28")))
        assertFalse(child.occursOn(LocalDate.parse("2027-01-01")))
        assertTrue(child.followsSchedule(shift))
    }

    @Test fun resyncKeepsChildsOwnSkippedDays() {
        val child = sleep.followingSchedule(shift, fresh = true)
            .let { it.copy(exceptions = it.exceptions + "2026-11-18") }
        val moved = shift.copy(dueDate = at("2026-11-07T09:00"), exceptions = listOf("2026-11-11"))
        assertFalse(child.followsSchedule(moved))
        val synced = child.followingSchedule(moved)
        assertEquals(LocalDate.parse("2026-11-07"), synced.day())
        assertEquals(LocalTime.of(23, 0), synced.time())
        assertTrue("2026-11-18" in synced.exceptions)
        assertTrue("2026-11-11" in synced.exceptions)
    }

    @Test fun freshLinkDropsTheOldScheduleSkips() {
        val withOldSkip = sleep.copy(exceptions = listOf("2026-11-14"))
        assertEquals(listOf("2026-11-10"), withOldSkip.followingSchedule(shift, fresh = true).exceptions)
    }

    @Test fun unlinkLeavesAOneOffOnThatDay() {
        val child = sleep.followingSchedule(shift, fresh = true)
        val free = child.unlinkedOn(LocalDate.parse("2026-11-18"))
        assertEquals("", free.parentId)
        assertNull(free.repeatRule)
        assertNull(free.repeatEnd)
        assertTrue(free.exceptions.isEmpty())
        assertEquals(LocalDate.parse("2026-11-18"), free.day())
        assertEquals(LocalTime.of(23, 0), free.time())
        assertTrue(free.occursOn(LocalDate.parse("2026-11-18")))
        assertFalse(free.occursOn(LocalDate.parse("2026-11-22")))
    }

    @Test fun nestingIsOneLevelDeep() {
        val gym = Task(id = "gym", title = "헬스")
        val child = sleep.followingSchedule(shift, fresh = true)
        val all = listOf(shift, child, gym)
        assertTrue(gym.canNestUnder(shift, all))
        assertFalse(gym.canNestUnder(gym, all))          // itself
        assertFalse(gym.canNestUnder(child, all))        // under a subtask
        assertFalse(shift.canNestUnder(gym, all))        // a parent can't become a child
        assertFalse(child.canNestUnder(shift, all))      // already there
    }

    @Test fun dayListPutsSubtasksUnderTheirParent() {
        val child = sleep.followingSchedule(shift, fresh = true)
        val gym = Task(id = "gym", title = "헬스")
        val orphan = Task(id = "orphan", title = "x", parentId = "gone")
        val grouped = listOf(child, gym, orphan, shift).groupedUnderParents()
        assertEquals(listOf("gym", "orphan", "shift", "sleep"), grouped.map { it.id })
    }
}
