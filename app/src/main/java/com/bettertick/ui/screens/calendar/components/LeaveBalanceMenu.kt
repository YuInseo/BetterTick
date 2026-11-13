package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val types = listOf("병가", "연차", "지휴")
    val colors = listOf(Color(0xFFE57373), Color(0xFF66BB6A), Color(0xFFB388FF))
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            types.forEachIndexed { index, type ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label(type), color = colors[index], fontSize = 11.sp)
                    Text(balance(type, today), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (expanded) {
        val month = YearMonth.parse(monthKey)
        val day = month.atDay(1)
        ModalBottomSheet(onDismissRequest = { expanded = false }) {
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
                                Text("$period · 총 ${LeavePrefs.quota(type, day)}일 · 사용 ${LeavePrefs.used(type, day)}일", fontSize = 12.sp)
                            }
                            Text(balance(type, day), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (LeavePrefs.autoJihyu) {
                    Text("지휴 자동 계산 · ${LeavePrefs.periodOf("지휴").label}", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold)
                    Text("공휴일${if (LeavePrefs.includeSunday) "·일요일" else ""} 근무 기준 · 예정 근무 포함 · 휴가 사용일 제외", fontSize = 12.sp)
                    Text("자동 ${LeavePrefs.earnedIn(day)}일 + 추가 ${LeavePrefs.quota("지휴", day) - LeavePrefs.earnedIn(day)}일", fontSize = 12.sp)
                }
                Text("항목을 누르면 지급 일수와 계산 기준을 설정할 수 있어요.", modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp)
            }
        }
    }
}
