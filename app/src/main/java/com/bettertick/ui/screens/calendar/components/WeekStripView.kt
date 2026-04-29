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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Task
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Compact single-week strip shown when a date is selected. Sits above the
 * SelectedDatePanel and lets the user see the focused week's events while the
 * task list takes the remaining space below.
 */
@Composable
fun WeekStripView(
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
    val today = LocalDate.now()
    val maxVisibleTasks = 3

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        weekDates.forEach { date ->
            WeekDayCell(
                date = date,
                isToday = date == today,
                isSelected = date == selectedDate,
                tasks = tasksByDate[date] ?: emptyList(),
                maxVisibleTasks = maxVisibleTasks,
                onClick = { onDateSelected(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    tasks: List<Task>,
    maxVisibleTasks: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .then(
                    when {
                        isSelected -> Modifier
                            .clip(CircleShape)
                            .background(Orange)
                        isToday -> Modifier
                            .clip(CircleShape)
                            .border(1.5.dp, Orange, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> Orange
                    else -> MaterialTheme.colorScheme.onBackground
                },
                textAlign = TextAlign.Center
            )
        }

        if (tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            val visible = tasks.take(maxVisibleTasks)
            val overflow = tasks.size - maxVisibleTasks

            visible.forEach { task ->
                StripTaskChip(
                    title = task.title,
                    isCompleted = task.isCompleted
                )
                Spacer(modifier = Modifier.height(1.dp))
            }
            if (overflow > 0) {
                Text(
                    text = "+$overflow",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StripTaskChip(
    title: String,
    isCompleted: Boolean
) {
    val chipColor = if (isCompleted) Color(0xFF8B6914) else Color(0xFFCC7000)
    Text(
        text = if (isCompleted) "\u2715 $title" else title,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(chipColor)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    )
}
