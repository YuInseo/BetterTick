package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.YearMonth

private val RepeatEndAccent = Color(0xFF4A90E2)

/** End-condition for a recurring task. */
sealed interface RepeatEnd {
    data object Never : RepeatEnd
    data class ByDate(val date: LocalDate) : RepeatEnd
    data class ByCount(val count: Int) : RepeatEnd
}

/** Label for the 반복 종료 row. */
fun RepeatEnd.displayLabel(): String = when (this) {
    RepeatEnd.Never -> "없음"
    is RepeatEnd.ByDate -> "${date.monthValue}월 ${date.dayOfMonth}일"
    is RepeatEnd.ByCount -> "${count}회"
}

/** Serialize for [Task.repeatEnd]. null means Never. */
fun RepeatEnd.toPersisted(): String? = when (this) {
    RepeatEnd.Never -> null
    is RepeatEnd.ByDate -> "DATE:$date"
    is RepeatEnd.ByCount -> "COUNT:$count"
}

fun parseRepeatEnd(raw: String?): RepeatEnd {
    if (raw.isNullOrBlank()) return RepeatEnd.Never
    return runCatching {
        val (kind, value) = raw.split(":", limit = 2).let { it[0] to it.getOrNull(1).orEmpty() }
        when (kind) {
            "DATE" -> RepeatEnd.ByDate(LocalDate.parse(value))
            "COUNT" -> RepeatEnd.ByCount(value.toInt())
            else -> RepeatEnd.Never
        }
    }.getOrDefault(RepeatEnd.Never)
}

private enum class EndTab(val label: String) {
    Date("날짜별로 종료"),
    Count("카운트로 종료")
}

/**
 * Two-tab dialog for setting a recurrence end condition (image 3 / image 4
 * of the reference). 날짜별로 종료 shows a month calendar; 카운트로 종료 shows
 * a wheel picker over 1–200. The confirm button returns a [RepeatEnd]
 * reflecting the active tab; switching tabs preserves each side's draft.
 */
@Composable
fun RepeatEndDialog(
    initial: RepeatEnd,
    referenceDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (RepeatEnd) -> Unit
) {
    var tab by remember {
        mutableStateOf(
            when (initial) {
                is RepeatEnd.ByCount -> EndTab.Count
                else -> EndTab.Date
            }
        )
    }
    val defaultDate = remember(referenceDate) { referenceDate.plusMonths(1) }
    var selectedDate by remember {
        mutableStateOf((initial as? RepeatEnd.ByDate)?.date ?: defaultDate)
    }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var count by remember {
        mutableStateOf((initial as? RepeatEnd.ByCount)?.count ?: 2)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            EndTabRow(selected = tab, onSelect = { tab = it })

            Spacer(Modifier.height(16.dp))

            if (tab == EndTab.Date) {
                DatePane(
                    month = visibleMonth,
                    selected = selectedDate,
                    onPrev = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNext = { visibleMonth = visibleMonth.plusMonths(1) },
                    onSelect = { selectedDate = it }
                )
            } else {
                CountPane(
                    count = count,
                    onCountChange = { count = it }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = RepeatEndAccent,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "확인",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RepeatEndAccent,
                    modifier = Modifier
                        .clickable {
                            onConfirm(
                                when (tab) {
                                    EndTab.Date -> RepeatEnd.ByDate(selectedDate)
                                    EndTab.Count -> RepeatEnd.ByCount(count)
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EndTabRow(
    selected: EndTab,
    onSelect: (EndTab) -> Unit
) {
    Row {
        EndTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = tab.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) RepeatEndAccent else TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 28.dp else 0.dp)
                        .background(RepeatEndAccent)
                )
            }
        }
    }
}

@Composable
private fun DatePane(
    month: YearMonth,
    selected: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    Column {
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
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(4.dp))

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

        Spacer(Modifier.height(4.dp))

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
                                val isSelected = date == selected
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSelected) Modifier.background(RepeatEndAccent)
                                            else Modifier
                                        )
                                        .clickable { onSelect(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPane(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val items = remember { (1..200).map { it.toString() } }
    Row(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            WheelPicker(
                items = items,
                selectedIndex = (count - 1).coerceIn(0, items.lastIndex),
                onSelectedIndexChange = { onCountChange(it + 1) }
            )
        }
        Text(
            text = "카운트",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}
