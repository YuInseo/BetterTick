package com.bettertick.data.model

import java.time.LocalDate

/**
 * 날짜별 체크: a repeating task is checked off one occurrence at a time
 * ([Task.completedDates]) unless the task opts into checking the whole
 * series at once ([Task.checkWholeSeries]).
 */
fun Task.checksPerDay(): Boolean = !repeatRule.isNullOrBlank() && !checkWholeSeries

/** Is the occurrence on [day] done? */
fun Task.completedOn(day: LocalDate): Boolean =
    if (checksPerDay()) day.toString() in completedDates else isCompleted

/** The task as it stands on [day]: [Task.isCompleted] reflects that occurrence. */
fun Task.forDay(day: LocalDate): Task =
    if (checksPerDay()) copy(isCompleted = day.toString() in completedDates) else this
