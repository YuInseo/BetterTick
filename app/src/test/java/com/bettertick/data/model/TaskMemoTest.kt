package com.bettertick.data.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskMemoTest {
    private val task = Task(
        id = "t", title = "운동", repeatRule = "DAILY",
        memos = listOf(
            TaskMemo(id = "a", text = "공통"),
            TaskMemo(id = "b", text = "9일만", day = "2026-10-09"),
            TaskMemo(id = "c", text = "", day = "")
        ),
        dayNotes = mapOf("2026-10-10" to "예전 메모")
    )

    @Test fun memosOnADayAreSharedPlusThatDay() {
        assertEquals(listOf("공통", "9일만"), task.memosOn(LocalDate.parse("2026-10-09")).map { it.text })
        assertEquals(listOf("공통", "예전 메모"), task.memosOn(LocalDate.parse("2026-10-10")).map { it.text })
        assertEquals(listOf("공통"), task.memosOn(LocalDate.parse("2026-10-11")).map { it.text })
    }

    @Test fun withoutADayEverythingShows() {
        assertEquals(3, task.memosOn(null).size)
    }
}
