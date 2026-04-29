package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Task
import com.bettertick.data.model.occursOn
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Week timeline grid. Time axis runs vertically down the left side (GMT+9,
 * 12 AM → 12 AM next day). Days run horizontally across. Timed tasks render
 * as vertical blocks positioned by their start time and durationMinutes.
 * All-day tasks appear in a stacked strip above the grid, under the day
 * headers.
 *
 * Pinch-to-zoom adjusts [hourHeightDp] so the user can spread hours out
 * (see 30-min detail) or pack them in (see the whole day at once).
 */
@Composable
fun WeekTimelineView(
    weekDates: List<LocalDate>,
    allTasks: List<Task>,
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val minHourHeight = 32.dp
    val maxHourHeight = 140.dp
    var hourHeight by remember { mutableStateOf(52.dp) }

    val scrollState = rememberScrollState()
    val timeGutter = 52.dp

    // Partition tasks into all-day (midnight dueDate) vs timed.
    data class PerDay(val allDay: List<Task>, val timed: List<Task>)
    val byDay: Map<LocalDate, PerDay> = remember(weekDates, allTasks) {
        weekDates.associateWith { d ->
            val dayTasks = allTasks.filter { it.occursOn(d) && !it.isAbandoned }
            val (allDay, timed) = dayTasks.partition { task ->
                val lt = task.dueDate?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())?.toLocalTime()
                lt == null || lt == LocalTime.MIDNIGHT
            }
            PerDay(allDay, timed)
        }
    }
    val maxAllDayRows = byDay.values.maxOfOrNull { it.allDay.size } ?: 0
    val allDayRowHeight = 22.dp
    val allDayBandHeight = (allDayRowHeight + 2.dp) * maxAllDayRows
    val gridHeight = hourHeight * 24

    // Jump to ~8 AM on first composition so the user lands in daytime hours.
    LaunchedEffect(Unit) {
        val target = with(density) { (hourHeight * 8).toPx() }.roundToInt()
        scrollState.scrollTo(target)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        val next = (hourHeight.value * zoom)
                            .coerceIn(minHourHeight.value, maxHourHeight.value)
                        hourHeight = next.dp
                    }
                }
            }
    ) {
        // Day-of-week header row + date numbers.
        DayHeaderRow(
            weekDates = weekDates,
            today = today,
            selectedDate = selectedDate,
            timeGutter = timeGutter,
            onDateSelected = onDateSelected
        )

        // All-day strip
        if (maxAllDayRows > 0) {
            AllDayStrip(
                weekDates = weekDates,
                byDay = byDay.mapValues { it.value.allDay },
                rows = maxAllDayRows,
                rowHeight = allDayRowHeight,
                timeGutter = timeGutter,
                height = allDayBandHeight
            )
        }

        // Scrollable time grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Row(modifier = Modifier.height(gridHeight)) {
                // Time gutter
                HourGutter(hourHeight = hourHeight, width = timeGutter)

                // Day columns
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDates.forEach { date ->
                        DayColumn(
                            date = date,
                            today = today,
                            timed = byDay[date]?.timed ?: emptyList(),
                            hourHeight = hourHeight,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeaderRow(
    weekDates: List<LocalDate>,
    today: LocalDate,
    selectedDate: LocalDate,
    timeGutter: Dp,
    onDateSelected: (LocalDate) -> Unit
) {
    val koreanDow = listOf("일", "월", "화", "수", "목", "금", "토")
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(timeGutter))
            weekDates.forEach { d ->
                val dowIdx = d.dayOfWeek.value % 7
                Text(
                    text = koreanDow[dowIdx],
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(timeGutter))
            weekDates.forEach { d ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .clickable { onDateSelected(d) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (d == selectedDate) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF2F7CF6))
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = d.dayOfMonth.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                d == selectedDate -> Color.White
                                d == today -> Color(0xFF2F7CF6)
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllDayStrip(
    weekDates: List<LocalDate>,
    byDay: Map<LocalDate, List<Task>>,
    rows: Int,
    rowHeight: Dp,
    timeGutter: Dp,
    height: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Gutter label: GMT+9
        Box(
            modifier = Modifier.width(timeGutter).fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "GMT+9",
                fontSize = 9.sp,
                color = TextTertiary,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        weekDates.forEach { d ->
            val tasks = byDay[d] ?: emptyList()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp),
                verticalArrangement = Arrangement.Top
            ) {
                tasks.take(rows).forEach { task ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(bottom = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF4257B2).copy(alpha = if (task.isCompleted) 0.35f else 0.9f))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = task.title,
                            fontSize = 10.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourGutter(hourHeight: Dp, width: Dp) {
    val now = LocalTime.now()
    Column(modifier = Modifier.width(width)) {
        for (h in 0..24) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight),
                contentAlignment = Alignment.TopEnd
            ) {
                val label = when {
                    h == 0 || h == 24 -> "12 AM"
                    h < 12 -> "$h AM"
                    h == 12 -> "12 PM"
                    else -> "${h - 12} PM"
                }
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(end = 6.dp, top = 0.dp)
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    date: LocalDate,
    today: LocalDate,
    timed: List<Task>,
    hourHeight: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gridLineColor = TextTertiary.copy(alpha = 0.25f)
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val nowTime = LocalTime.now()
    val isToday = date == today

    Layout(
        modifier = modifier
            .drawBehind {
                // Horizontal hour lines
                val strokePx = 1f
                for (h in 0..24) {
                    val y = h * hourHeightPx
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokePx
                    )
                }
                // Left vertical separator
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = strokePx
                )
                // Current-time indicator — only on today's column.
                if (isToday) {
                    val y = (nowTime.hour + nowTime.minute / 60f) * hourHeightPx
                    drawLine(
                        color = OverdueRed,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                }
            },
        content = {
            timed.forEach { task ->
                TimedBlock(task = task, date = date)
            }
        }
    ) { measurables, constraints ->
        val colWidth = constraints.maxWidth
        val colHeight = (hourHeightPx * 24).roundToInt()

        val placements = measurables.mapIndexed { i, m ->
            val task = timed[i]
            val lt = task.dueDate?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
                ?: LocalDateTime.of(date, LocalTime.NOON)
            val startMinutes = lt.hour * 60 + lt.minute
            val dur = task.durationMinutes.coerceAtLeast(15)
            val top = (startMinutes / 60f * hourHeightPx).roundToInt()
            val blockH = (dur / 60f * hourHeightPx).roundToInt().coerceAtLeast(
                with(density) { 18.dp.toPx() }.roundToInt()
            )
            val placeable = m.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = colWidth,
                    minHeight = blockH,
                    maxHeight = blockH
                )
            )
            Triple(placeable, top, blockH)
        }

        layout(colWidth, colHeight) {
            placements.forEach { (p, top, _) ->
                p.place(0, top)
            }
        }
    }
}

@Composable
private fun TimedBlock(task: Task, date: LocalDate) {
    val lt = task.dueDate?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
    val startLabel = lt?.let { formatShortTime(it.toLocalTime()) } ?: ""
    val endLabel = lt?.plusMinutes(task.durationMinutes.toLong())
        ?.let { formatShortTime(it.toLocalTime()) } ?: ""
    val alpha = if (task.isCompleted) 0.35f else 0.85f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF4257B2).copy(alpha = alpha))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = task.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )
        if (startLabel.isNotEmpty()) {
            Text(
                text = "$startLabel-$endLabel",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

private fun formatShortTime(t: LocalTime): String {
    val ampm = if (t.hour < 12) "오전" else "오후"
    val h = when {
        t.hour == 0 -> 12
        t.hour > 12 -> t.hour - 12
        else -> t.hour
    }
    return if (t.minute == 0) "$ampm ${h}시" else "$ampm $h:%02d".format(t.minute)
}
