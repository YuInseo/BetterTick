package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.bettertick.ui.screens.tasks.components.leaveColor
import com.bettertick.ui.theme.DividerColor
import com.bettertick.ui.theme.GroupedBackground
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("스케줄 보기", style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }

    // 월별 총 일수 직접 입력: (type, a day in that month).
    // 기간별 총 일수 직접 입력: (type, a day in that period, period name for the title).
    var editMonthTotal by remember { mutableStateOf<Triple<String, LocalDate, String>?>(null) }

    if (expanded) {
        val month = YearMonth.parse(monthKey)
        val day = month.atDay(today.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
        val accent = ThemeState.accentColor
        // Same look as the settings pages: page-colored sheet, rounded cards.
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            // Opens over half the screen; scroll (or drag up) for the rest.
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = GroupedBackground,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary.copy(alpha = 0.5f)) }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "남은 휴가",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthKey = month.minusMonths(1).toString() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달", tint = TextSecondary)
                    }
                    Text(
                        "${month.year}년 ${month.monthValue}월",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { monthKey = month.plusMonths(1).toString() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달", tint = TextSecondary)
                    }
                }
                types.forEach { type ->
                    val period = when (LeavePrefs.periodOf(type)) {
                        LeavePeriod.MONTH -> "${month.monthValue}월"
                        LeavePeriod.YEAR -> "${month.year}년"
                        LeavePeriod.SERVICE_YEAR -> LeavePrefs.periodName(type, day)
                        LeavePeriod.NONE -> "전체 기간"
                    }
                    val left = LeavePrefs.remaining(type, day)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GroupedCard)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = false
                                    onSettings(type)
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(leaveColor(type)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label(type), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (LeavePrefs.periodOf(type) == LeavePeriod.SERVICE_YEAR && LeavePrefs.workStartDate == null) "근무 시작일 미설정 · 눌러서 설정"
                                    else if (left == null) "지급 일수 미설정 · 눌러서 설정"
                                    else "$period · 총 ${LeavePrefs.quota(type, day)}일 · 사용 ${LeavePrefs.used(type, day)}일",
                                    fontSize = 12.sp, color = TextTertiary
                                )
                            }
                            Text(
                                balance(type, day),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    left == null -> accent
                                    left < 0 -> OverdueRed
                                    else -> TextPrimary
                                }
                            )
                        }
                        if (LeavePrefs.allowsMonthTotal(type, day)) {
                            val manual = LeavePrefs.monthTotal(type, day)
                            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(0.5.dp).background(DividerColor))
                            Row(
                                Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (manual != null) "$period 총 ${manual}일 직접 입력함"
                                    else "$period 총 일수 자동 계산 · 직접 정할 수 있어요",
                                    fontSize = 12.sp, color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (manual != null) {
                                    TextButton(onClick = { LeavePrefs.updateMonthTotal(type, day, null) }) {
                                        Text("자동으로", color = TextSecondary, fontSize = 13.sp)
                                    }
                                }
                                TextButton(onClick = { editMonthTotal = Triple(type, day, period) }) {
                                    Text(if (manual != null) "수정" else "직접 입력", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                if (LeavePrefs.autoJihyu) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GroupedCard)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "지휴 자동 계산 · ${LeavePrefs.periodOf("지휴").label}",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                        )
                        Text(
                            "공휴일${if (LeavePrefs.includeSaturday) "·토요일" else ""}${if (LeavePrefs.includeSunday) "·일요일" else ""} 근무 기준 · 예정 근무 포함 · 휴가 사용일 제외",
                            fontSize = 12.sp, color = TextTertiary
                        )
                        val manualJihyu = LeavePrefs.monthTotal("지휴", day)
                        Text(
                            if (manualJihyu != null) "직접 입력 ${manualJihyu}일 사용 중 · 자동 계산이면 ${LeavePrefs.earnedIn(day)}일"
                            else "자동 ${LeavePrefs.earnedIn(day)}일 + 추가 ${LeavePrefs.quota("지휴", day) - LeavePrefs.earnedIn(day)}일",
                            fontSize = 13.sp, color = TextSecondary
                        )
                        // Which red days made up the 자동 days, so the number can be checked.
                        LeavePrefs.earnedDaysIn(day).forEach { (d, kind) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${d.monthValue}월 ${d.dayOfMonth}일 (${"월화수목금토일"[d.dayOfWeek.value - 1]}) " +
                                        "${LeavePrefs.redDayName(d) ?: "빨간날"} ${com.bettertick.data.model.shiftKindName(kind)}",
                                    fontSize = 13.sp, color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "+${LeavePrefs.weightOf(kind)}일" + if (d.isAfter(LocalDate.now())) " · 예정" else "",
                                    fontSize = 12.sp, color = leaveColor("지휴")
                                )
                            }
                        }
                    }
                }
                Text(
                    "항목을 누르면 지급 일수와 계산 기준을 설정할 수 있어요.",
                    fontSize = 12.sp, color = TextTertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }
    }

    editMonthTotal?.let { (type, d, periodName) ->
        var text by remember(type, d) { mutableStateOf(LeavePrefs.monthTotal(type, d)?.toString() ?: "") }
        val value = text.toIntOrNull()
        AlertDialog(
            onDismissRequest = { editMonthTotal = null },
            containerColor = GroupedCard,
            title = { Text("$periodName 총 ${label(type)}", color = TextPrimary) },
            text = {
                Column {
                    Text("이 기간은 자동 계산 대신 입력한 일수를 씁니다.", fontSize = 13.sp, color = TextSecondary)
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
                }) { Text("저장", color = if (value != null) ThemeState.accentColor else TextTertiary) }
            },
            dismissButton = { TextButton(onClick = { editMonthTotal = null }) { Text("취소", color = TextSecondary) } }
        )
    }
}
