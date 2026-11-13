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
// parent's schedule changes, down every level. Subtasks can have subtasks.

/** Nested under another task. */
val Task.isSubtask: Boolean get() = parentId.isNotBlank()

/**
 * Can this task be dropped under [parent]? Any depth is fine, but not onto
 * itself, not under one of its own subtasks (that would make a loop — checked
 * through [all]), and not under the parent it already has.
 */
fun Task.canNestUnder(parent: Task, all: List<Task>): Boolean {
    if (id.isBlank() || parent.id.isBlank() || parent.id == id || parentId == parent.id) return false
    val byId = all.associateBy { it.id }
    var cur: Task? = parent
    var steps = 0
    while (cur != null && cur.parentId.isNotBlank() && steps++ < 64) {
        if (cur.parentId == id) return false
        cur = byId[cur.parentId]
    }
    return true
}

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
 * Day list as a tree: every subtask right after its parent (then its own
 * subtasks, and so on), keeping the existing order among siblings. Subtasks
 * whose parent isn't in the list stay where they are, as roots.
 */
fun List<Task>.groupedUnderParents(): List<Task> {
    val ids = mapTo(HashSet()) { it.id }
    val children = filter { it.parentId.isNotBlank() && it.parentId in ids }.groupBy { it.parentId }
    if (children.isEmpty()) return this
    val out = ArrayList<Task>(size)
    val seen = HashSet<String>()
    fun add(t: Task) {
        if (!seen.add(t.id)) return
        out += t
        children[t.id]?.forEach { add(it) }
    }
    for (t in this) if (t.parentId.isBlank() || t.parentId !in ids) add(t)
    // A loop in bad data would leave tasks unvisited — keep them anyway.
    for (t in this) if (t.id !in seen) { seen += t.id; out += t }
    return out
}

/** How deep each task sits under parents that are also in this list (0 = top). */
fun List<Task>.subtaskDepths(): Map<String, Int> {
    val byId = associateBy { it.id }
    return associate { t ->
        var depth = 0
        var cur = t
        while (cur.parentId.isNotBlank() && depth < 16) {
            cur = byId[cur.parentId] ?: break
            depth++
        }
        t.id to depth
    }
}
