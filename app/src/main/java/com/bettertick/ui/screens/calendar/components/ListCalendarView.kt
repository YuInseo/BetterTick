package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Task
import com.bettertick.data.model.occursOn
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Scrollable chronological list of tasks grouped by date — "목록" view.
 * Shows tasks from today onward, sorted by date. Empty days are skipped.
 */
@Composable
fun ListCalendarView(
    allTasks: List<Task>,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedTasks: List<Pair<LocalDate, List<Task>>> = remember(allTasks, today) {
        val endDate = today.plusMonths(12)
        val byDate = mutableMapOf<LocalDate, MutableList<Task>>()
        var day = today
        while (!day.isAfter(endDate)) {
            val dayTasks = allTasks.filter { it.occursOn(day) && !it.isAbandoned }
            if (dayTasks.isNotEmpty()) byDate[day] = dayTasks.toMutableList()
            day = day.plusDays(1)
        }
        byDate.entries.sortedBy { it.key }.map { it.key to it.value.toList() }
    }

    val listState = rememberLazyListState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN) }

    // Scroll to the selected date's group on first display.
    LaunchedEffect(selectedDate, groupedTasks) {
        val target = selectedDate ?: return@LaunchedEffect
        val idx = groupedTasks.indexOfFirst { it.first == target }
        if (idx >= 0) listState.animateScrollToItem(idx * 2)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 12.dp)
    ) {
        groupedTasks.forEach { (date, tasks) ->
            item(key = "header_${date.toEpochDay()}") {
                ListDateHeader(
                    date = date,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    formatter = dateFormatter,
                    onClick = { onDateSelected(date) }
                )
            }
            items(tasks, key = { "${date.toEpochDay()}_${it.id}" }) { task ->
                ListTaskRow(task = task)
                Spacer(modifier = Modifier.height(4.dp))
            }
            item(key = "spacer_${date.toEpochDay()}") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ListDateHeader(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    formatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(
                    when {
                        isSelected -> Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                        isToday -> Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSelected -> Color.White
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = date.format(formatter),
            fontSize = 14.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground
            }
        )
    }
}

@Composable
private fun ListTaskRow(task: Task) {
    val alpha = if (task.isCompleted) 0.45f else 1f
    val timeLabel = task.dueDate?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())?.toLocalTime()
        ?.let { t ->
            if (t.hour == 0 && t.minute == 0) null
            else {
                val ampm = if (t.hour < 12) "오전" else "오후"
                val h = when {
                    t.hour == 0 -> 12
                    t.hour > 12 -> t.hour - 12
                    else -> t.hour
                }
                if (t.minute == 0) "$ampm ${h}시" else "$ampm $h:%02d".format(t.minute)
            }
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF4257B2).copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                maxLines = 1
            )
            if (timeLabel != null || !task.repeatRule.isNullOrBlank()) {
                Row {
                    if (timeLabel != null) {
                        Text(text = timeLabel, fontSize = 11.sp, color = TextSecondary)
                    }
                    if (!task.repeatRule.isNullOrBlank()) {
                        if (timeLabel != null) Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "반복", fontSize = 11.sp, color = TextTertiary)
                    }
                }
            }
        }
        if (task.isCompleted) {
            Text(text = "완료", fontSize = 11.sp, color = TextTertiary)
        }
    }
}
