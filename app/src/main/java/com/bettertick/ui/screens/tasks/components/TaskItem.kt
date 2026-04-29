package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.ui.components.MarkdownText
import com.bettertick.ui.theme.AbandonedBlue
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.util.DateUtils.toLocalDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val SubtitleAccent = Color(0xFF4A90E2)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * When non-null, the subtitle reflects [overrideDate] instead of the
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
    // due date is in the past, the rule keeps producing fresh occurrences,
    // so showing red on the original date is misleading. Keep the normal
    // accent color for any task with a repeat rule.
    val isOverdue = shownDate?.let {
        it.isBefore(LocalDate.now()) && !task.isCompleted && !task.isAbandoned &&
            task.repeatRule.isNullOrBlank()
    } ?: false
    val isDone = task.isCompleted || task.isAbandoned

    // Start time comes from the task's own dueDate (even when overrideDate
    // shifts the shown day for a recurrence). Midnight is treated as "no
    // time" — the subtitle then just reads the day label.
    val startTime = task.dueDate?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())?.toLocalTime()
    val hasTime = startTime != null && startTime != LocalTime.MIDNIGHT

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val checkboxShape = RoundedCornerShape(6.dp)
        // 외곽 44dp clickable Box로 터치 타겟을 확대. 24dp 시각 체크박스는
        // 가운데에 배치. SwipeableTaskItem이 부모에서 detectHorizontalDragGestures
        // 를 걸어 두기 때문에 작은 24dp 영역만 clickable이면 미세 움직임에
        // drag detector가 이벤트를 가져가버려 탭이 안 먹힘. pointerInput +
        // detectTapGestures로 tap을 명시적으로 consume해 race 차단.
        Box(
            modifier = Modifier
                .size(44.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onToggleComplete() })
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(checkboxShape)
                    .then(
                        when {
                            task.isCompleted -> Modifier.background(TextTertiary)
                            task.isAbandoned -> Modifier.border(2.dp, AbandonedBlue, checkboxShape)
                            else -> Modifier.border(2.dp, TextSecondary, checkboxShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    task.isCompleted -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    task.isAbandoned -> Icon(
                        Icons.Default.Close,
                        contentDescription = "Abandoned",
                        tint = AbandonedBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            MarkdownText(
                text = task.title,
                color = if (isDone) TextSecondary else MaterialTheme.colorScheme.onBackground,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.notes.isNotBlank()) {
                MarkdownText(
                    text = task.notes,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (resolvedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.size(2.dp))
                FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                ) {
                    resolvedTags.forEach { tag ->
                        val chipColor = runCatching {
                            Color(android.graphics.Color.parseColor(tag.color))
                        }.getOrDefault(Orange)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipColor.copy(alpha = 0.32f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag.name,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            shownDate?.let { date ->
                val subtitle = buildSubtitle(date, startTime, task.durationMinutes, hasTime)
                val subtitleColor = when {
                    isDone -> TextTertiary
                    isOverdue -> OverdueRed
                    else -> SubtitleAccent
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = subtitleColor
                    )
                    if (!task.repeatRule.isNullOrBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "반복",
                            tint = subtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (hasTime) {
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "알림",
                            tint = subtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = listName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDone) TextTertiary else TextSecondary
        )
    }
}

private fun buildSubtitle(
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
        else -> "${date.monthValue}월 ${date.dayOfMonth}일"
    }
    if (!hasTime || startTime == null) return dayLabel
    val endTime = startTime.plusMinutes(durationMinutes.toLong())
    return "$dayLabel, ${formatKorean12h(startTime)} - ${formatKorean12h(endTime)}"
}

private fun formatKorean12h(time: LocalTime): String {
    val ampm = if (time.hour < 12) "오전" else "오후"
    val h = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    return "%s %d:%02d".format(ampm, h, time.minute)
}
