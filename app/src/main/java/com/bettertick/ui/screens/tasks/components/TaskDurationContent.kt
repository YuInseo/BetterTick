package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val DurationAccent = Color(0xFF4A90E2)

/**
 * 지속 시간 tab content inside the task date/duration picker. Handles four
 * visual modes from one state:
 *  - Simple + timed: date card and time card with "지속 시간: N hrs" label.
 *  - Simple + all-day: two date cards with "N 일" badge; time/alarm/repeat
 *    rows collapse because an all-day event has no sub-day alarms.
 *  - Advanced: 시작/끝 sub-tabs with wheel pickers for date + 오전/오후 + hour
 *    + minute. Mirrors the reference app's fine-grained control path.
 *  - Timeline sheet: opens from the time card. A draggable blue block
 *    represents the range visually — the edges report back as the new
 *    start/end times.
 */
@Composable
fun DurationContent(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    isAllDay: Boolean,
    onStartChange: (LocalDateTime) -> Unit,
    onEndChange: (LocalDateTime) -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    alarmChoice: AlarmChoice,
    repeatChoice: RepeatChoice,
    repeatEnd: RepeatEnd,
    onAlarmClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onRepeatEndClick: () -> Unit,
    onAlarmClear: () -> Unit,
    onRepeatClear: () -> Unit,
    onRepeatEndClear: () -> Unit,
    // Lifted so the parent sheet can hide its own tab row / footer chrome
    // while the wheel-picker advanced view is open — otherwise 날짜/지속시간
    // tabs bleed through above the 시작/끝 advanced UI.
    advanced: Boolean,
    onAdvancedChange: (Boolean) -> Unit
) {
    var showTimeline by remember { mutableStateOf(false) }

    if (advanced) {
        AdvancedDurationView(
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            onStartChange = onStartChange,
            onEndChange = onEndChange,
            onBackToSimple = { onAdvancedChange(false) }
        )
    } else {
        SimpleDurationView(
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            isAllDay = isAllDay,
            onAllDayChange = {
                onAllDayChange(it)
                if (it) {
                    // All-day events have no sub-day alarm/repeat meaning;
                    // reference app resets both to 없음 when the user opts in.
                    if (alarmChoice !is AlarmChoice.None) onAlarmClear()
                    if (repeatChoice !is RepeatChoice.None) onRepeatClear()
                    if (repeatEnd !is RepeatEnd.Never) onRepeatEndClear()
                }
            },
            alarmChoice = alarmChoice,
            repeatChoice = repeatChoice,
            repeatEnd = repeatEnd,
            onTimeCardClick = { if (!isAllDay) showTimeline = true },
            onAlarmClick = onAlarmClick,
            onRepeatClick = onRepeatClick,
            onRepeatEndClick = onRepeatEndClick,
            onAlarmClear = onAlarmClear,
            onRepeatClear = onRepeatClear,
            onRepeatEndClear = onRepeatEndClear,
            onSwitchToAdvanced = { onAdvancedChange(true) }
        )
    }

    if (showTimeline) {
        DurationTimelineSheet(
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            onDismiss = { showTimeline = false },
            onSwitchToAdvanced = {
                showTimeline = false
                onAdvancedChange(true)
            },
            onConfirm = { start, end ->
                onStartChange(start)
                onEndChange(end)
                showTimeline = false
            }
        )
    }
}

// ───────────────────────── Simple view ─────────────────────────

@Composable
private fun SimpleDurationView(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    isAllDay: Boolean,
    onAllDayChange: (Boolean) -> Unit,
    alarmChoice: AlarmChoice,
    repeatChoice: RepeatChoice,
    repeatEnd: RepeatEnd,
    onTimeCardClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onRepeatEndClick: () -> Unit,
    onAlarmClear: () -> Unit,
    onRepeatClear: () -> Unit,
    onRepeatEndClear: () -> Unit,
    onSwitchToAdvanced: () -> Unit
) {
    Column {
        if (isAllDay) {
            AllDayCardsRow(
                startDate = startDateTime.toLocalDate(),
                endDate = endDateTime.toLocalDate()
            )
        } else {
            DateTimeCardsRow(
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                onTimeCardClick = onTimeCardClick
            )
        }

        Spacer(Modifier.height(12.dp))

        AllDayToggleRow(
            isAllDay = isAllDay,
            onChange = onAllDayChange
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
        ) {
            DurationOptionRow(
                icon = Icons.Outlined.NotificationsNone,
                label = "알림",
                value = if (isAllDay || alarmChoice is AlarmChoice.None) "없음" else alarmChoice.displayLabel(),
                clearable = !isAllDay && alarmChoice !is AlarmChoice.None,
                onClick = { if (!isAllDay) onAlarmClick() },
                onClear = onAlarmClear
            )
            DurationOptionRow(
                icon = Icons.Outlined.Refresh,
                label = "반복",
                value = if (isAllDay || repeatChoice is RepeatChoice.None) "없음" else repeatChoice.displayLabel(startDateTime.toLocalDate()),
                clearable = !isAllDay && repeatChoice !is RepeatChoice.None,
                onClick = { if (!isAllDay) onRepeatClick() },
                onClear = onRepeatClear
            )
            // Only surface 반복 종료 when a 반복 is active — matches the
            // date-tab's behavior and avoids showing an end condition with
            // no recurrence to terminate.
            if (!isAllDay && repeatChoice !is RepeatChoice.None) {
                DurationOptionRow(
                    icon = Icons.Outlined.EventBusy,
                    label = "반복 종료",
                    value = if (repeatEnd is RepeatEnd.Never) "없음" else repeatEnd.displayLabel(),
                    clearable = repeatEnd !is RepeatEnd.Never,
                    onClick = onRepeatEndClick,
                    onClear = onRepeatEndClear
                )
            }
        }

    }
}

@Composable
private fun DateTimeCardsRow(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onTimeCardClick: () -> Unit
) {
    val hours = java.time.Duration.between(startDateTime, endDateTime).toHours()
    val safeHours = if (hours <= 0) 1 else hours
    Row(modifier = Modifier.fillMaxWidth()) {
        DurationCard(
            primary = formatKoreanDateWithDay(startDateTime.toLocalDate()),
            modifier = Modifier
                .weight(1f)
                .height(92.dp)
        )
        Spacer(Modifier.width(10.dp))
        DurationCard(
            primary = "${formatKoreanTime(startDateTime.toLocalTime())} - ${formatKoreanTime(endDateTime.toLocalTime())}",
            secondary = "지속 시간: ${safeHours} hrs",
            onClick = onTimeCardClick,
            modifier = Modifier
                .weight(1.4f)
                .height(92.dp)
        )
    }
}

@Composable
private fun AllDayCardsRow(
    startDate: LocalDate,
    endDate: LocalDate
) {
    val days = (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1)
    Row(modifier = Modifier.fillMaxWidth()) {
        DurationCard(
            primary = formatKoreanDateWithDay(startDate),
            modifier = Modifier
                .weight(1f)
                .height(78.dp)
        )
        Spacer(Modifier.width(10.dp))
        DurationCard(
            primary = formatKoreanDateWithDay(endDate),
            secondary = "${days} 일",
            modifier = Modifier
                .weight(1f)
                .height(78.dp)
        )
    }
}

@Composable
private fun DurationCard(
    primary: String,
    secondary: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val base = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(DarkSurfaceVariant)
    Column(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        if (secondary != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = secondary,
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
private fun AllDayToggleRow(
    isAllDay: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
            .clickable { onChange(!isAllDay) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "하루 종일",
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isAllDay,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DurationAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3A3A3A)
            )
        )
    }
}

@Composable
private fun DurationOptionRow(
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
        if (clearable) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "×", fontSize = 18.sp, color = TextTertiary)
            }
        }
    }
}

// ───────────────────────── Advanced wheel view ─────────────────────────

@Composable
private fun AdvancedDurationView(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onStartChange: (LocalDateTime) -> Unit,
    onEndChange: (LocalDateTime) -> Unit,
    onBackToSimple: () -> Unit
) {
    var editingStart by remember { mutableStateOf(true) }

    val target = if (editingStart) startDateTime else endDateTime
    val onTargetChange: (LocalDateTime) -> Unit =
        if (editingStart) onStartChange else onEndChange

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StartEndTab(label = "시작", selected = editingStart) { editingStart = true }
            Spacer(Modifier.width(20.dp))
            StartEndTab(label = "끝", selected = !editingStart) { editingStart = false }
        }

        Spacer(Modifier.height(18.dp))

        WheelDateTimeRow(
            value = target,
            onValueChange = onTargetChange
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "보통",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DurationAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBackToSimple() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun StartEndTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) DurationAccent else TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (selected) 28.dp else 0.dp)
                .background(DurationAccent)
        )
    }
}

@Composable
private fun WheelDateTimeRow(
    value: LocalDateTime,
    onValueChange: (LocalDateTime) -> Unit
) {
    // Build a dense date list centered on the current value: ±60 days is
    // enough for every normal range a user configures without making the
    // wheel unusably long.
    val anchor = value.toLocalDate()
    val dateOptions = remember(anchor) {
        (-60..60).map { anchor.plusDays(it.toLong()) }
    }
    val dateLabels = remember(dateOptions) {
        dateOptions.map { formatKoreanDateWithDay(it) }
    }
    val ampmLabels = listOf("오전", "오후")
    val hourLabels = (1..12).map { it.toString() }
    val minuteLabels = (0..59).map { "%02d".format(it) }

    val dateIndex = 60
    val isPm = value.hour >= 12
    val hour12 = when {
        value.hour == 0 -> 12
        value.hour > 12 -> value.hour - 12
        else -> value.hour
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1.6f)) {
            WheelPicker(
                items = dateLabels,
                selectedIndex = dateIndex,
                onSelectedIndexChange = { idx ->
                    val newDate = dateOptions[idx]
                    onValueChange(value.withYear(newDate.year).withMonth(newDate.monthValue).withDayOfMonth(newDate.dayOfMonth))
                }
            )
        }
        Box(modifier = Modifier.weight(0.9f)) {
            WheelPicker(
                items = ampmLabels,
                selectedIndex = if (isPm) 1 else 0,
                onSelectedIndexChange = { idx ->
                    val base = hour12 % 12
                    val newHour = if (idx == 1) base + 12 else base
                    onValueChange(value.withHour(newHour))
                }
            )
        }
        Box(modifier = Modifier.weight(0.8f)) {
            WheelPicker(
                items = hourLabels,
                selectedIndex = hour12 - 1,
                onSelectedIndexChange = { idx ->
                    val h12 = idx + 1
                    val newHour = when {
                        isPm && h12 == 12 -> 12
                        isPm -> h12 + 12
                        !isPm && h12 == 12 -> 0
                        else -> h12
                    }
                    onValueChange(value.withHour(newHour))
                }
            )
        }
        Box(modifier = Modifier.weight(0.8f)) {
            WheelPicker(
                items = minuteLabels,
                selectedIndex = value.minute,
                onSelectedIndexChange = { idx ->
                    onValueChange(value.withMinute(idx))
                }
            )
        }
    }
}

// ───────────────────────── Timeline bottom sheet ─────────────────────────

/**
 * Scrollable 27-hour timeline (오전 12 → next day 오전 3) matching Samsung's
 * duration picker. Each hour is [HOUR_HEIGHT_DP] tall so only a handful of
 * rows are visible at once — the user scrolls through a long column rather
 * than seeing the whole day compressed. The selected range is a blue block
 * with corner handles:
 *  - Top-left circle: drag to adjust start (end fixed).
 *  - Bottom-right circle: drag to adjust end (start fixed).
 *  - Body: drag to move the whole range.
 * All drags quantise to 15-minute increments so the touch target stays
 * forgiving. The current time is marked with a red line when today falls
 * inside the visible 27-hour window.
 */
private const val HOUR_HEIGHT_DP = 72

@Composable
private fun DurationTimelineSheet(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onDismiss: () -> Unit,
    onSwitchToAdvanced: () -> Unit,
    onConfirm: (LocalDateTime, LocalDateTime) -> Unit
) {
    val density = LocalDensity.current
    val baseDate = remember(startDateTime) { startDateTime.toLocalDate() }
    val totalHours = 27  // 오전 12 → next day 오전 3
    val totalMinutes = totalHours * 60

    val hourHeight = HOUR_HEIGHT_DP.dp
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val pxPerMinute = hourHeightPx / 60f

    val initialStart = minutesFromBase(baseDate, startDateTime).coerceIn(0, totalMinutes)
    val initialEnd = minutesFromBase(baseDate, endDateTime).coerceIn(initialStart + 15, totalMinutes)

    var startMin by remember { mutableStateOf(initialStart) }
    var endMin by remember { mutableStateOf(initialEnd) }

    val scrollState = rememberScrollState()
    // Scroll so the start edge sits about one hour below the viewport top —
    // gives the user visual breathing room above the block on open.
    LaunchedEffect(Unit) {
        val target = (startMin * pxPerMinute - hourHeightPx).roundToInt().coerceAtLeast(0)
        scrollState.scrollTo(target)
    }

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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = formatDurationHeader(baseDate),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight * (totalHours + 1))
                ) {
                    TimelineHourGrid(
                        baseDate = baseDate,
                        totalHours = totalHours,
                        hourHeight = hourHeight
                    )

                    CurrentTimeIndicator(
                        baseDate = baseDate,
                        totalMinutes = totalMinutes,
                        pxPerMinute = pxPerMinute
                    )

                    TimelineRangeBlock(
                        baseDate = baseDate,
                        startMin = startMin,
                        endMin = endMin,
                        pxPerMinute = pxPerMinute,
                        totalMinutes = totalMinutes,
                        onRangeChange = { s, e ->
                            startMin = s
                            endMin = e
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "고급",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DurationAccent,
                    modifier = Modifier
                        .clickable {
                            // Commit the current drag block as start/end
                            // before opening the wheel picker so the
                            // advanced view reflects what the user sees.
                            onConfirm(
                                baseDate.atStartOfDay().plusMinutes(startMin.toLong()),
                                baseDate.atStartOfDay().plusMinutes(endMin.toLong())
                            )
                            onSwitchToAdvanced()
                        }
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DurationAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "확인",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DurationAccent,
                    modifier = Modifier
                        .clickable {
                            onConfirm(
                                baseDate.atStartOfDay().plusMinutes(startMin.toLong()),
                                baseDate.atStartOfDay().plusMinutes(endMin.toLong())
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineHourGrid(
    baseDate: LocalDate,
    totalHours: Int,
    hourHeight: androidx.compose.ui.unit.Dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        for (h in 0..totalHours) {
            val hourInDay = h % 24
            val displayDate = baseDate.plusDays((h / 24).toLong())
            val showDateMarker = h > 0 && hourInDay == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.width(70.dp)) {
                    if (showDateMarker) {
                        Text(
                            text = formatMonthDay(displayDate),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Orange
                        )
                    }
                    Text(
                        text = formatKoreanHour(hourInDay),
                        fontSize = 14.sp,
                        color = TextTertiary,
                        modifier = Modifier.offset(y = (-6).dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = 4.dp)
                ) {
                    // Faint gridline anchored to the hour label baseline.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentTimeIndicator(
    baseDate: LocalDate,
    totalMinutes: Int,
    pxPerMinute: Float
) {
    val now = remember { LocalDateTime.now() }
    val minutesOffset = minutesFromBase(baseDate, now)
    if (minutesOffset !in 0..totalMinutes) return

    val density = LocalDensity.current
    val yPx = minutesOffset * pxPerMinute
    Box(
        modifier = Modifier
            .offset { IntOffset(with(density) { 70.dp.toPx() }.roundToInt(), yPx.roundToInt()) }
            .fillMaxWidth()
            .height(2.dp)
            .background(OverdueRed)
    )
    Box(
        modifier = Modifier
            .offset { IntOffset(with(density) { 66.dp.toPx() }.roundToInt(), (yPx - with(density) { 4.dp.toPx() }).roundToInt()) }
            .size(10.dp)
            .clip(CircleShape)
            .background(OverdueRed)
    )
}

@Composable
private fun TimelineRangeBlock(
    baseDate: LocalDate,
    startMin: Int,
    endMin: Int,
    pxPerMinute: Float,
    totalMinutes: Int,
    onRangeChange: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val leftInset = 70.dp
    val rightInset = 8.dp
    val handleSize = 18.dp
    val handleSizePx = with(density) { handleSize.toPx() }

    val topPx = startMin * pxPerMinute
    val heightPx = (endMin - startMin) * pxPerMinute
    val heightDp = with(density) { heightPx.toDp() }

    val startTime = baseDate.atStartOfDay().plusMinutes(startMin.toLong()).toLocalTime()
    val endTime = baseDate.atStartOfDay().plusMinutes(endMin.toLong()).toLocalTime()
    val hours = (endMin - startMin) / 60
    val minutes = (endMin - startMin) % 60
    val durationLabel = if (minutes == 0) "$hours hrs" else "$hours hrs $minutes min"

    // The tremor came from reading drag deltas in a coordinate space that was
    // moving under the finger: per-frame delta reported by `detectDragGestures`
    // is `pointer_delta - element_delta`, so when a drag on the bottom handle
    // made the body grow, the handle slid down with it and the next frame's
    // delta flipped sign. Result: rapid oscillation.
    //
    // Fix: attach a single pointer handler to an outer Box that never moves
    // (fillMaxSize over the timeline's fixed area), pick which region was
    // touched on the first-down event, capture the starting state, and drive
    // new start/end from absolute pointer travel (`change.position.y -
    // downY`) instead of accumulating per-frame deltas. Visual body and
    // handles are inside and own no pointer input — pure decoration.
    val currentStart by rememberUpdatedState(startMin)
    val currentEnd by rememberUpdatedState(endMin)
    val currentOnRangeChange by rememberUpdatedState(onRangeChange)
    val leftInsetPx = with(density) { leftInset.toPx() }
    val rightInsetPx = with(density) { rightInset.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val hitRadiusPx = handleSizePx / 2f + with(density) { 14.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val downPos = down.position

                    // Snapshot the current range — the drag equations run
                    // off this fixed origin so state updates don't feed back
                    // into the input.
                    val startAtDown = currentStart
                    val endAtDown = currentEnd
                    val topAtDown = startAtDown * pxPerMinute
                    val bottomAtDown = topAtDown + (endAtDown - startAtDown) * pxPerMinute
                    val rightEdgePx = size.width - rightInsetPx
                    val tlCenter = Offset(leftInsetPx, topAtDown)
                    val brCenter = Offset(rightEdgePx, bottomAtDown)

                    val target = when {
                        (downPos - brCenter).getDistance() <= hitRadiusPx -> DragTarget.End
                        (downPos - tlCenter).getDistance() <= hitRadiusPx -> DragTarget.Start
                        downPos.x in leftInsetPx..rightEdgePx &&
                            downPos.y in topAtDown..bottomAtDown -> DragTarget.Body
                        else -> null
                    } ?: return@awaitEachGesture

                    // Wait for touch slop before committing.
                    val slopChange = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    } ?: return@awaitEachGesture

                    val downY = downPos.y
                    // Apply the slop-crossing event first so the block starts
                    // moving with the finger.
                    applyDrag(
                        target = target,
                        totalDy = slopChange.position.y - downY,
                        pxPerMinute = pxPerMinute,
                        startAtDown = startAtDown,
                        endAtDown = endAtDown,
                        totalMinutes = totalMinutes,
                        onRangeChange = currentOnRangeChange
                    )
                    slopChange.consume()

                    drag(down.id) { change ->
                        applyDrag(
                            target = target,
                            totalDy = change.position.y - downY,
                            pxPerMinute = pxPerMinute,
                            startAtDown = startAtDown,
                            endAtDown = endAtDown,
                            totalMinutes = totalMinutes,
                            onRangeChange = currentOnRangeChange
                        )
                        change.consume()
                    }
                }
            }
    ) {
        // Visual body — no pointer input.
        Box(
            modifier = Modifier
                .offset { IntOffset(0, topPx.roundToInt()) }
                .fillMaxWidth()
                .padding(start = leftInset, end = rightInset)
                .height(heightDp)
                .background(DurationAccent, RoundedCornerShape(12.dp))
        ) {
            if (heightDp > 56.dp) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "${formatKoreanTime(startTime)} - ${formatKoreanTime(endTime)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = durationLabel,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Visual-only handles at the corners.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (-handleSizePx / 2f).roundToInt(),
                            (-handleSizePx / 2f).roundToInt()
                        )
                    }
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, DurationAccent, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset {
                        IntOffset(
                            (handleSizePx / 2f).roundToInt(),
                            (handleSizePx / 2f).roundToInt()
                        )
                    }
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, DurationAccent, CircleShape)
            )
        }
    }
}

private enum class DragTarget { Start, End, Body }

private fun applyDrag(
    target: DragTarget,
    totalDy: Float,
    pxPerMinute: Float,
    startAtDown: Int,
    endAtDown: Int,
    totalMinutes: Int,
    onRangeChange: (Int, Int) -> Unit
) {
    val dMin = (totalDy / pxPerMinute).toInt()
    when (target) {
        DragTarget.End -> {
            val newEnd = (endAtDown + dMin).coerceIn(startAtDown + 15, totalMinutes)
            onRangeChange(startAtDown, newEnd)
        }
        DragTarget.Start -> {
            val newStart = (startAtDown + dMin).coerceIn(0, endAtDown - 15)
            onRangeChange(newStart, endAtDown)
        }
        DragTarget.Body -> {
            val span = endAtDown - startAtDown
            val newStart = (startAtDown + dMin).coerceIn(0, totalMinutes - span)
            onRangeChange(newStart, newStart + span)
        }
    }
}

private fun formatDurationHeader(date: LocalDate): String {
    val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN).removeSuffix("요일")
    return "$dow, ${date.monthValue}월 ${date.dayOfMonth}"
}

// ───────────────────────── Formatters ─────────────────────────

private fun minutesFromBase(baseDate: LocalDate, dt: LocalDateTime): Int {
    val baseStart = baseDate.atStartOfDay()
    return java.time.Duration.between(baseStart, dt).toMinutes().toInt()
}

private fun formatKoreanDateWithDay(date: LocalDate): String {
    val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    return "${date.monthValue}월 ${date.dayOfMonth}일, ${dow.removeSuffix("요일")}"
}

private fun formatMonthDay(date: LocalDate): String {
    return "${date.monthValue}월 ${date.dayOfMonth}일"
}

private fun formatKoreanTime(time: LocalTime): String {
    val ampm = if (time.hour < 12) "오전" else "오후"
    val hour12 = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    return "%s %d:%02d".format(ampm, hour12, time.minute)
}

private fun formatKoreanHour(hour: Int): String {
    val ampm = if (hour < 12) "오전" else "오후"
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$ampm $h12"
}
