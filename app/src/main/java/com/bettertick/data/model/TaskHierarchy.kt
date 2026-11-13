package com.bettertick.data.model

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

// ── 하위 할일 (subtasks) ─────────────────────────────────────────────────────
//
// A subtask is an ordinary task whose schedule is copied from its parent:
// the parent's start day, repeat rule, repeat end and skipped days, combined
// with the subtask's own time-of-day and length. Keeping the copy on the
// subtask itself means the calendar, timetable, widget and reminders all work
// unchanged (they only call [occursOn]); TaskRepository re-copies whenever the
// parent's schedule changes. Nesting is one level deep.

/** Nested under another task. */
val Task.isSubtask: Boolean get() = parentId.isNotBlank()

/**
 * Can this task be dropped under [parent]? Not onto itself, not under a
 * subtask (one level only), not when it already has subtasks of its own,
 * and not under the parent it already has.
 */
fun Task.canNestUnder(parent: Task, all: List<Task>): Boolean =
    id.isNotBlank() && parent.id.isNotBlank() && parent.id != id &&
        !parent.isSubtask && parentId != parent.id &&
        all.none { it.parentId == id }

private fun Task.localTimeOrMidnight(zone: ZoneId): LocalTime =
    dueDate?.toDate()?.toInstant()?.atZone(zone)?.toLocalTime() ?: LocalTime.MIDNIGHT

private fun at(day: LocalDate, time: LocalTime, zone: ZoneId): Timestamp =
    Timestamp(Date.from(day.atTime(time).atZone(zone).toInstant()))

/**
 * This task following [parent]'s schedule with its own time kept.
 * [fresh] = just linked: the old skipped days belonged to the old schedule,
 * so only the parent's are taken. Otherwise (re-sync) the subtask's own
 * skipped days are kept too.
 */
fun Task.followingSchedule(parent: Task, fresh: Boolean = false): Task {
    val zone = ZoneId.systemDefault()
    val parentDay = parent.dueDate?.toDate()?.toInstant()?.atZone(zone)?.toLocalDate()
        ?: return copy(parentId = parent.id)
    return copy(
        parentId = parent.id,
        dueDate = at(parentDay, localTimeOrMidnight(zone), zone),
        repeatRule = parent.repeatRule,
        repeatEnd = parent.repeatEnd,
        exceptions = if (fresh) parent.exceptions else (exceptions + parent.exceptions).distinct()
    )
}

/** Does this subtask already match [parent]'s schedule (nothing to write)? */
fun Task.followsSchedule(parent: Task): Boolean {
    val synced = followingSchedule(parent)
    return synced.dueDate == dueDate && synced.repeatRule == repeatRule &&
        synced.repeatEnd == repeatEnd && synced.exceptions.toSet() == exceptions.toSet()
}

/** Back to a standalone one-off task on [day], at its own time. */
fun Task.unlinkedOn(day: LocalDate): Task {
    val zone = ZoneId.systemDefault()
    return copy(
        parentId = "",
        dueDate = at(day, localTimeOrMidnight(zone), zone),
        repeatRule = null,
        repeatEnd = null,
        exceptions = emptyList()
    )
}

/**
 * Day list with every subtask moved right after its parent (in their
 * existing order). Subtasks whose parent isn't in the list stay where they are.
 */
fun List<Task>.groupedUnderParents(): List<Task> {
    val ids = mapTo(HashSet()) { it.id }
    val children = filter { it.parentId.isNotBlank() && it.parentId in ids }.groupBy { it.parentId }
    if (children.isEmpty()) return this
    val out = ArrayList<Task>(size)
    for (t in this) {
        if (t.parentId.isNotBlank() && t.parentId in ids) continue
        out += t
        children[t.id]?.let { out += it }
    }
    return out
}
