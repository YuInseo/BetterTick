package com.bettertick.widget.util

import com.bettertick.data.model.Task
import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object WidgetDateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun today(): LocalDate = LocalDate.now()

    fun todayString(): String = today().format(dateFormatter)

    fun weekStart(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    fun weekEnd(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    fun weekDates(date: LocalDate = today()): List<LocalDate> {
        val start = weekStart(date)
        return (0..6).map { start.plusDays(it.toLong()) }
    }

    fun monthStart(date: LocalDate = today()): LocalDate =
        date.withDayOfMonth(1)

    fun monthEnd(date: LocalDate = today()): LocalDate =
        date.withDayOfMonth(date.lengthOfMonth())

    fun monthDates(date: LocalDate = today()): List<LocalDate> {
        val start = monthStart(date)
        val end = monthEnd(date)
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }

    fun calendarGridDates(date: LocalDate = today()): List<LocalDate?> {
        val firstDay = monthStart(date)
        val lastDay = monthEnd(date)
        val startOffset = firstDay.dayOfWeek.value % 7 // Sun=0
        val grid = mutableListOf<LocalDate?>()
        repeat(startOffset) { grid.add(null) }
        var current = firstDay
        while (!current.isAfter(lastDay)) {
            grid.add(current)
            current = current.plusDays(1)
        }
        return grid
    }

    fun Task.localDueDate(): LocalDate? {
        return dueDate?.let {
            it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    // occursOn() lives in com.bettertick.data.model.TaskRecurrence so both
    // the widget and the in-app calendar share one recurrence engine.

    fun dayOfWeekKorean(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "월"
        2 -> "화"
        3 -> "수"
        4 -> "목"
        5 -> "금"
        6 -> "토"
        7 -> "일"
        else -> ""
    }

    fun dayOfWeekKoreanSunFirst(index: Int): String = when (index) {
        0 -> "일"
        1 -> "월"
        2 -> "화"
        3 -> "수"
        4 -> "목"
        5 -> "금"
        6 -> "토"
        else -> ""
    }
}
