package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * Full date picker reachable from the quick-pick sheet's "날짜 선택" tile.
 * View-only for now — selection state is internal; callers get the final
 * result through [onConfirm].
 *
 * Rendered as a centered [Dialog] to match the reference. A bottom sheet
 * felt wrong for this surface because the calendar grid plus the options
 * list plus the footer buttons pushed the important action buttons to the
 * bottom of the screen where they competed with the nav bar.
 *
 * Layout:
 *  [날짜 | 지속 시간]  tab row
 *  month label + < > nav
 *  7-column date grid
 *  options list (시간 / 알림 / 반복)
 *  footer:  삭제 (left)      취소  확인 (right)
 */
@Composable
fun TaskDatePickerSheet(
    initialDate: LocalDate = LocalDate.now(),
    initialTime: LocalTime? = null,
    initialDurationMinutes: Int = 60,
    initialRepeat: RepeatChoice = RepeatChoice.None,
    initialRepeatEnd: RepeatEnd = RepeatEnd.Never,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (LocalDate, LocalTime?, Int, RepeatChoice, RepeatEnd) -> Unit
) {
    var selectedTab by remember { mutableStateOf(DateTab.Date) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    // Time / alarm / repeat state — seeded from the task's persisted values
    // so reopening the picker shows what's already saved. Selecting a time
    // auto-populates a default alarm of 15분 전 so users don't have to
    // configure it separately for the common case.
    var selectedTime by remember { mutableStateOf(initialTime) }
    var alarmChoice by remember {
        mutableStateOf<AlarmChoice>(
            if (initialTime != null) AlarmChoice.Preset(15) else AlarmChoice.None
        )
    }
    var persistentAlarm by remember { mutableStateOf(false) }
    var repeatChoice by remember { mutableStateOf(initialRepeat) }
    var repeatEnd by remember { mutableStateOf(initialRepeatEnd) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAlarmPicker by remember { mutableStateOf(false) }
    var showCustomAlarmPicker by remember { mutableStateOf(false) }
    var showRepeatPicker by remember { mutableStateOf(false) }
    var showCustomRepeat by remember { mutableStateOf(false) }
    var showRepeatEndPicker by remember { mutableStateOf(false) }

    // Duration-tab state — seed start from the task's saved time (falling
    // back to 오후 2시) and end from start + saved duration. Lives alongside
    // date state so switching tabs preserves the user's in-progress picks.
    val seedTime = initialTime ?: LocalTime.of(14, 0)
    val seedDuration = if (initialDurationMinutes > 0) initialDurationMinutes else 60
    var durationStart by remember {
        mutableStateOf(LocalDateTime.of(initialDate, seedTime))
    }
    var durationEnd by remember {
        mutableStateOf(LocalDateTime.of(initialDate, seedTime).plusMinutes(seedDuration.toLong()))
    }
    var durationAllDay by remember { mutableStateOf(false) }
    // Lifted from DurationContent so the TabRow + Footer below can
    // collapse while the wheel-picker (시작/끝) view is active. Otherwise
    // 날짜/지속시간 tabs + 삭제/취소/확인 bleed through the advanced UI.
    var durationAdvanced by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            if (!durationAdvanced) {
                TabRow(
                    selected = selectedTab,
                    onSelect = { selectedTab = it }
                )

                Spacer(Modifier.height(16.dp))
            }

            if (selectedTab == DateTab.Date) {
                MonthNav(
                    month = visibleMonth,
                    onPrev = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNext = { visibleMonth = visibleMonth.plusMonths(1) }
                )

                Spacer(Modifier.height(12.dp))

                DayOfWeekHeader()

                Spacer(Modifier.height(4.dp))

                DateGrid(
                    month = visibleMonth,
                    selected = selectedDate,
                    onSelect = { selectedDate = it }
                )

                Spacer(Modifier.height(20.dp))

                OptionRows(
                    timeLabel = selectedTime?.let { formatKoreanTime(it) },
                    alarmLabel = if (alarmChoice is AlarmChoice.None) null else alarmChoice.displayLabel(),
                    repeatLabel = if (repeatChoice is RepeatChoice.None) null else repeatChoice.displayLabel(selectedDate),
                    repeatEndLabel = if (repeatEnd is RepeatEnd.Never) null else repeatEnd.displayLabel(),
                    onTimeClick = { showTimePicker = true },
                    onAlarmClick = { showAlarmPicker = true },
                    onRepeatClick = { showRepeatPicker = true },
                    onRepeatEndClick = { showRepeatEndPicker = true },
                    onTimeClear = {
                        selectedTime = null
                        alarmChoice = AlarmChoice.None
                        persistentAlarm = false
                    },
                    onAlarmClear = {
                        alarmChoice = AlarmChoice.None
                        persistentAlarm = false
                    },
                    onRepeatClear = {
                        repeatChoice = RepeatChoice.None
                        repeatEnd = RepeatEnd.Never
                    },
                    onRepeatEndClear = { repeatEnd = RepeatEnd.Never }
                )
            } else {
                DurationContent(
                    startDateTime = durationStart,
                    endDateTime = durationEnd,
                    isAllDay = durationAllDay,
                    onStartChange = { durationStart = it },
                    onEndChange = { durationEnd = it },
                    onAllDayChange = { durationAllDay = it },
                    alarmChoice = alarmChoice,
                    repeatChoice = repeatChoice,
                    repeatEnd = repeatEnd,
                    onAlarmClick = { showAlarmPicker = true },
                    onRepeatClick = { showRepeatPicker = true },
                    onRepeatEndClick = { showRepeatEndPicker = true },
                    onAlarmClear = {
                        alarmChoice = AlarmChoice.None
                        persistentAlarm = false
                    },
                    onRepeatClear = {
                        repeatChoice = RepeatChoice.None
                        repeatEnd = RepeatEnd.Never
                    },
                    onRepeatEndClear = { repeatEnd = RepeatEnd.Never },
                    advanced = durationAdvanced,
                    onAdvancedChange = { durationAdvanced = it }
                )
            }

            if (!durationAdvanced) {
                Spacer(Modifier.height(16.dp))
            }

            if (!durationAdvanced) Footer(
                onDelete = onDelete,
                onCancel = onDismiss,
                onConfirm = {
                    // The Duration tab is the source of truth when it's the
                    // active tab — its start/end round-trip as (time,
                    // durationMinutes). The Date tab only owns a single
                    // point in time, so duration falls back to the initial
                    // value (or 60 min) to avoid silently zeroing it out.
                    if (selectedTab == DateTab.Date) {
                        onConfirm(selectedDate, selectedTime, seedDuration, repeatChoice, repeatEnd)
                    } else {
                        val duration = java.time.Duration
                            .between(durationStart, durationEnd)
                            .toMinutes()
                            .toInt()
                            .coerceAtLeast(1)
                        val time = if (durationAllDay) null else durationStart.toLocalTime()
                        onConfirm(
                            durationStart.toLocalDate(),
                            time,
                            duration,
                            repeatChoice,
                            repeatEnd
                        )
                    }
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showTimePicker) {
        TaskTimePickerDialog(
            initialHour = selectedTime?.hour ?: 9,
            initialMinute = selectedTime?.minute ?: 0,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                selectedTime = LocalTime.of(h, m)
                // Default to 15분 전 when the user hasn't explicitly set an
                // alarm. This matches the reference app's default: most
                // users want a short heads-up rather than a fire-at-time
                // alarm. Cleared via the X on the 알림 row.
                if (alarmChoice is AlarmChoice.None) alarmChoice = AlarmChoice.Preset(15)
                showTimePicker = false
            }
        )
    }

    if (showAlarmPicker) {
        AlarmPickerDialog(
            current = alarmChoice,
            persistent = persistentAlarm,
            onDismiss = { showAlarmPicker = false },
            onPresetSelected = {
                alarmChoice = it
                showAlarmPicker = false
            },
            onCustomRequested = {
                // Hand off to the custom wheel dialog. Keep AlarmPicker
                // dismissed so it's not stacked behind; the user returns
                // straight to the date sheet on confirm/cancel.
                showAlarmPicker = false
                showCustomAlarmPicker = true
            },
            onPersistentChange = { persistentAlarm = it }
        )
    }

    if (showCustomAlarmPicker) {
        val existing = alarmChoice as? AlarmChoice.Custom
        CustomAlarmDialog(
            initialDays = existing?.days ?: 0,
            initialHours = existing?.hours ?: 0,
            initialMinutes = existing?.minutes ?: 15,
            onDismiss = { showCustomAlarmPicker = false },
            onConfirm = { d, h, m ->
                alarmChoice = AlarmChoice.Custom(d, h, m)
                showCustomAlarmPicker = false
            }
        )
    }

    if (showRepeatPicker) {
        RepeatPickerDialog(
            current = repeatChoice,
            referenceDate = selectedDate,
            onDismiss = { showRepeatPicker = false },
            onPresetSelected = {
                repeatChoice = it
                showRepeatPicker = false
            },
            onCustomRequested = {
                // Swap the preset list for the fullscreen editor. Only
                // one of these is visible at a time by design.
                showRepeatPicker = false
                showCustomRepeat = true
            }
        )
    }

    if (showCustomRepeat) {
        CustomRepeatDialog(
            initial = repeatChoice as? RepeatChoice.Custom,
            referenceDate = selectedDate,
            onDismiss = { showCustomRepeat = false },
            onConfirm = { custom ->
                repeatChoice = custom
                showCustomRepeat = false
            }
        )
    }

    if (showRepeatEndPicker) {
        RepeatEndDialog(
            initial = repeatEnd,
            referenceDate = if (selectedTab == DateTab.Date) selectedDate else durationStart.toLocalDate(),
            onDismiss = { showRepeatEndPicker = false },
            onConfirm = {
                repeatEnd = it
                showRepeatEndPicker = false
            }
        )
    }
}

@Composable
private fun TabRow(
    selected: DateTab,
    onSelect: (DateTab) -> Unit
) {
    Row {
        DateTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(end = 20.dp)
            ) {
                Text(
                    text = tab.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) DatePickerAccent else TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 28.dp else 0.dp)
                        .background(DatePickerAccent)
                )
            }
        }
    }
}

@Composable
private fun MonthNav(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${month.monthValue}월",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onPrev) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = TextSecondary
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateGrid(
    month: YearMonth,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7 // Sun=0
    val daysInMonth = month.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val weeks = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        for (w in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (d in 0..6) {
                    val cellIdx = w * 7 + d
                    val dayNum = cellIdx - startOffset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = month.atDay(dayNum)
                            DateCell(
                                day = dayNum,
                                selected = date == selected,
                                onClick = { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    day: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(DatePickerAccent) else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun OptionRows(
    timeLabel: String?,
    alarmLabel: String?,
    repeatLabel: String?,
    repeatEndLabel: String?,
    onTimeClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onRepeatEndClick: () -> Unit,
    onTimeClear: () -> Unit,
    onAlarmClear: () -> Unit,
    onRepeatClear: () -> Unit,
    onRepeatEndClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
    ) {
        OptionRow(
            icon = Icons.Outlined.AccessTime,
            label = "시간",
            value = timeLabel ?: "없음",
            clearable = timeLabel != null,
            onClick = onTimeClick,
            onClear = onTimeClear
        )
        OptionRow(
            icon = Icons.Outlined.NotificationsNone,
            label = "알림",
            value = alarmLabel ?: "없음",
            clearable = alarmLabel != null,
            onClick = onAlarmClick,
            onClear = onAlarmClear
        )
        OptionRow(
            icon = Icons.Outlined.Refresh,
            label = "반복",
            value = repeatLabel ?: "없음",
            clearable = repeatLabel != null,
            onClick = onRepeatClick,
            onClear = onRepeatClear
        )
        // 반복 종료 only makes sense once a 반복 rule is set — otherwise the
        // end condition has nothing to bound. Hide the row entirely when
        // repeat is 없음 to keep the list tight.
        if (repeatLabel != null) {
            OptionRow(
                icon = Icons.Outlined.EventBusy,
                label = "반복 종료",
                value = repeatEndLabel ?: "없음",
                clearable = repeatEndLabel != null,
                onClick = onRepeatEndClick,
                onClear = onRepeatEndClear
            )
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    value: String,
    clearable: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextSecondary
        )
        Spacer(Modifier.width(6.dp))
        if (clearable) {
            // Tap target is a separate Box so clearing doesn't also fire
            // the row's onClick (which would re-open the picker).
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** "오후 3:00" — 12-hour Korean formatter used in the date-picker's 시간 row. */
private fun formatKoreanTime(time: LocalTime): String {
    val ampm = if (time.hour < 12) "오전" else "오후"
    val hour12 = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    return "%s %d:%02d".format(ampm, hour12, time.minute)
}

@Composable
private fun Footer(
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "삭제",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = DatePickerAccent,
            modifier = Modifier
                .clickable { onDelete() }
                .padding(vertical = 8.dp)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "취소",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = DatePickerAccent,
            modifier = Modifier
                .clickable { onCancel() }
                .padding(vertical = 8.dp, horizontal = 12.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "확인",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DatePickerAccent,
            modifier = Modifier
                .clickable { onConfirm() }
                .padding(vertical = 8.dp, horizontal = 12.dp)
        )
    }
}

private enum class DateTab(val label: String) {
    Date("날짜"),
    Duration("지속 시간")
}

private val DatePickerAccent = Orange
