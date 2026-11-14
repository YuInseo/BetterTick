package com.bettertick.ui.screens.tasks.components

import com.bettertick.ui.theme.AppPrefs
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal fun formatDueLabel(ts: Timestamp?, durationMinutes: Int, occurrenceDate: LocalDate? = null): String =
    formatTaskDateLabel(ts?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime(),
        durationMinutes, occurrenceDate, AppPrefs::formatTime)

/** The selected repetition supplies the day; the series supplies its time. Display only. */
internal fun formatTaskDateLabel(
    scheduled: LocalDateTime?,
    durationMinutes: Int,
    occurrenceDate: LocalDate? = null,
    formatTime: (LocalTime) -> String
): String {
    if (scheduled == null) return "날짜 설정"
    val date = occurrenceDate ?: scheduled.toLocalDate()
    val time = scheduled.toLocalTime()
    val weekday = listOf("월", "화", "수", "목", "금", "토", "일")[date.dayOfWeek.value - 1]
    val label = "${date.monthValue}월 ${date.dayOfMonth}일 ($weekday)"
    if (time == LocalTime.MIDNIGHT) return label
    val start = formatTime(time)
    if (durationMinutes <= 0) return "$label · $start"
    val end = date.atTime(time).plusMinutes(durationMinutes.toLong())
    val days = java.time.temporal.ChronoUnit.DAYS.between(date, end.toLocalDate())
    val overnight = if (days > 0) " (+${days}일)" else ""
    return "$label · $start–${formatTime(end.toLocalTime())}$overnight"
}
