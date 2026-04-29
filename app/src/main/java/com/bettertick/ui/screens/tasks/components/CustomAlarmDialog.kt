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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary

private val CustomAlarmAccent = Color(0xFF4A90E2)

/**
 * "기타 알림" dialog — three wheel pickers for the days / hours / minutes
 * of advance notice.
 *
 * The expiry warning ("알림이 만료되었습니다") appears when the picked
 * offset is zero — a zero-offset relative to a task already at "now" can't
 * produce a future alarm. With a concrete task due-date in hand we could
 * check against `dueDate - offset < LocalDateTime.now()`, but view-only
 * for now: we warn on the degenerate (0, 0, 0) case so the visual is
 * exercised.
 */
@Composable
fun CustomAlarmDialog(
    initialDays: Int = 0,
    initialHours: Int = 0,
    initialMinutes: Int = 15,
    onDismiss: () -> Unit,
    onConfirm: (days: Int, hours: Int, minutes: Int) -> Unit
) {
    var days by remember { mutableIntStateOf(initialDays.coerceIn(0, 60)) }
    var hours by remember { mutableIntStateOf(initialHours.coerceIn(0, 23)) }
    var minutes by remember { mutableIntStateOf(initialMinutes.coerceIn(0, 59)) }

    val isExpired = days == 0 && hours == 0 && minutes == 0

    val dayItems = remember { (0..60).map { it.toString() } }
    val hourItems = remember { (0..23).map { it.toString() } }
    val minuteItems = remember { (0..59).map { it.toString() } }

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
                text = "기타 알림",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            WheelRow(
                days = days,
                hours = hours,
                minutes = minutes,
                dayItems = dayItems,
                hourItems = hourItems,
                minuteItems = minuteItems,
                onDaysChange = { days = it },
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it }
            )

            Spacer(Modifier.height(8.dp))

            if (isExpired) {
                Text(
                    text = "알림이 만료되었습니다.",
                    fontSize = 13.sp,
                    color = OverdueRed,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = CustomAlarmAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "완료",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CustomAlarmAccent,
                    modifier = Modifier
                        .clickable {
                            if (!isExpired) onConfirm(days, hours, minutes)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun WheelRow(
    days: Int,
    hours: Int,
    minutes: Int,
    dayItems: List<String>,
    hourItems: List<String>,
    minuteItems: List<String>,
    onDaysChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            items = dayItems,
            selectedIndex = days,
            onSelectedIndexChange = onDaysChange,
            modifier = Modifier.weight(1f)
        )
        UnitLabel("일")
        WheelPicker(
            items = hourItems,
            selectedIndex = hours,
            onSelectedIndexChange = onHoursChange,
            modifier = Modifier.weight(1f)
        )
        UnitLabel("시")
        WheelPicker(
            items = minuteItems,
            selectedIndex = minutes,
            onSelectedIndexChange = onMinutesChange,
            modifier = Modifier.weight(1f)
        )
        UnitLabel("분")
    }
}

@Composable
private fun UnitLabel(text: String) {
    // Labels sit at the centre slot of the adjacent wheel, so they
    // visually attach to the selected number.
    Box(
        modifier = Modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}
