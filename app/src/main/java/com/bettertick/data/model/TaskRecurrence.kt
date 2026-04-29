package com.bettertick.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Does this task occur on [target]? Non-recurring tasks occur only on their
 * [Task.dueDate]. Recurring tasks occur on every date matching their rule,
 * starting at or after the due date.
 *
 * Rule formats (see `toRule` in RepeatPickerDialog):
 *   "DAILY"
 *   "WEEKLY:<DAYOFWEEK>"   e.g. WEEKLY:MONDAY
 *   "MONTHLY:<n>"
 *   "YEARLY:<m>:<d>"
 *   "WEEKDAYS"
 * Unknown or null rules fall back to single-occurrence on the due date.
 *
 * Shared by the in-app calendar and the home-screen widget — both need
 * identical expansion so a daily task shows a dot/row on every visible day.
 */
fun Task.occursOn(target: LocalDate): Boolean {
    val start = this.dueDate?.toDate()
        ?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        ?: return false
    val rule = repeatRule
    if (rule.isNullOrBlank()) return start == target
    if (target.isBefore(start)) return false
    // A skipped occurrence never renders — even if the rule would otherwise
    // match. Serialized as ISO date strings ("2026-04-20") to stay safe for
    // Firestore round-tripping.
    if (exceptions.contains(target.toString())) return false

    // Short-circuit on end-date exhaustion. End-by-count is handled after
    // we determine the rule matches.
    val end = repeatEnd
    if (!end.isNullOrBlank() && end.startsWith("DATE:")) {
        val limit = runCatching { LocalDate.parse(end.removePrefix("DATE:")) }.getOrNull()
        if (limit != null && target.isAfter(limit)) return false
    }

    // Rule match check.
    val matches = if (rule.startsWith("CUSTOM|")) {
        customOccurs(start, target, rule)
    } else {
        val parts = rule.split(":")
        when (parts[0]) {
            "DAILY" -> true
            "WEEKLY" -> runCatching {
                target.dayOfWeek == DayOfWeek.valueOf(parts[1])
            }.getOrDefault(false)
            "MONTHLY" -> runCatching {
                target.dayOfMonth == parts[1].toInt()
            }.getOrDefault(false)
            "YEARLY" -> runCatching {
                target.monthValue == parts[1].toInt() &&
                    target.dayOfMonth == parts[2].toInt()
            }.getOrDefault(false)
            "WEEKDAYS" -> target.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            else -> start == target
        }
    }
    if (!matches) return false

    // Enforce end-by-count: count occurrences up to and including `target`;
    // if we've passed the cap, this one is beyond the limit.
    if (!end.isNullOrBlank() && end.startsWith("COUNT:")) {
        val cap = runCatching { end.removePrefix("COUNT:").toInt() }.getOrNull()
        if (cap != null) {
            var count = 0
            var d = start
            while (!d.isAfter(target)) {
                val hit = if (rule.startsWith("CUSTOM|")) customOccurs(start, d, rule)
                else {
                    val parts = rule.split(":")
                    when (parts[0]) {
                        "DAILY" -> true
                        "WEEKLY" -> runCatching { d.dayOfWeek == DayOfWeek.valueOf(parts[1]) }.getOrDefault(false)
                        "MONTHLY" -> runCatching { d.dayOfMonth == parts[1].toInt() }.getOrDefault(false)
                        "YEARLY" -> runCatching {
                            d.monthValue == parts[1].toInt() && d.dayOfMonth == parts[2].toInt()
                        }.getOrDefault(false)
                        "WEEKDAYS" -> d.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        else -> start == d
                    }
                }
                if (hit) count++
                if (count > cap) return false
                d = d.plusDays(1)
            }
            return count <= cap
        }
    }

    return true
}

/**
 * Custom recurrence is "every N <units>, optionally restricted to specific
 * days of week or explicit dates". Fields as produced by
 * [com.bettertick.ui.screens.tasks.components.toRule]:
 *   CUSTOM|<type>|<freqNum>|<unit>|<dow csv>|<dates csv>
 * Unit can be DAY / WEEK / MONTH / YEAR. For WEEK the day-of-week set
 * narrows which weekdays count; for everything else daysOfWeek is empty.
 */
private fun customOccurs(start: LocalDate, target: LocalDate, rule: String): Boolean = runCatching {
    val parts = rule.split("|")
    val freq = parts[2].toInt().coerceAtLeast(1)
    // CustomRepeatUnit.name serializes as "Day"/"Week"/"Month"/"Year", so
    // normalize before matching — historically this compared raw strings
    // and silently fell through to the `else` branch (single-occurrence).
    val unit = parts[3].uppercase()
    val dows = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
        ?.split(",")?.map { DayOfWeek.valueOf(it) }?.toSet()
        ?: emptySet()
    val dates = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
        ?.split(",")?.map { LocalDate.parse(it) }?.toSet()
        ?: emptySet()

    // Explicit-date list short-circuits every other field.
    if (dates.isNotEmpty()) return@runCatching target in dates

    when (unit) {
        "DAY" -> ChronoUnit.DAYS.between(start, target) % freq == 0L
        "WEEK" -> {
            val weeks = ChronoUnit.WEEKS.between(
                start.with(DayOfWeek.MONDAY),
                target.with(DayOfWeek.MONDAY)
            )
            if (weeks % freq != 0L) false
            else if (dows.isEmpty()) target.dayOfWeek == start.dayOfWeek
            else target.dayOfWeek in dows
        }
        "MONTH" -> {
            val months = ChronoUnit.MONTHS.between(
                start.withDayOfMonth(1),
                target.withDayOfMonth(1)
            )
            months % freq == 0L && target.dayOfMonth == start.dayOfMonth
        }
        "YEAR" -> {
            val years = ChronoUnit.YEARS.between(
                start.withDayOfYear(1),
                target.withDayOfYear(1)
            )
            years % freq == 0L &&
                target.monthValue == start.monthValue &&
                target.dayOfMonth == start.dayOfMonth
        }
        else -> start == target
    }
}.getOrDefault(false)
