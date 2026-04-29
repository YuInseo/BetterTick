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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import java.time.DayOfWeek
import java.time.LocalDate

private val RepeatPickerAccent = Color(0xFF4A90E2)

/** Recurrence rule attached to a task, surfaced as the date picker's 반복 row. */
sealed interface RepeatChoice {
    data object None : RepeatChoice
    data object Daily : RepeatChoice
    data class Weekly(val dayOfWeek: DayOfWeek) : RepeatChoice
    data class Monthly(val dayOfMonth: Int) : RepeatChoice
    data class Yearly(val month: Int, val dayOfMonth: Int) : RepeatChoice
    data object Weekdays : RepeatChoice
    data class Custom(
        val type: CustomRepeatType,
        val frequencyNum: Int,
        val frequencyUnit: CustomRepeatUnit,
        val daysOfWeek: Set<DayOfWeek> = emptySet(),
        val specificDates: Set<LocalDate> = emptySet()
    ) : RepeatChoice
}

enum class CustomRepeatType(val label: String) {
    ByDueDate("만기일별로 정렬하기"),
    ByCompletionDate("완료 날짜별로 정렬하기"),
    BySpecificDates("특정 날짜별로 선택하여 검색하기")
}

enum class CustomRepeatUnit(val label: String) {
    Day("일"),
    Week("주"),
    Month("개월"),
    Year("Year")
}

/** Korean single-letter day-of-week. */
fun dayOfWeekKo(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.SUNDAY -> "일"
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
}

/**
 * Compact string serialization for [Task.repeatRule]. Chosen to be trivial
 * to hand-parse on the widget side — no RRULE dependency needed.
 *
 * Formats:
 *   "DAILY"
 *   "WEEKLY:MONDAY"
 *   "MONTHLY:15"
 *   "YEARLY:4:20"
 *   "WEEKDAYS"
 * Custom repeats aren't modeled here yet; they serialize to null so the
 * widget treats them as single-occurrence.
 */
fun RepeatChoice.toRule(): String? = when (this) {
    RepeatChoice.None -> null
    RepeatChoice.Daily -> "DAILY"
    is RepeatChoice.Weekly -> "WEEKLY:${dayOfWeek.name}"
    is RepeatChoice.Monthly -> "MONTHLY:$dayOfMonth"
    is RepeatChoice.Yearly -> "YEARLY:$month:$dayOfMonth"
    RepeatChoice.Weekdays -> "WEEKDAYS"
    is RepeatChoice.Custom -> {
        // Format: CUSTOM|<type>|<freqNum>|<unit>|<dow csv>|<dates csv>
        // Uses `|` as field separator since `:` would collide with the
        // yearly-rule syntax and dates themselves contain ":".
        val dows = daysOfWeek.joinToString(",") { it.name }
        val dates = specificDates.joinToString(",") { it.toString() }
        "CUSTOM|${type.name}|$frequencyNum|${frequencyUnit.name}|$dows|$dates"
    }
}

fun parseRepeatRule(rule: String?): RepeatChoice {
    if (rule.isNullOrBlank()) return RepeatChoice.None
    return runCatching {
        if (rule.startsWith("CUSTOM|")) {
            val parts = rule.split("|")
            // parts: ["CUSTOM", type, freqNum, unit, dows, dates]
            val type = CustomRepeatType.valueOf(parts[1])
            val freqNum = parts[2].toInt()
            val unit = CustomRepeatUnit.valueOf(parts[3])
            val dows = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                ?.split(",")?.map { DayOfWeek.valueOf(it) }?.toSet()
                ?: emptySet()
            val dates = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
                ?.split(",")?.map { LocalDate.parse(it) }?.toSet()
                ?: emptySet()
            RepeatChoice.Custom(type, freqNum, unit, dows, dates)
        } else {
            val parts = rule.split(":")
            when (parts[0]) {
                "DAILY" -> RepeatChoice.Daily
                "WEEKLY" -> RepeatChoice.Weekly(DayOfWeek.valueOf(parts[1]))
                "MONTHLY" -> RepeatChoice.Monthly(parts[1].toInt())
                "YEARLY" -> RepeatChoice.Yearly(parts[1].toInt(), parts[2].toInt())
                "WEEKDAYS" -> RepeatChoice.Weekdays
                else -> RepeatChoice.None
            }
        }
    }.getOrDefault(RepeatChoice.None)
}

/** Label for the date picker's 반복 row. `ref` is the task's current due-date
 *  used to render context strings like `(15일)` / `(4월 15)`. */
fun RepeatChoice.displayLabel(ref: LocalDate): String = when (this) {
    RepeatChoice.None -> "없음"
    RepeatChoice.Daily -> "매일"
    is RepeatChoice.Weekly -> "매주 (${dayOfWeekKo(dayOfWeek)})"
    is RepeatChoice.Monthly -> "매월 (${dayOfMonth}일)"
    is RepeatChoice.Yearly -> "매년 (${month}월 ${dayOfMonth})"
    RepeatChoice.Weekdays -> "매주 평일"
    is RepeatChoice.Custom -> customLabel()
}

/** Mirror the CustomRepeatDialog's own header summary so the picker row
 *  reflects exactly what the user configured (e.g. "매주 월,수,금") rather
 *  than the generic "사용자 설정" placeholder. */
private fun RepeatChoice.Custom.customLabel(): String {
    if (type == CustomRepeatType.BySpecificDates) {
        return if (specificDates.isEmpty()) "특정 날짜"
        else "특정 날짜 ${specificDates.size}개"
    }
    val dowOrder = listOf(
        java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
        java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY,
        java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY
    )
    val freqLabel = if (frequencyNum <= 1) "매" else "매 $frequencyNum"
    return when (frequencyUnit) {
        CustomRepeatUnit.Day -> "${freqLabel}일"
        CustomRepeatUnit.Month -> "${freqLabel}개월"
        CustomRepeatUnit.Year -> "${freqLabel}년"
        CustomRepeatUnit.Week -> {
            val dows = daysOfWeek.sortedBy { dowOrder.indexOf(it) }
                .joinToString(",") { dayOfWeekKo(it) }
            val base = if (frequencyNum <= 1) "매주" else "매 ${frequencyNum}주"
            if (dows.isEmpty()) base else "$base ($dows)"
        }
    }
}

/**
 * Preset list dialog for task recurrence (image 1 of the reference). The
 * in-parens previews (수 / 15일 / 4월 15) are populated from [referenceDate]
 * so the preset labels reflect whatever date the user is targeting.
 */
@Composable
fun RepeatPickerDialog(
    current: RepeatChoice,
    referenceDate: LocalDate,
    onDismiss: () -> Unit,
    onPresetSelected: (RepeatChoice) -> Unit,
    onCustomRequested: () -> Unit
) {
    // Presets derived from the reference date. `매주 (X)` uses today's day
    // of week; `매월 (Nth)` uses today's day of month; `매년 (M월 D)` uses
    // the full date. Matches the reference app's preview conventions.
    val presets: List<Pair<String, RepeatChoice>> = listOf(
        "없음" to RepeatChoice.None,
        "매일" to RepeatChoice.Daily,
        "매주" to RepeatChoice.Weekly(referenceDate.dayOfWeek),
        "매월" to RepeatChoice.Monthly(referenceDate.dayOfMonth),
        "매년" to RepeatChoice.Yearly(referenceDate.monthValue, referenceDate.dayOfMonth)
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
                text = "반복",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(4.dp))

            presets.forEach { (baseLabel, choice) ->
                val preview = previewFor(choice, referenceDate)
                RepeatRow(
                    label = baseLabel,
                    preview = preview,
                    selected = choice == current,
                    showCheck = true,
                    onClick = { onPresetSelected(choice) }
                )
            }

            HorizontalDivider(
                color = TextTertiary.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            RepeatRow(
                label = "매주 평일",
                preview = "(월요일 - 금요일)",
                selected = current == RepeatChoice.Weekdays,
                showCheck = true,
                onClick = { onPresetSelected(RepeatChoice.Weekdays) }
            )

            HorizontalDivider(
                color = TextTertiary.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            RepeatRow(
                label = "사용자 설정",
                preview = null,
                selected = current is RepeatChoice.Custom,
                showCheck = false,
                onClick = onCustomRequested
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = RepeatPickerAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun previewFor(choice: RepeatChoice, ref: LocalDate): String? = when (choice) {
    RepeatChoice.None, RepeatChoice.Daily, RepeatChoice.Weekdays -> null
    is RepeatChoice.Weekly -> "(${dayOfWeekKo(choice.dayOfWeek)})"
    is RepeatChoice.Monthly -> "(${choice.dayOfMonth}일)"
    is RepeatChoice.Yearly -> "(${choice.month}월 ${choice.dayOfMonth})"
    is RepeatChoice.Custom -> null
}

@Composable
private fun RepeatRow(
    label: String,
    preview: String?,
    selected: Boolean,
    showCheck: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = if (selected) RepeatPickerAccent else Color.White
            )
            if (preview != null) {
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = preview,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
        if (showCheck && selected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = RepeatPickerAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Box(modifier = Modifier.size(20.dp))
        }
    }
}
