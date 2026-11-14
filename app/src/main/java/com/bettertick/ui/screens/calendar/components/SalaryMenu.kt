package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.more.AmountDialog
import com.bettertick.ui.screens.more.PayDivider
import com.bettertick.ui.screens.more.PayRow
import com.bettertick.ui.screens.more.ymLabel
import com.bettertick.ui.screens.tasks.SalaryPrefs
import com.bettertick.ui.screens.tasks.quarterMonths
import com.bettertick.ui.screens.tasks.quarterOf
import com.bettertick.ui.screens.tasks.won
import com.bettertick.ui.theme.GroupedBackground
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Bottom-left floating '월급' button and its month-by-month sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryMenu(onSettings: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var editOverride by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            monthKey = YearMonth.now().toString()
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
            Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("월급", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }

    if (!expanded) return
    val month = YearMonth.parse(monthKey)
    val today = LocalDate.now()
    val accent = ThemeState.accentColor
    val wp = SalaryPrefs.workplaceFor(month)
    val rules = SalaryPrefs.rulesIn(month)

    ModalBottomSheet(
        onDismissRequest = { expanded = false },
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
                "월급",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthKey = month.minusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달", tint = TextSecondary)
                }
                Text(
                    ymLabel(month),
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { monthKey = month.plusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달", tint = TextSecondary)
                }
            }

            when {
                wp == null -> SheetCard {
                    Text(
                        "이 달은 근무지 기간 밖이라 월급이 없습니다.",
                        fontSize = 14.sp, color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                    )
                }
                rules.isEmpty -> SheetCard {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded = false; onSettings() }
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text("${wp.name} 월급을 아직 정하지 않았어요", fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("눌러서 기본급·수당·상여·공제를 설정하세요", fontSize = 12.sp, color = accent)
                    }
                }
                else -> {
                    val payDate = rules.payDate(month)
                    val override = rules.override(month)
                    val total = rules.total(month)
                    SheetCard {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                            Text(wp.name, fontSize = 13.sp, color = TextTertiary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (total < 0) "−" + won(-total) else won(total),
                                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            val days = ChronoUnit.DAYS.between(today, payDate)
                            Text(
                                "월급날 ${payDate.monthValue}월 ${payDate.dayOfMonth}일 (${"월화수목금토일"[payDate.dayOfWeek.value - 1]}) · " +
                                    when {
                                        days > 0 -> "D-$days"
                                        days == 0L -> "오늘"
                                        else -> "지급됨"
                                    },
                                fontSize = 13.sp, color = if (days == 0L) accent else TextSecondary
                            )
                        }
                        PayDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (override != null) "이 달 총액 직접 입력함 (항목 합계 ${won(rules.autoTotal(month))})"
                                else "항목으로 자동 계산 · 직접 정할 수 있어요",
                                fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f)
                            )
                            if (override != null) {
                                TextButton(onClick = { SalaryPrefs.updateOverride(wp.id, month, null) }) {
                                    Text("자동으로", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                            TextButton(onClick = { editOverride = true }) {
                                Text(if (override != null) "수정" else "직접 입력", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    val items = rules.itemsIn(month)
                    if (items.isNotEmpty()) {
                        SheetLabel("이 달 항목")
                        SheetCard {
                            items.forEach { PayRow(it.name, it.scheduleLabel, it.signed) }
                            PayDivider()
                            PayRow("지급 합계", null, rules.gross(month))
                            if (rules.deducted(month) > 0) PayRow("공제 합계", null, -rules.deducted(month))
                        }
                    }

                    val q = quarterOf(month)
                    SheetLabel("${month.year}년 ${q}분기")
                    SheetCard {
                        quarterMonths(month.year, q).forEach { m ->
                            PayRow(
                                "${m.monthValue}월" + if (m == month) " · 보는 달" else "",
                                if (SalaryPrefs.rulesIn(m).override(m) != null) "직접 입력" else null,
                                SalaryPrefs.total(m)
                            )
                        }
                        PayDivider()
                        PayRow("${q}분기 합계", null, SalaryPrefs.quarterTotal(month.year, q))
                    }

                    SheetLabel("${month.year}년")
                    SheetCard {
                        val year = month.year
                        val received = (1..12).map { YearMonth.of(year, it) }
                            .filter { !SalaryPrefs.rulesIn(it).payDate(it).isAfter(today) }
                            .sumOf { SalaryPrefs.total(it) }
                        (1..4).forEach { qq -> PayRow("${qq}분기", null, SalaryPrefs.quarterTotal(year, qq)) }
                        PayDivider()
                        if (year == today.year) PayRow("지금까지 받은 금액", "월급날이 지난 달", received)
                        PayRow("연간 합계 (예상)", null, SalaryPrefs.yearTotal(year))
                    }
                }
            }

            TextButton(
                onClick = { expanded = false; onSettings() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("월급 설정", color = accent, fontWeight = FontWeight.SemiBold) }
        }
    }

    if (editOverride && wp != null) {
        AmountDialog(
            title = "${ymLabel(month)} 총액",
            initial = rules.override(month) ?: rules.autoTotal(month),
            text = "이 달은 항목 계산 대신 입력한 금액을 씁니다.",
            onDismiss = { editOverride = false },
            onClear = if (rules.override(month) != null) ({ SalaryPrefs.updateOverride(wp.id, month, null); editOverride = false }) else null,
            onConfirm = { SalaryPrefs.updateOverride(wp.id, month, it); editOverride = false }
        )
    }
}

@Composable
private fun SheetCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GroupedCard)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}
