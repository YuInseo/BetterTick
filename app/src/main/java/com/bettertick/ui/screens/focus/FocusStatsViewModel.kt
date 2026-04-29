package com.bettertick.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.FocusCategory
import com.bettertick.data.model.FocusSession
import com.bettertick.data.repository.FocusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class FocusStats(
    val sessions: List<FocusSession> = emptyList(),
    val categories: List<FocusCategory> = emptyList()
) {
    private val zone: ZoneId = ZoneId.systemDefault()

    private val completed: List<FocusSession> by lazy {
        sessions.filter { it.isCompleted && it.durationSeconds > 0 }
    }

    private fun localDate(s: FocusSession): LocalDate =
        s.startedAt.toDate().toInstant().atZone(zone).toLocalDate()

    fun todaySessionCount(): Int {
        val today = LocalDate.now()
        return completed.count { localDate(it) == today }
    }

    fun todaySeconds(): Long {
        val today = LocalDate.now()
        return completed.filter { localDate(it) == today }.sumOf { it.durationSeconds }
    }

    fun yesterdaySessionCount(): Int {
        val d = LocalDate.now().minusDays(1)
        return completed.count { localDate(it) == d }
    }

    fun yesterdaySeconds(): Long {
        val d = LocalDate.now().minusDays(1)
        return completed.filter { localDate(it) == d }.sumOf { it.durationSeconds }
    }

    fun totalSessionCount(): Int = completed.size
    fun totalSeconds(): Long = completed.sumOf { it.durationSeconds }

    /** Aggregated seconds by activityName, sorted descending, for the given window. */
    fun categoryBreakdown(window: StatsWindow): List<Pair<String, Long>> {
        val slice = sessionsInWindow(window)
        return slice.groupBy { it.activityName.ifBlank { "기타" } }
            .mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
            .toList()
            .sortedByDescending { it.second }
    }

    fun colorFor(name: String): String =
        categories.firstOrNull { it.name == name }?.color
            ?: completed.firstOrNull { it.activityName == name }?.activityColor
            ?: "#FF8C00"

    fun iconFor(name: String): String =
        categories.firstOrNull { it.name == name }?.icon
            ?: completed.firstOrNull { it.activityName == name }?.activityIcon
            ?: ""

    fun sessionsInWindow(window: StatsWindow): List<FocusSession> {
        val today = LocalDate.now()
        return when (window) {
            StatsWindow.Day -> completed.filter { localDate(it) == today }
            StatsWindow.Week -> {
                val (start, end) = weekBounds(today)
                completed.filter { val d = localDate(it); !d.isBefore(start) && !d.isAfter(end) }
            }
            StatsWindow.Month -> completed.filter {
                val d = localDate(it); d.year == today.year && d.month == today.month
            }
            StatsWindow.Year -> completed.filter { localDate(it).year == today.year }
        }
    }

    /** Sunday-Saturday bounds anchored on [today], matching [FocusDistributionWidget]. */
    fun weekBounds(today: LocalDate): Pair<LocalDate, LocalDate> {
        val sun = today.with(DayOfWeek.SUNDAY).let { s -> if (s.isAfter(today)) s.minusWeeks(1) else s }
        return sun to sun.plusDays(6)
    }

    /** Seconds per day (Sun..Sat) of the week containing [today]. */
    fun weeklySecondsByDay(today: LocalDate = LocalDate.now()): List<Long> {
        val (start, _) = weekBounds(today)
        val byDate = completed.groupBy { localDate(it) }
            .mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
        return (0..6).map { i -> byDate[start.plusDays(i.toLong())] ?: 0L }
    }

    /** Seconds started within each hour (0..23) for the given month. */
    fun peakHourSecondsForMonth(month: LocalDate): List<Long> {
        val buckets = LongArray(24)
        completed.forEach { s ->
            val zdt = s.startedAt.toDate().toInstant().atZone(zone)
            if (zdt.year == month.year && zdt.month == month.month) {
                buckets[zdt.hour] += s.durationSeconds
            }
        }
        return buckets.toList()
    }

    /** Map<LocalDate, seconds> for the given year. */
    fun yearlySecondsByDate(year: Int): Map<LocalDate, Long> {
        val result = mutableMapOf<LocalDate, Long>()
        completed.forEach { s ->
            val d = localDate(s)
            if (d.year == year) {
                result[d] = (result[d] ?: 0L) + s.durationSeconds
            }
        }
        return result
    }

    /** Sessions on [date], oldest→newest, for the timeline chart. */
    fun sessionsOnDate(date: LocalDate): List<FocusSession> =
        completed.filter { localDate(it) == date }.sortedBy { it.startedAt.toDate() }
}

enum class StatsWindow { Day, Week, Month, Year }

@HiltViewModel
class FocusStatsViewModel @Inject constructor(
    private val focusRepository: FocusRepository
) : ViewModel() {

    val stats: StateFlow<FocusStats> = combine(
        focusRepository.observeAllSessions(),
        focusRepository.observeCategories()
    ) { sessions, categories -> FocusStats(sessions, categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusStats())
}
