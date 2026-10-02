package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.data.model.ShiftDetection
import com.bettertick.data.model.matchedPrefixLength
import com.bettertick.data.model.shiftLabelOn
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ShiftRepeatEnd(val label: String, val months: Long?) {
    Forever("끝없이", null),
    ThreeMonths("3개월", 3),
    SixMonths("6개월", 6),
    OneYear("1년", 12)
}

private val DEFAULT_PATTERN = listOf("주", "야간", "비", "휴")
private const val MAX_SLOTS = 31

/**
 * 근무 패턴 반복 설정. 달력에 입력된 근무에서 찾은 패턴을 보여주고,
 * 사용자가 칸을 고치거나 더한 뒤 적용하면 그 주기대로 자동 반복된다.
 *
 * @param onApply (시작일, 패턴, 종료일) — 종료일 null = 끝없이
 */
@Composable
fun ShiftRotationDialog(
    detection: ShiftDetection,
    onDismiss: () -> Unit,
    onApply: (start: LocalDate, pattern: List<String>, end: LocalDate?) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val detected = detection.pattern.isNotEmpty()
    val slots = remember(detection) {
        mutableStateListOf<String>().apply {
            addAll(if (detected) detection.pattern else DEFAULT_PATTERN)
        }
    }
    var repeatEnd by remember { mutableStateOf(ShiftRepeatEnd.Forever) }

    val start = detection.start
    val pattern = slots.map { it.trim() }
    val canApply = pattern.any { it.isNotBlank() }
    val from = start.plusDays(matchedPrefixLength(detection.run, pattern).toLong())
    val dateFmt = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "근무 패턴 반복",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (detected) {
                    "${start.format(dateFmt)}부터 입력된 ${detection.run.size}일에서 " +
                        "${detection.pattern.size}일 주기를 찾았어요."
                } else {
                    "${start.format(dateFmt)}부터 시작해요. 입력된 근무가 없어 기본 패턴을 넣어뒀어요."
                },
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                SectionLabel("패턴 (${slots.size}일 주기)")
                slots.forEachIndexed { index, label ->
                    SlotRow(
                        index = index,
                        date = start.plusDays(index.toLong()),
                        value = label,
                        canRemove = slots.size > 1,
                        onValueChange = { slots[index] = it },
                        onRemove = { slots.removeAt(index) }
                    )
                }
                if (slots.size < MAX_SLOTS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { slots.add("") }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("하루 추가", fontSize = 14.sp, color = accent)
                    }
                }
                Text(
                    text = "비워 둔 칸은 일정 없이 지나가는 날이에요.",
                    fontSize = 12.sp,
                    color = TextTertiary
                )

                Spacer(Modifier.height(16.dp))
                SectionLabel("반복 종료")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShiftRepeatEnd.entries.forEach { option ->
                        val selected = option == repeatEnd
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) accent.copy(alpha = 0.2f) else DarkCard)
                                .border(
                                    1.dp,
                                    if (selected) accent else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { repeatEnd = option }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                option.label,
                                fontSize = 13.sp,
                                color = if (selected) accent else Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionLabel("${from.format(dateFmt)}부터 미리보기")
                PreviewGrid(start = start, pattern = pattern, from = from, accent = accent)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "반복 적용",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canApply) accent else TextTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = canApply) {
                            val end = repeatEnd.months?.let { from.plusMonths(it).minusDays(1) }
                            onApply(start, pattern, end)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SlotRow(
    index: Int,
    date: LocalDate,
    value: String,
    canRemove: Boolean,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text("${index + 1}일차", fontSize = 13.sp, color = Color.White)
            Text(
                date.format(DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)),
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkCard)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text("(일정 없음)", fontSize = 15.sp, color = TextTertiary)
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = canRemove) { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "삭제",
                tint = if (canRemove) TextSecondary else Color.Transparent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 생성 시작일부터 2주치를 7칸 × 2줄로 보여준다. */
@Composable
private fun PreviewGrid(start: LocalDate, pattern: List<String>, from: LocalDate, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { col ->
                    val day = from.plusDays((week * 7 + col).toLong())
                    val label = shiftLabelOn(start, pattern, day)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkCard)
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${day.dayOfMonth}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                        Text(
                            label ?: "-",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (label != null) accent else TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
