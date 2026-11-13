package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Task
import com.bettertick.ui.screens.tasks.LeavePeriod
import com.bettertick.ui.screens.tasks.LeavePrefs
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.delay

/** Shares the shift chips' allowances and usage; never stores a second balance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveBalanceMenu(tasks: List<Task>, onSettings: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    // Refresh across midnight and when a sleeping app resumes, without a background job.
    LaunchedEffect(Unit) {
        while (true) {
            today = LocalDate.now()
            delay(30_000)
        }
    }
    LaunchedEffect(tasks, today) { LeavePrefs.recount(tasks) }
    val types = listOf("병가", "연차", "지휴", "특휴")
    val colors = listOf(Color(0xFFE57373), Color(0xFF66BB6A), Color(0xFFB388FF), Color(0xFFFFB74D))
    fun label(type: String) = if (type == "연차") "연가" else type
    fun balance(type: String, day: LocalDate): String {
        val left = LeavePrefs.remaining(type, day) ?: return "설정"
        return if (left < 0) "${-left}일 초과" else "${left}일"
    }

    Surface(
        onClick = {
            monthKey = YearMonth.from(today).toString()
            expanded = true
        },
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp
    ) {
        Row(
            Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("스케줄 보기", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }

    // 월별 총 일수 직접 입력: (type, a day in that month).
    var editMonthTotal by remember { mutableStateOf<Pair<String, LocalDate>?>(null) }

    if (expanded) {
        val month = YearMonth.parse(monthKey)
        val day = month.atDay(today.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("남은 휴가", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { monthKey = month.minusMonths(1).toString() }) { Text("이전 달") }
                    Text("${month.year}년 ${month.monthValue}월", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { monthKey = month.plusMonths(1).toString() }) { Text("다음 달") }
                }
                types.forEachIndexed { index, type ->
                    val period = when (LeavePrefs.periodOf(type)) {
                        LeavePeriod.MONTH -> "${month.monthValue}월"
                        LeavePeriod.YEAR -> "${month.year}년"
                        LeavePeriod.SERVICE_YEAR -> LeavePrefs.periodName(type, day)
                        LeavePeriod.NONE -> "전체 기간"
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            expanded = false
                            onSettings(type)
                        }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label(type), color = colors[index], fontWeight = FontWeight.Bold)
                                Text(if (LeavePrefs.periodOf(type) == LeavePeriod.SERVICE_YEAR && LeavePrefs.workStartDate == null) "근무 시작일 미설정 · 눌러서 설정"
                                    else if (LeavePrefs.remaining(type, day) == null) "지급 일수 미설정 · 눌러서 설정"
                                    else "$period · 총 ${LeavePrefs.quota(type, day)}일 · 사용 ${LeavePrefs.used(type, day)}일", fontSize = 12.sp)
                            }
                            Text(balance(type, day), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (LeavePrefs.allowsMonthTotal(type, day)) {
                        val manual = LeavePrefs.monthTotal(type, day)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (manual != null) "${month.monthValue}월 총 ${manual}일 직접 입력함"
                                else "${month.monthValue}월 총 일수를 직접 정할 수 있어요",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            )
                            if (manual != null) {
                                TextButton(onClick = { LeavePrefs.updateMonthTotal(type, day, null) }) { Text("자동으로") }
                            }
                            TextButton(onClick = { editMonthTotal = type to day }) {
                                Text(if (manual != null) "수정" else "직접 입력")
                            }
                        }
                    }
                }
                if (LeavePrefs.autoJihyu) {
                    Text("지휴 자동 계산 · ${LeavePrefs.periodOf("지휴").label}", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold)
                    Text("공휴일${if (LeavePrefs.includeSaturday) "·토요일" else ""}${if (LeavePrefs.includeSunday) "·일요일" else ""} 근무 기준 · 예정 근무 포함 · 휴가 사용일 제외", fontSize = 12.sp)
                    val manualJihyu = LeavePrefs.monthTotal("지휴", day)
                    Text(
                        if (manualJihyu != null) "직접 입력 ${manualJihyu}일 사용 중 · 자동 계산이면 ${LeavePrefs.earnedIn(day)}일"
                        else "자동 ${LeavePrefs.earnedIn(day)}일 + 추가 ${LeavePrefs.quota("지휴", day) - LeavePrefs.earnedIn(day)}일",
                        fontSize = 12.sp
                    )
                    // Which red days made up the 자동 days, so the number can be checked.
                    LeavePrefs.earnedDaysIn(day).forEach { (d, kind) ->
                        Text(
                            "· ${d.monthValue}월 ${d.dayOfMonth}일 (${"월화수목금토일"[d.dayOfWeek.value - 1]}) " +
                                "${LeavePrefs.redDayName(d) ?: "빨간날"} ${com.bettertick.data.model.shiftKindName(kind)} 근무 +${LeavePrefs.weightOf(kind)}일" +
                                if (d.isAfter(java.time.LocalDate.now())) " (예정)" else "",
                            fontSize = 12.sp
                        )
                    }
                }
                Text("항목을 누르면 지급 일수와 계산 기준을 설정할 수 있어요.", modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp)
            }
        }
    }

    editMonthTotal?.let { (type, d) ->
        var text by remember(type, d) { mutableStateOf(LeavePrefs.monthTotal(type, d)?.toString() ?: "") }
        val value = text.toIntOrNull()
        AlertDialog(
            onDismissRequest = { editMonthTotal = null },
            title = { Text("${d.year}년 ${d.monthValue}월 총 ${label(type)}") },
            text = {
                Column {
                    Text("이 달은 자동 계산 대신 입력한 일수를 씁니다.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() }.take(3) },
                        singleLine = true,
                        suffix = { Text("일") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = value != null, onClick = {
                    LeavePrefs.updateMonthTotal(type, d, value)
                    editMonthTotal = null
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { editMonthTotal = null }) { Text("취소") } }
        )
    }
}
