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

/**
 * 날짜별 취소: a cancelled occurrence of a repeating task is kept in
 * [Task.completedDates] as "x:2026-11-18" (no new field, so stored data and the
 * security rules stay as they are; [completedOn] never matches the prefix).
 */
fun abandonedKey(day: LocalDate): String = "x:$day"

/** Is the occurrence on [day] cancelled (취소됨)? */
fun Task.abandonedOn(day: LocalDate): Boolean =
    isAbandoned || (checksPerDay() && abandonedKey(day) in completedDates)

/** The task as it stands on [day]: done / cancelled reflect that occurrence only. */
fun Task.forDay(day: LocalDate): Task =
    if (checksPerDay()) copy(isCompleted = day.toString() in completedDates, isAbandoned = abandonedOn(day))
    else this
