package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary

private val AlarmPickerAccent = Color(0xFF4A90E2)

/**
 * Alarm preset + custom alarm offset model. Built-in preset list matches
 * what users of the reference app are familiar with; the custom row opens
 * [CustomAlarmDialog] for fine-grained offset entry.
 */
sealed interface AlarmChoice {
    data object None : AlarmChoice
    data object OnTime : AlarmChoice           // 정각에
    data class Preset(val minutesBefore: Int) : AlarmChoice
    data class Custom(val days: Int, val hours: Int, val minutes: Int) : AlarmChoice
}

/** Human-facing label used in the date picker's 알림 row. */
fun AlarmChoice.displayLabel(): String = when (this) {
    AlarmChoice.None -> "없음"
    AlarmChoice.OnTime -> "정각에"
    is AlarmChoice.Preset -> when {
        minutesBefore < 60 -> "${minutesBefore}분 전"
        minutesBefore % (60 * 24) == 0 -> "${minutesBefore / (60 * 24)}일 전"
        minutesBefore % 60 == 0 -> "${minutesBefore / 60}시간 전"
        else -> "${minutesBefore}분 전"
    }
    is AlarmChoice.Custom -> buildString {
        if (days > 0) append("${days}일 ")
        if (hours > 0) append("${hours}시간 ")
        if (minutes > 0) append("${minutes}분 ")
        append("전")
    }.trim().ifEmpty { "정각에" }
}

/**
 * Preset list dialog. Tapping a preset reports via [onPresetSelected] and
 * closes. Tapping "사용자정의알람" forwards to [onCustomRequested] so the
 * parent can stack the [CustomAlarmDialog] on top.
 *
 * A trailing [persistent] switch at the bottom lets the user flag the
 * alarm as "지속적인" (won't auto-dismiss). It's managed by the parent so
 * it can be persisted alongside the other alarm state.
 */
@Composable
fun AlarmPickerDialog(
    current: AlarmChoice,
    persistent: Boolean,
    onDismiss: () -> Unit,
    onPresetSelected: (AlarmChoice) -> Unit,
    onCustomRequested: () -> Unit,
    onPersistentChange: (Boolean) -> Unit
) {
    val presets = listOf(
        AlarmChoice.OnTime,
        AlarmChoice.Preset(5),
        AlarmChoice.Preset(10),
        AlarmChoice.Preset(15),
        AlarmChoice.Preset(30),
        AlarmChoice.Preset(60),
        AlarmChoice.Preset(60 * 24)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "알림",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(4.dp))

            presets.forEach { preset ->
                AlarmRow(
                    label = preset.displayLabel(),
                    selected = preset == current,
                    trailing = AlarmTrailing.Check,
                    onClick = { onPresetSelected(preset) }
                )
            }

            AlarmRow(
                label = "사용자정의알람",
                selected = current is AlarmChoice.Custom,
                // The reference shows a > arrow on custom to signal that
                // tapping it opens another screen rather than committing
                // a value inline.
                trailing = AlarmTrailing.Arrow,
                onClick = onCustomRequested
            )

            HorizontalDivider(
                color = TextTertiary.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // "지속적인 알림" switch row — separated from the list because
            // it's a modifier of whichever alarm is selected, not a
            // mutually-exclusive choice.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPersistentChange(!persistent) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "지속적인 알림",
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = persistent,
                    onCheckedChange = onPersistentChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AlarmPickerAccent,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0xFF3A3A3A),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlarmPickerAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private enum class AlarmTrailing { Check, Arrow, None }

@Composable
private fun AlarmRow(
    label: String,
    selected: Boolean,
    trailing: AlarmTrailing,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        when (trailing) {
            AlarmTrailing.Check -> {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AlarmPickerAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(20.dp))
                }
            }
            AlarmTrailing.Arrow -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            AlarmTrailing.None -> Box(modifier = Modifier.size(20.dp))
        }
    }
}
