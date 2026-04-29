package com.bettertick.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy. M. d.")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")
    private val dayOfWeekShort = DateTimeFormatter.ofPattern("E", Locale.KOREAN)

    fun LocalDate.toTimestamp(): Timestamp {
        val instant = this.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return Timestamp(Date.from(instant))
    }

    fun Timestamp.toLocalDate(): LocalDate {
        return this.toDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun Timestamp.toLocalDateTime(): LocalDateTime {
        return this.toDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    fun LocalDate.formatDisplay(): String = this.format(dateFormatter)

    fun LocalDate.formatMonthYear(): String = this.format(monthYearFormatter)

    fun LocalDate.formatDayOfWeek(): String = this.format(dayOfWeekShort)

    fun LocalDate.isOverdue(): Boolean = this.isBefore(LocalDate.now())

    fun LocalDate.isToday(): Boolean = this == LocalDate.now()

    fun LocalDate.daysUntil(): Long = ChronoUnit.DAYS.between(LocalDate.now(), this)

    fun LocalDate.getWeekDates(): List<LocalDate> {
        val monday = this.minusDays((this.dayOfWeek.value - 1).toLong())
        return (0L..6L).map { monday.plusDays(it) }
    }
}
