package com.bettertick.ui.screens.tasks.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import kotlinx.coroutines.flow.distinctUntilChanged

private val TimePickerAccent = Color(0xFF4A90E2)

/**
 * Time picker dialog with two modes, toggled by the bottom-left icon:
 *  - CLOCK — Material3 [TimePicker] (radial dial)
 *  - WHEEL — three-column wheel picker (hours / minutes / AM/PM)
 *
 * The two modes share a single source of truth (local 24h `hour` + `minute`
 * state), so toggling preserves the currently picked time in either
 * direction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTimePickerDialog(
    initialHour: Int = 9,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    // 24-hour source of truth. Both sub-pickers read from / write to this.
    var hour24 by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var inputMode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "시간",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (inputMode) {
                    WheelTimeInput(
                        hour24 = hour24,
                        minute = minute,
                        onHour24Change = { hour24 = it },
                        onMinuteChange = { minute = it }
                    )
                } else {
                    ClockTimePicker(
                        hour24 = hour24,
                        minute = minute,
                        onTimeChange = { h, m ->
                            hour24 = h
                            minute = m
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToggleIconButton(
                    icon = if (inputMode) Icons.Outlined.AccessTime else Icons.Outlined.Keyboard,
                    onClick = { inputMode = !inputMode }
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TimePickerAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "확인",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TimePickerAccent,
                    modifier = Modifier
                        .clickable { onConfirm(hour24, minute) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/** Clock face mode — wraps Material3's [TimePicker] and keeps our shared
 *  hour/minute state in sync via [snapshotFlow]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockTimePicker(
    hour24: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = hour24,
        initialMinute = minute,
        is24Hour = false
    )

    // Bridge Material3's internal state out to our shared state so the
    // wheel mode sees what the clock picked after a toggle.
    LaunchedEffect(state) {
        snapshotFlow { state.hour to state.minute }
            .distinctUntilChanged()
            .collect { (h, m) -> onTimeChange(h, m) }
    }

    val colors = TimePickerDefaults.colors(
        clockDialColor = DarkSurface,
        clockDialSelectedContentColor = Color.White,
        clockDialUnselectedContentColor = Color.White,
        selectorColor = TimePickerAccent,
        containerColor = DarkSurfaceVariant,
        periodSelectorBorderColor = TimePickerAccent,
        periodSelectorSelectedContainerColor = TimePickerAccent.copy(alpha = 0.2f),
        periodSelectorUnselectedContainerColor = Color.Transparent,
        periodSelectorSelectedContentColor = TimePickerAccent,
        periodSelectorUnselectedContentColor = TextSecondary,
        timeSelectorSelectedContainerColor = Color.Transparent,
        timeSelectorUnselectedContainerColor = Color.Transparent,
        timeSelectorSelectedContentColor = TimePickerAccent,
        timeSelectorUnselectedContentColor = TextSecondary
    )

    TimePicker(state = state, colors = colors)
}

/**
 * Wheel mode — three columns: hours (1..12), minutes (00..59), AM/PM.
 *
 * Translates between the shared 24h state and the 12h display:
 *  - 0 → 12 AM
 *  - 1..11 → 1..11 AM
 *  - 12 → 12 PM
 *  - 13..23 → 1..11 PM
 */
@Composable
private fun WheelTimeInput(
    hour24: Int,
    minute: Int,
    onHour24Change: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val isPm = hour24 >= 12

    val hourItems = remember { (1..12).map { "%02d".format(it) } }
    val minuteItems = remember { (0..59).map { "%02d".format(it) } }
    val ampmItems = remember { listOf("오전", "오후") }

    fun push(newHour12: Int, newPm: Boolean) {
        val newHour24 = when {
            newHour12 == 12 && !newPm -> 0        // 12 AM
            newHour12 == 12 && newPm -> 12        // 12 PM
            newPm -> newHour12 + 12
            else -> newHour12
        }
        if (newHour24 != hour24) onHour24Change(newHour24)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            items = hourItems,
            selectedIndex = hour12 - 1,
            onSelectedIndexChange = { idx -> push(idx + 1, isPm) },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = minuteItems,
            selectedIndex = minute,
            onSelectedIndexChange = { onMinuteChange(it) },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = ampmItems,
            selectedIndex = if (isPm) 1 else 0,
            onSelectedIndexChange = { idx -> push(hour12, idx == 1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToggleIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(22.dp)
        )
    }
}
