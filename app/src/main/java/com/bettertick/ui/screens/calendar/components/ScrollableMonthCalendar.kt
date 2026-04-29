package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Task
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate
import java.time.YearMonth

// File-scope constants — previously allocated on every DayCell recomposition,
// which added GC churn during scroll.
private val DayCellBorderColor = TextTertiary.copy(alpha = 0.35f)
private val TaskChipCompletedColor = Color(0xFF8B6914)
private val TaskChipActiveColor = Color(0xFFCC7000)

/**
 * Vertically scrollable calendar rendered week-by-week. Borders are drawn per
 * cell edge so each month's cells form a single continuous L-shaped outline
 * that flows naturally across shared week rows.
 */
@Composable
fun ScrollableMonthCalendar(
    months: List<YearMonth>,
    selectedDate: LocalDate?,
    lookup: TaskDateLookup,
    initialMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    onVisibleMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val weeks = remember(months) { buildWeeks(months) }
    val initialIndex = remember(weeks, initialMonth) {
        val target = initialMonth.atDay(1)
        weeks.indexOfFirst { it.contains(target) }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // When a date is selected, animate the calendar so the week containing
    // that date sits at the top of the viewport — the bottom card then has
    // room to stretch upward as the dominant content.
    LaunchedEffect(selectedDate, weeks) {
        val target = selectedDate ?: return@LaunchedEffect
        val idx = weeks.indexOfFirst { it.contains(target) }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    // Track the month that occupies the largest visible area, weighted by
    // each week's visible height × number of cells belonging to that month.
    LaunchedEffect(listState, weeks) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val scores = mutableMapOf<YearMonth, Long>()
            for (item in layoutInfo.visibleItemsInfo) {
                val week = weeks.getOrNull(item.index) ?: continue
                val top = item.offset
                val bottom = item.offset + item.size
                val visible = (minOf(bottom, viewportEnd) - maxOf(top, viewportStart))
                    .coerceAtLeast(0)
                if (visible == 0) continue
                week.groupingBy { YearMonth.from(it) }.eachCount().forEach { (m, c) ->
                    scores[m] = (scores[m] ?: 0L) + visible.toLong() * c
                }
            }
            scores.maxByOrNull { it.value }?.key
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { onVisibleMonthChanged(it) }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberCalendarFlingBehavior(),
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        // Stable key = first day of the week. Keeps item identity across
        // scroll and prevents full recomposition of every row when the list
        // shifts.
        items(
            count = weeks.size,
            key = { idx -> weeks[idx].first().toEpochDay() }
        ) { weekIdx ->
            WeekRow(
                week = weeks[weekIdx],
                weekIdx = weekIdx,
                allWeeks = weeks,
                today = today,
                selectedDate = selectedDate,
                lookup = lookup,
                onDateSelected = onDateSelected,
                showMonthChips = listState.isScrollInProgress
            )
        }
    }
}

private fun List<LocalDate>.dominantMonth(): YearMonth {
    return this
        .groupingBy { YearMonth.from(it) }
        .eachCount()
        .entries
        .maxByOrNull { it.value }!!
        .key
}

/** Sunday-starting week (List of 7 LocalDates) containing [date]. */
fun weekContaining(date: LocalDate): List<LocalDate> {
    val sundayOffset = date.dayOfWeek.value % 7 // ISO Mon=1..Sun=7; Sun -> 0
    val sunday = date.minusDays(sundayOffset.toLong())
    return (0..6).map { sunday.plusDays(it.toLong()) }
}

/** Build continuous weeks covering all [months], with Sunday-starting rows. */
private fun buildWeeks(months: List<YearMonth>): List<List<LocalDate>> {
    if (months.isEmpty()) return emptyList()
    val firstMonth = months.first()
    val lastMonth = months.last()
    val firstDay = firstMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7 // Sun = 0
    val start = firstDay.minusDays(startOffset.toLong())
    val lastDay = lastMonth.atEndOfMonth()
    val endOffset = (6 - lastDay.dayOfWeek.value % 7 + 7) % 7
    val end = lastDay.plusDays(endOffset.toLong())

    val weeks = mutableListOf<List<LocalDate>>()
    var current = start
    while (!current.isAfter(end)) {
        weeks.add((0..6).map { current.plusDays(it.toLong()) })
        current = current.plusDays(7)
    }
    return weeks
}

/** Public so CalendarScreen can render a single week in compact mode. */
@Composable
fun WeekRow(
    week: List<LocalDate>,
    weekIdx: Int,
    allWeeks: List<List<LocalDate>>,
    today: LocalDate,
    selectedDate: LocalDate?,
    lookup: TaskDateLookup,
    onDateSelected: (LocalDate) -> Unit,
    showMonthChips: Boolean = false
) {
    // Precompute the 7 edge-states once per week so DayCell stays skippable.
    // Without this, computeBorderEdges fired on every recomposition for every
    // cell, and the weeks list (unstable) dragged DayCell along with it.
    val edges = remember(weekIdx, allWeeks) {
        Array(7) { dayIdx -> computeBorderEdges(weekIdx, dayIdx, allWeeks) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        week.forEachIndexed { dayIdx, date ->
            DayCell(
                date = date,
                isToday = date == today,
                isSelected = date == selectedDate,
                tasks = lookup.tasksOn(date),
                showMonthChip = showMonthChips && date.dayOfMonth == 1,
                edges = edges[dayIdx],
                onClick = { onDateSelected(date) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

private data class BorderEdges(
    val top: Boolean,
    val right: Boolean,
    val bottom: Boolean,
    val left: Boolean
)

/** A cell draws a border on an edge if the neighbor is a different month. */
private fun computeBorderEdges(
    weekIdx: Int,
    dayIdx: Int,
    weeks: List<List<LocalDate>>
): BorderEdges {
    val current = weeks[weekIdx][dayIdx]
    val currentYm = YearMonth.from(current)

    fun sameMonthAt(w: Int, d: Int): Boolean {
        if (w < 0 || w > weeks.lastIndex) return false
        if (d < 0 || d > 6) return false
        return YearMonth.from(weeks[w][d]) == currentYm
    }

    return BorderEdges(
        top = !sameMonthAt(weekIdx - 1, dayIdx),
        right = !sameMonthAt(weekIdx, dayIdx + 1),
        bottom = !sameMonthAt(weekIdx + 1, dayIdx),
        left = !sameMonthAt(weekIdx, dayIdx - 1)
    )
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    tasks: List<Task>,
    showMonthChip: Boolean,
    edges: BorderEdges,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVisibleTasks = 3

    Column(
        modifier = modifier
            .drawBehind {
                val sw = 1.dp.toPx()
                val w = size.width
                val h = size.height
                if (edges.top) drawLine(DayCellBorderColor, Offset(0f, 0f), Offset(w, 0f), sw)
                if (edges.bottom) drawLine(DayCellBorderColor, Offset(0f, h), Offset(w, h), sw)
                if (edges.left) drawLine(DayCellBorderColor, Offset(0f, 0f), Offset(0f, h), sw)
                if (edges.right) drawLine(DayCellBorderColor, Offset(w, 0f), Offset(w, h), sw)
            }
            .clickable { onClick() }
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        // Keep the same 32.dp circular box in both states so the day cell
        // never shifts when the "4월" chip appears — only the text swaps.
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    when {
                        isSelected -> Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                        isToday -> Modifier
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showMonthChip) "${date.monthValue}월" else date.dayOfMonth.toString(),
                fontSize = if (showMonthChip) 11.sp else 14.sp,
                fontWeight = if (isToday || isSelected || showMonthChip) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> Color.White
                    else -> MaterialTheme.colorScheme.onBackground
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }

        if (tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            val visibleTasks = tasks.take(maxVisibleTasks)
            val overflow = tasks.size - visibleTasks.size

            visibleTasks.forEachIndexed { index, task ->
                val isLastWithOverflow = overflow > 0 && index == visibleTasks.lastIndex
                if (isLastWithOverflow) {
                    // Last visible chip shares its row with the overflow badge
                    // so the hidden-count reads "after the last task" rather
                    // than stacked on its own line below the chip list.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskChip(
                            title = task.title,
                            isCompleted = task.isCompleted,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        Text(
                            text = "+$overflow",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                } else {
                    TaskChip(
                        title = task.title,
                        isCompleted = task.isCompleted
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun TaskChip(
    title: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val chipColor = if (isCompleted) TaskChipCompletedColor else TaskChipActiveColor

    Text(
        text = if (isCompleted) "\u2715 $title" else title,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(chipColor)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    )
}
