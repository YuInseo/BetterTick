package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.ui.components.MarkdownText
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import com.bettertick.ui.theme.priorityColor
import com.bettertick.util.DateUtils.toLocalDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * TickTick-style list row:
 *
 *   [▢]  Title ................................  ⟳ ⏰  오늘 오후 2:00
 *        notes preview / #tag chips                         list name
 *
 * - the checkbox is a small rounded square whose border carries the task's
 *   priority color (high red / medium yellow / low blue / none gray),
 * - the due date sits on the right: red when overdue, accent when today,
 *   gray otherwise,
 * - completed / won't-do rows fade their title to the tertiary text color.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * When non-null, the date label reflects [overrideDate] instead of the
     * task's own due date. Used by the calendar's per-day panel so a daily
     * recurring task viewed on 4/26 reads "오늘" rather than its original
     * "4월 19일".
     */
    overrideDate: LocalDate? = null,
    listName: String = "기본함",
    resolvedTags: List<Tag> = emptyList()
) {
    val shownDate = overrideDate ?: task.dueDate?.toLocalDate()
    // Recurring tasks aren't really "overdue" — even when their stored
    // due date is in the past, the rule keeps producing fresh occurrences.
    val isOverdue = shownDate?.let {
        it.isBefore(LocalDate.now()) && !task.isCompleted && !task.isAbandoned &&
            task.repeatRule.isNullOrBlank()
    } ?: false
    val isDone = task.isCompleted || task.isAbandoned

    // Start time comes from the task's own dueDate (even when overrideDate
    // shifts the shown day for a recurrence). Midnight = "no time".
    val startTime = task.dueDate?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())?.toLocalTime()
    val hasTime = startTime != null && startTime != LocalTime.MIDNIGHT

    val accent = ThemeState.accentColor
    val dateColor = when {
        isDone -> TextTertiary
        isOverdue -> OverdueRed
        shownDate == LocalDate.now() -> accent
        else -> TextSecondary
    }
    val metaIconTint = TextTertiary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable { onClick() }
            .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 44dp touch target keeps the parent drag detector from stealing the
        // tap; the visible box is TickTick's 18dp rounded square.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable { onToggleComplete() },
            contentAlignment = Alignment.Center
        ) {
            TickCheckbox(
                priority = task.priority,
                isCompleted = task.isCompleted,
                isAbandoned = task.isAbandoned
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            MarkdownText(
                text = task.title,
                color = if (isDone) TextTertiary else TextPrimary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.notes.isNotBlank()) {
                MarkdownText(
                    text = task.notes,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (resolvedTags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    resolvedTags.forEach { tag ->
                        val tagColor = runCatching {
                            Color(android.graphics.Color.parseColor(tag.color))
                        }.getOrDefault(accent)
                        TickTagChip(name = tag.name, color = tagColor)
                    }
                }
            }
        }

        if (shownDate != null || !task.repeatRule.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.notes.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Notes,
                            contentDescription = "메모",
                            tint = metaIconTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (!task.repeatRule.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "반복",
                            tint = metaIconTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (hasTime) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = "알림",
                            tint = metaIconTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    shownDate?.let { date ->
                        Text(
                            text = buildDateLabel(date, startTime, task.durationMinutes, hasTime),
                            fontSize = 12.sp,
                            color = dateColor,
                            maxLines = 1
                        )
                    }
                }
                if (listName.isNotBlank()) {
                    Text(
                        text = listName,
                        fontSize = 11.sp,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else if (listName.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = listName,
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1
            )
        }
    }
}

/**
 * TickTick checkbox — 18dp rounded square. Border color = priority color;
 * completed fills gray with a white check, "won't do" shows a gray ✕.
 */
@Composable
fun TickCheckbox(
    priority: Int,
    isCompleted: Boolean,
    isAbandoned: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 18.dp
) {
    val shape = RoundedCornerShape(4.dp)
    val border = priorityColor(priority)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .then(
                when {
                    isCompleted -> Modifier.background(TextTertiary)
                    isAbandoned -> Modifier.border(1.5.dp, TextTertiary, shape)
                    priority > 0 -> Modifier
                        .background(border.copy(alpha = 0.08f))
                        .border(1.5.dp, border, shape)
                    else -> Modifier.border(1.5.dp, border, shape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isCompleted -> Icon(
                Icons.Default.Check,
                contentDescription = "완료",
                tint = Color.White,
                modifier = Modifier.size(size * 0.75f)
            )
            isAbandoned -> Icon(
                Icons.Default.Close,
                contentDescription = "하지 않음",
                tint = TextTertiary,
                modifier = Modifier.size(size * 0.7f)
            )
            else -> {}
        }
    }
}

/** TickTick tag pill — tinted background with the tag color as text. */
@Composable
fun TickTagChip(name: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (ThemeState.isDark) 0.24f else 0.14f))
            .padding(horizontal = 7.dp, vertical = 1.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
        )
    }
}

private fun buildDateLabel(
    date: LocalDate,
    startTime: LocalTime?,
    durationMinutes: Int,
    hasTime: Boolean
): String {
    val today = LocalDate.now()
    val dayLabel = when (date) {
        today -> "오늘"
        today.plusDays(1) -> "내일"
        today.minusDays(1) -> "어제"
        else -> if (date.year == today.year) "${date.monthValue}월 ${date.dayOfMonth}일"
            else "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일"
    }
    if (!hasTime || startTime == null) return dayLabel
    val start = formatKorean12h(startTime)
    val time = if (durationMinutes > 0) {
        val end = startTime.plusMinutes(durationMinutes.toLong())
        val sameHalf = (startTime.hour < 12) == (end.hour < 12)
        "$start - ${if (sameHalf) formatClock(end) else formatKorean12h(end)}"
    } else start
    return if (date == today) time else "$dayLabel, $time"
}

private fun formatKorean12h(time: LocalTime): String {
    val ampm = if (time.hour < 12) "오전" else "오후"
    return "$ampm ${formatClock(time)}"
}

private fun formatClock(time: LocalTime): String {
    val h = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    return "%d:%02d".format(h, time.minute)
}
