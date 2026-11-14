package com.bettertick.ui.screens.calendar.components

import androidx.compose.runtime.Immutable
import com.bettertick.data.model.Task
import java.time.LocalDate

/**
 * @Immutable wrapper around the ViewModel's `tasksByDate` map so Compose can
 * skip recomposition of calendar children whose inputs haven't meaningfully
 * changed. Raw `Map` parameters are treated as unstable by the compiler and
 * force every downstream composable to re-run on each parent recomposition —
 * that showed up as visible scroll lag once the year view stacked ~500
 * cells per visible year.
 *
 * The backing map is replaced (not mutated) on every Firestore snapshot, so
 * equality by reference is correct for stability purposes.
 */
@Immutable
class TaskDateLookup(private val map: Map<LocalDate, List<Task>>) {
    fun hasTasksOn(date: LocalDate): Boolean = map[date]?.isNotEmpty() == true
    fun tasksOn(date: LocalDate): List<Task> = map[date].orEmpty()

    override fun equals(other: Any?): Boolean =
        other is TaskDateLookup && other.map === map

    override fun hashCode(): Int = System.identityHashCode(map)

    companion object {
        val Empty = TaskDateLookup(emptyMap())
    }
}
