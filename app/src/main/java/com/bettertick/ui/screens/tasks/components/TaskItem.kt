package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.LEAVE_TYPES
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.leaveOn
import com.bettertick.data.model.normalizeShiftLabel
import com.bettertick.data.model.shiftDay
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
@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    resolvedTags: List<Tag> = emptyList(),
    /** 목록 ⋮ > 세부 정보 표시 — notes preview + tag pills. */
    showDetails: Boolean = com.bettertick.ui.screens.tasks.TaskListPrefs.showDetails,
    /** Long-press — enters multi-select in the task list. */
    onLongClick: (() -> Unit)? = null,
    /** 근무 할일의 병가 / 지휴 토글 (day, type). Null shows the used leave read-only. */
    onToggleLeave: ((LocalDate, String) -> Unit)? = null
) {
    val isShift = task.isShiftTask()
    val shiftDay = if (isShift) task.shiftDay(overrideDate) else null
    val leave = if (isShift) task.leaveOn(shiftDay) else null
    val shownDate = overrideDate ?: task.dueDate?.toLocalDate()
    // Recurring tasks aren't really "overdue" — even when their stored
    // due date is in the past, the rule keeps producing fresh occurrences.
    val isOverdue = shownDate?.let {
        it.isBefore(LocalDate.now()) && !task.isCompleted && !task.isAbandoned &&
            task.repeatRule.isNullOrBlank() && !task.isShiftTask()
    } ?: false
    val isDone = task.isCompleted || task.isAbandoned

    // Start time comes from the task's own dueDate (even when overrideDate
    // shifts the shown day for a recurrence). Midnight = "no time".
    val startTime = task.dueDate?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())?.toLocalTime()
    val hasTime = startTime != null && startTime != LocalTime.MIDNIGHT

    val accent = ThemeState.accentColor
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 44dp touch target keeps the parent drag detector from stealing the
        // tap; the visible box is TickTick's 18dp rounded square.
        if (isShift) {
            // 근무 일정 — no completion checkbox, just the shift badge.
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                ShiftBadge(normalizeShiftLabel(task.title), leave)
            }
        } else Box(
            modifier = Modifier
                .size(44.dp)
                .clickable {
                    if (!task.isCompleted && !task.isAbandoned) {
                        com.bettertick.ui.components.CompletionFeedback.play(context, haptic)
                    }
                    onToggleComplete()
                },
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
            if (isShift && (onToggleLeave != null || leave != null)) {
                Spacer(Modifier.height(4.dp))
                LeaveChipRow(
                    day = shiftDay,
                    used = leave,
                    onToggle = if (onToggleLeave != null && shiftDay != null) {
                        { type -> onToggleLeave(shiftDay, type) }
                    } else null
                )
            }
            if (showDetails && task.notes.isNotBlank()) {
                MarkdownText(
                    text = task.notes,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showDetails && resolvedTags.isNotEmpty()) {
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

        if (shownDate != null || !task.repeatRule.isNullOrBlank() || task.isPinned) {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isPinned) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "고정됨",
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
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
    val p = com.bettertick.ui.theme.AppPrefs
    val start = p.formatTime(startTime)
    val time = if (durationMinutes > 0) {
        val end = startTime.plusMinutes(durationMinutes.toLong())
        val sameHalf = (startTime.hour < 12) == (end.hour < 12)
        "$start - ${if (sameHalf && !p.is24h) formatClock(end) else p.formatTime(end)}"
    } else start
    return if (date == today) time else "$dayLabel, $time"
}


private fun formatClock(time: LocalTime): String {
    val h = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    return "%d:%02d".format(h, time.minute)
}


/** Color of a leave kind (병가 red, 지휴 purple). */
fun leaveColor(type: String): Color = when (type) {
    "병가" -> Color(0xFFE5483F)
    "연차" -> Color(0xFF35B36A)
    else -> Color(0xFF8E6CEF)
}

/**
 * 병가 / 지휴 / 연차 chips for a shift on [day], each with the days left in
 * its allowance (설정 > 일반 > 휴가 일수). Read-only ([onToggle] null) shows
 * only the leave that was used.
 */
@Composable
fun LeaveChipRow(
    day: LocalDate?,
    used: String?,
    onToggle: ((String) -> Unit)?,
    /** Unsaved edit (detail sheet): the leave the saved task has on [day]. */
    savedUsed: String? = used
) {
    // One straight line (scrolls if the row is narrow) with short labels.
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LEAVE_TYPES.forEach { type ->
            if (onToggle != null || used == type) {
                val left = day?.let {
                    val extra = (if (used == type) 1 else 0) - (if (savedUsed == type) 1 else 0)
                    com.bettertick.ui.screens.tasks.LeavePrefs.remaining(type, it, extra)
                }
                LeaveChip(
                    label = type,
                    selected = used == type,
                    remaining = left?.let { if (it >= 0) "${it}일" else "${-it}일 초과" },
                    onClick = onToggle?.let { { it(type) } }
                )
            }
        }
    }
}

/**
 * Detail-sheet version: 지휴 · 병가 · 연차 as three equal-width buttons in
 * one row, each with the days left underneath.
 */
@Composable
fun LeaveButtonsRow(
    day: LocalDate?,
    used: String?,
    savedUsed: String?,
    onToggle: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LEAVE_TYPES.forEach { type ->
            val selected = used == type
            val c = leaveColor(type)
            val left = day?.let {
                val extra = (if (selected) 1 else 0) - (if (savedUsed == type) 1 else 0)
                com.bettertick.ui.screens.tasks.LeavePrefs.remaining(type, it, extra)
            }
            val shape = RoundedCornerShape(12.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .then(if (selected) Modifier.background(c) else Modifier.border(1.dp, TextTertiary.copy(alpha = 0.5f), shape))
                    .then(if (onToggle != null) Modifier.clickable { onToggle(type) } else Modifier)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (selected) "$type ✓" else type,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else c
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        left == null -> if (selected) "사용" else "일수 미설정"
                        left >= 0 -> "${left}일 남음"
                        else -> "${-left}일 초과"
                    },
                    fontSize = 12.sp,
                    color = when {
                        selected -> Color.White.copy(alpha = 0.9f)
                        left != null && left < 0 -> OverdueRed
                        else -> TextSecondary
                    }
                )
            }
        }
    }
}

/** Round badge with the shift label (주 / 야 / 비 / 휴); tinted by the leave used. */
@Composable
fun ShiftBadge(label: String, leave: String?) {
    val tint = leave?.let { leaveColor(it) } ?: ThemeState.accentColor
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(label.take(2), fontSize = if (label.length > 1) 10.sp else 12.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

/** 병가 / 지휴 toggle pill. Filled when that leave was used on the day. */
@Composable
fun LeaveChip(label: String, selected: Boolean, onClick: (() -> Unit)?, remaining: String? = null) {
    val c = leaveColor(label)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(c) else Modifier.border(1.dp, TextTertiary.copy(alpha = 0.6f), shape))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        val over = remaining?.endsWith("초과") == true
        Text(
            text = buildString {
                append(label)
                if (selected) append(" ✓")
                if (remaining != null) append(" ").append(remaining)
            },
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                selected -> Color.White
                over -> OverdueRed
                else -> TextSecondary
            }
        )
    }
}
