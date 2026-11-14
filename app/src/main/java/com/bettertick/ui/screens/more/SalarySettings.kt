package com.bettertick.ui.screens.more

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.calendar.OptionCard
import com.bettertick.ui.screens.calendar.OptionNavRow
import com.bettertick.ui.screens.calendar.OptionPage
import com.bettertick.ui.screens.calendar.OptionSwitchRow
import com.bettertick.ui.screens.tasks.LeavePrefs
import com.bettertick.ui.screens.tasks.SalaryItem
import com.bettertick.ui.screens.tasks.SalaryPrefs
import com.bettertick.ui.screens.tasks.SalaryRepeat
import com.bettertick.ui.screens.tasks.quarterMonths
import com.bettertick.ui.screens.tasks.won
import com.bettertick.ui.theme.DividerColor
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import java.time.YearMonth

internal val PayRed = Color(0xFFE5483F)

internal fun ymLabel(m: YearMonth) = "${m.year}년 ${m.monthValue}월"

/**
 * 설정 > 일반 > 월급. Each 근무지 has its own 월급날, items (매달 / 분기마다 /
 * 매년 정한 달 / 한 번만, each with an optional 기간) and 달별 총액 직접 입력.
 */
@Composable
fun SalarySettingsScreen(onBack: () -> Unit) {
    var wpId by remember { mutableStateOf(SalaryPrefs.workplaceFor(YearMonth.now())?.id ?: LeavePrefs.currentWorkplace.id) }
    if (LeavePrefs.workplaces.none { it.id == wpId }) wpId = LeavePrefs.currentWorkplace.id
    val wp = LeavePrefs.workplaces.first { it.id == wpId }
    val rules = SalaryPrefs.rulesFor(wpId)

    var editItem by remember { mutableStateOf<SalaryItem?>(null) }
    var editPayday by remember { mutableStateOf(false) }
    var pickOverrideMonth by remember { mutableStateOf(false) }
    var editOverride by remember { mutableStateOf<YearMonth?>(null) }
    var previewYear by remember { mutableStateOf(YearMonth.now().year) }
    var addRecurring by remember { mutableStateOf<Boolean?>(null) }
    var editRecurring by remember { mutableStateOf<com.bettertick.ui.screens.tasks.RecurringMoney?>(null) }
    var askMilitary by remember { mutableStateOf(false) }
    var pickEnlist by remember { mutableStateOf(false) }
    var editRank by remember { mutableStateOf<Int?>(null) } // -1 = 새 계급
    val mil = rules.military

    OptionPage(title = "월급 · 가계부", onBack = onBack) {
        PaySectionLabel("근무지")
        OptionCard {
            OptionChoiceRow(
                title = "설정할 근무지",
                subtitle = wp.rangeLabel,
                value = wpId,
                options = LeavePrefs.workplaces.sortedByDescending { it.start ?: java.time.LocalDate.MIN }.map { it.id },
                label = { id -> LeavePrefs.workplaces.firstOrNull { it.id == id }?.name ?: id },
                onPick = { wpId = it }
            )
            OptionNavRow(
                "월급날",
                value = if (rules.payday >= 31) "매달 말일" else "매달 ${rules.payday}일"
            ) { editPayday = true }
        }

        PaySectionLabel("군 복무 월급")
        OptionCard {
            if (!mil.enabled) {
                OptionNavRow(
                    "군 월급 자동 계산",
                    subtitle = "입대일만 넣으면 이병·일병·상병·병장 월급을 달마다 계산합니다."
                ) { askMilitary = true }
            } else {
                OptionSwitchRow(
                    title = "군 월급 자동 계산",
                    subtitle = "끄면 계급 설정은 남고 월급 계산에서만 빠집니다.",
                    checked = true,
                    onCheckedChange = { on -> SalaryPrefs.updateMilitary(wpId) { it.copy(enabled = on) } }
                )
                OptionNavRow("입대일", value = mil.enlist?.toString() ?: "설정 안 함") { pickEnlist = true }
                mil.discharge?.let { end ->
                    val today = java.time.LocalDate.now()
                    val left = java.time.temporal.ChronoUnit.DAYS.between(today, end)
                    PayInfoRow("전역일 (자동)", "$end" + if (left >= 0) " · D-$left" else " · 전역")
                    val now = mil.rankIn(YearMonth.from(today))
                    val next = mil.rankStarts().firstOrNull { it.second.isAfter(today) }
                    if (now != null) {
                        PayInfoRow(
                            "지금 계급",
                            now.name + (next?.let { " · ${it.second} ${it.first.name} 진급" } ?: "")
                        )
                    }
                }
                PayDivider()
                val starts = mil.rankStarts().map { it.second }
                mil.ranks.forEachIndexed { i, r ->
                    OptionNavRow(
                        r.name,
                        value = won(r.pay),
                        subtitle = "${r.months}개월" + (starts.getOrNull(i)?.let { " · ${it}부터" } ?: "")
                    ) { editRank = i }
                }
                OptionNavRow("계급 추가") { editRank = -1 }
                OptionSwitchRow(
                    title = "입대·전역한 달은 일할 계산",
                    subtitle = "그 달에 복무한 날수만큼만 받습니다.",
                    checked = mil.prorate,
                    onCheckedChange = { on -> SalaryPrefs.updateMilitary(wpId) { it.copy(prorate = on) } }
                )
                OptionNavRow("기본값으로 (육군 18개월 · 2025년 봉급)") {
                    SalaryPrefs.updateMilitary(wpId) { it.copy(ranks = com.bettertick.ui.screens.tasks.DefaultMilitaryRanks) }
                }
            }
        }
        if (mil.enabled) {
            PayHint("입대한 달이 이병 1개월째이고, 다음 계급은 그 개월 수를 채운 뒤 1일에 진급합니다. 기본값(이병 75만·일병 90만·상병 120만·병장 150만, 2·6·6·4개월)은 직접 고칠 수 있어요. 해군·공군·해병대나 진급 누락은 계급별 개월 수를 바꾸세요. 이 근무지 기간은 입대일~전역일로 맞춰집니다.")
        }

        PaySectionLabel("항목 · 지급과 공제")
        OptionCard {
            if (rules.items.isEmpty()) {
                Text(
                    "기본급, 수당, 분기 상여, 명절 상여, 세금·보험 공제를 항목으로 추가하세요.",
                    fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            rules.items.forEach { item ->
                PayRow(
                    title = item.name,
                    subtitle = item.scheduleLabel,
                    amount = item.signed,
                    onClick = { editItem = item }
                )
            }
            OptionNavRow("항목 추가") { editItem = SalaryItem(id = "", name = "", amount = 0) }
        }
        PayHint("매달: 기본급처럼 매달 나오는 돈 (시작·끝 달을 정하면 그 기간만). 분기마다: 분기의 첫째·둘째·셋째 달. 매년 정한 달: 명절 상여처럼 해마다 같은 달. 한 번만: 그 달에만.")

        PaySectionLabel("달별 직접 입력")
        OptionCard {
            rules.monthOverrides.keys.sortedDescending().forEach { m ->
                PayRow(
                    title = ymLabel(m),
                    subtitle = "항목 합계 ${won(rules.autoTotal(m))} 대신",
                    amount = rules.monthOverrides.getValue(m),
                    onClick = { editOverride = m }
                )
            }
            OptionNavRow("달 추가") { pickOverrideMonth = true }
        }
        PayHint("실제로 받은 금액이 다르면 그 달만 총액을 직접 넣으세요. 항목 계산 대신 이 금액을 씁니다.")

        PaySectionLabel("매달 고정 수입·지출 (용돈·월세·구독)")
        OptionCard {
            com.bettertick.ui.screens.tasks.MoneyPrefs.book.recurring.forEach { r ->
                PayRow(
                    title = r.name,
                    subtitle = "${r.category} · ${r.scheduleLabel}",
                    amount = r.signed,
                    onClick = { editRecurring = r }
                )
            }
            OptionNavRow("고정 수입 추가 (용돈 등)") { addRecurring = true }
            OptionNavRow("고정 지출 추가 (월세·통신비 등)") { addRecurring = false }
        }
        PayHint("근무지와 상관없이 매달 들어오고 나가는 돈입니다. 한 번뿐인 수입·지출은 '가계부' 탭에서 바로 추가하세요.")

        PaySectionLabel("미리보기")
        OptionCard {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { previewYear-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 해", tint = TextSecondary)
                }
                Text(
                    "${previewYear}년 · ${wp.name}",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { previewYear++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 해", tint = TextSecondary)
                }
            }
            (1..4).forEach { q ->
                val months = quarterMonths(previewYear, q)
                PayDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${q}분기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(won(months.sumOf { rules.total(it) }), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                months.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = 36.dp, end = 20.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${m.monthValue}월" + if (rules.override(m) != null) " · 직접 입력" else "",
                            fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f)
                        )
                        Text(won(rules.total(m)), fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
            PayDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("연간 합계", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                Text(
                    won((1..12).sumOf { rules.total(YearMonth.of(previewYear, it)) }),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeState.accentColor
                )
            }
        }
        PayHint("이 근무지 규칙만으로 계산한 금액입니다. 근무 기간 밖의 달은 '가계부' 탭에서 0원으로 나옵니다.")
    }

    addRecurring?.let { inc ->
        MoneyDialog(defaultIncome = inc, defaultRepeat = true, onDismiss = { addRecurring = null })
    }
    editRecurring?.let { r ->
        MoneyDialog(recurring = r, onDismiss = { editRecurring = null })
    }
    if (askMilitary) {
        AlertDialog(
            onDismissRequest = { askMilitary = false },
            title = { Text("군 월급 자동 계산") },
            text = {
                Text("군 복무 기간을 근무지 하나로 둡니다. 그 근무지의 기간은 입대일~전역일로 맞춰져요. 지금 '${wp.name}'이(가) 다니는 회사라면 새 근무지를 만드세요.")
            },
            confirmButton = {
                TextButton(onClick = {
                    askMilitary = false
                    val id = LeavePrefs.addWorkplace("군 복무", start = null, end = null).id
                    wpId = id
                    SalaryPrefs.updateMilitary(id) { it.copy(enabled = true) }
                    pickEnlist = true
                }) { Text("새 근무지 '군 복무' 만들기") }
            },
            dismissButton = {
                TextButton(onClick = {
                    askMilitary = false
                    SalaryPrefs.updateMilitary(wpId) { it.copy(enabled = true) }
                    pickEnlist = true
                }) { Text("'${wp.name}'에서 켜기") }
            }
        )
    }
    if (pickEnlist) {
        WorkStartPickerDialog(
            initial = mil.enlist,
            title = "입대일",
            onDismiss = { pickEnlist = false },
            onPick = { day -> SalaryPrefs.updateMilitary(wpId) { it.copy(enlist = day) }; pickEnlist = false }
        )
    }
    editRank?.let { idx ->
        RankDialog(
            initial = mil.ranks.getOrNull(idx),
            canDelete = idx >= 0 && mil.ranks.size > 1,
            onDismiss = { editRank = null },
            onSave = { r ->
                SalaryPrefs.updateMilitary(wpId) { m ->
                    m.copy(ranks = if (idx >= 0) m.ranks.mapIndexed { i, old -> if (i == idx) r else old } else m.ranks + r)
                }
                editRank = null
            },
            onDelete = {
                SalaryPrefs.updateMilitary(wpId) { m -> m.copy(ranks = m.ranks.filterIndexed { i, _ -> i != idx }) }
                editRank = null
            }
        )
    }
    editItem?.let { item ->
        SalaryItemDialog(
            initial = item,
            onDismiss = { editItem = null },
            onSave = {
                SalaryPrefs.saveItem(wpId, if (it.id.isBlank()) it.copy(id = java.util.UUID.randomUUID().toString()) else it)
                editItem = null
            },
            onDelete = if (item.id.isBlank()) null else ({ SalaryPrefs.deleteItem(wpId, item.id); editItem = null })
        )
    }
    if (editPayday) {
        NumberDialog(
            title = "월급날",
            initial = rules.payday.toLong(),
            suffix = "일",
            hint = "31을 넣으면 매달 말일입니다. 짧은 달은 말일에 받습니다.",
            onDismiss = { editPayday = false },
            onConfirm = { SalaryPrefs.updatePayday(wpId, it.toInt()); editPayday = false },
            valid = { it in 1..31 }
        )
    }
    if (pickOverrideMonth) {
        MonthPickerDialog(
            title = "직접 입력할 달",
            initial = YearMonth.now(),
            onDismiss = { pickOverrideMonth = false },
            onPick = { pickOverrideMonth = false; editOverride = it }
        )
    }
    editOverride?.let { m ->
        AmountDialog(
            title = "${ymLabel(m)} 총액",
            initial = rules.override(m) ?: rules.autoTotal(m),
            text = "이 달은 항목 계산 대신 입력한 금액을 씁니다.",
            onDismiss = { editOverride = null },
            onClear = if (rules.override(m) != null) ({ SalaryPrefs.updateOverride(wpId, m, null); editOverride = null }) else null,
            onConfirm = { SalaryPrefs.updateOverride(wpId, m, it); editOverride = null }
        )
    }
}

@Composable
private fun PaySectionLabel(text: String) {
    Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(start = 20.dp))
}

@Composable
private fun PayHint(text: String) {
    Text(
        text, fontSize = 13.sp, color = TextTertiary, lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
internal fun PayDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(0.5.dp).background(DividerColor))
}

@Composable
internal fun PayRow(title: String, subtitle: String?, amount: Long, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = 15.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        Text(
            if (amount < 0) "−" + won(-amount) else won(amount),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = if (amount < 0) PayRed else TextPrimary
        )
    }
}

/** Small selectable pill used in the dialogs. */
@Composable
private fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = ThemeState.accentColor
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else TextTertiary.copy(alpha = 0.12f))
            .clickable { onClick() }
            .heightIn(min = 36.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 13.sp, maxLines = 1,
            color = if (selected) accent else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DialogLabel(text: String) {
    Text(text, fontSize = 12.sp, color = TextTertiary, modifier = Modifier.padding(top = 4.dp))
}

private fun digitsOnly(s: String) = s.filter { it.isDigit() }.take(12)

@Composable
private fun SalaryItemDialog(
    initial: SalaryItem,
    onDismiss: () -> Unit,
    onSave: (SalaryItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial.name) }
    var amountText by remember { mutableStateOf(if (initial.amount > 0) initial.amount.toString() else "") }
    var deduction by remember { mutableStateOf(initial.deduction) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var quarterMonth by remember { mutableStateOf(initial.quarterMonth) }
    var months by remember { mutableStateOf(initial.months) }
    var from by remember { mutableStateOf(initial.from) }
    var to by remember { mutableStateOf(initial.to) }
    var picking by remember { mutableStateOf<String?>(null) } // "from" | "to"

    val amount = amountText.toLongOrNull() ?: 0L
    val f = from
    val t = to
    val rangeOk = f == null || t == null || !t.isBefore(f)
    val valid = name.isNotBlank() && amount > 0 && rangeOk &&
        (repeat != SalaryRepeat.ONCE || from != null) &&
        (repeat != SalaryRepeat.YEARLY || months.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "항목 추가" else "항목 수정") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(20) },
                    singleLine = true, label = { Text("이름") },
                    placeholder = { Text("기본급, 식대, 분기 상여 …") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = digitsOnly(it) },
                    singleLine = true, label = { Text("금액 (원)") },
                    supportingText = { if (amount > 0) Text(won(amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("지급", !deduction, Modifier.weight(1f)) { deduction = false }
                    Pill("공제 (세금·보험 등)", deduction, Modifier.weight(1f)) { deduction = true }
                }
                DialogLabel("언제 받나요")
                SalaryRepeat.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { r -> Pill(r.label, repeat == r, Modifier.weight(1f)) { repeat = r } }
                    }
                }
                when (repeat) {
                    SalaryRepeat.QUARTERLY -> {
                        DialogLabel("분기의 몇째 달")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1 to "1·4·7·10월", 2 to "2·5·8·11월", 3 to "3·6·9·12월").forEach { (q, l) ->
                                Pill(l, quarterMonth == q, Modifier.weight(1f)) { quarterMonth = q }
                            }
                        }
                    }
                    SalaryRepeat.YEARLY -> {
                        DialogLabel("받는 달 (여러 개 선택)")
                        (1..12).chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { m ->
                                    Pill("${m}월", m in months, Modifier.weight(1f)) {
                                        months = if (m in months) months - m else months + m
                                    }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
                if (repeat == SalaryRepeat.ONCE) {
                    DialogLabel("받는 달")
                    Pill(from?.let { ymLabel(it) } ?: "달 고르기", from != null, Modifier.fillMaxWidth()) { picking = "from" }
                } else {
                    DialogLabel("기간 (비워 두면 계속)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(from?.let { ymLabel(it) } ?: "처음부터", from != null, Modifier.weight(1f)) { picking = "from" }
                        Text("~", color = TextTertiary)
                        Pill(to?.let { ymLabel(it) } ?: "계속", to != null, Modifier.weight(1f)) { picking = "to" }
                    }
                    if (!rangeOk) Text("끝 달이 시작 달보다 앞입니다.", fontSize = 12.sp, color = PayRed)
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "항목 삭제", color = PayRed, fontSize = 14.sp,
                        modifier = Modifier.clickable { onDelete() }.padding(vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    initial.copy(
                        name = name.trim(), amount = amount, deduction = deduction, repeat = repeat,
                        quarterMonth = quarterMonth, months = months,
                        from = from, to = if (repeat == SalaryRepeat.ONCE) null else to
                    )
                )
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )

    picking?.let { which ->
        MonthPickerDialog(
            title = if (which == "from") (if (repeat == SalaryRepeat.ONCE) "받는 달" else "시작 달") else "끝 달",
            initial = (if (which == "from") from else to) ?: YearMonth.now(),
            clearLabel = if (repeat == SalaryRepeat.ONCE) null else if (which == "from") "처음부터" else "계속",
            onClear = { if (which == "from") from = null else to = null; picking = null },
            onDismiss = { picking = null },
            onPick = { if (which == "from") from = it else to = it; picking = null }
        )
    }
}

/** Year arrows + 12 month cells. */
@Composable
internal fun MonthPickerDialog(
    title: String,
    initial: YearMonth,
    onDismiss: () -> Unit,
    onPick: (YearMonth) -> Unit,
    clearLabel: String? = null,
    onClear: () -> Unit = {}
) {
    var year by remember { mutableStateOf(initial.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 해", tint = TextSecondary)
                    }
                    Text(
                        "${year}년", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { year++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 해", tint = TextSecondary)
                    }
                }
                (1..12).chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { m ->
                            Pill("${m}월", year == initial.year && m == initial.monthValue, Modifier.weight(1f)) {
                                onPick(YearMonth.of(year, m))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (clearLabel != null) TextButton(onClick = onClear) { Text(clearLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

/** Amount in 원, with a live "1,234,000원" preview. */
@Composable
internal fun AmountDialog(
    title: String,
    initial: Long,
    text: String?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onClear: (() -> Unit)? = null
) {
    var value by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    val amount = value.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (text != null) Text(text, fontSize = 13.sp, color = TextSecondary)
                OutlinedTextField(
                    value = value, onValueChange = { value = digitsOnly(it) },
                    singleLine = true, label = { Text("금액 (원)") },
                    supportingText = { amount?.let { Text(won(it)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (onClear != null) {
                    Text(
                        "자동으로 (항목 계산)", color = ThemeState.accentColor, fontSize = 14.sp,
                        modifier = Modifier.clickable { onClear() }.padding(vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = amount != null, onClick = { amount?.let(onConfirm) }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun NumberDialog(
    title: String,
    initial: Long,
    suffix: String,
    hint: String?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    valid: (Long) -> Boolean
) {
    var value by remember { mutableStateOf(initial.toString()) }
    val n = value.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value, onValueChange = { value = it.filter { c -> c.isDigit() }.take(2) },
                    singleLine = true, suffix = { Text(suffix) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (hint != null) Text(hint, fontSize = 12.sp, color = TextTertiary)
            }
        },
        confirmButton = {
            TextButton(enabled = n != null && valid(n), onClick = { n?.let(onConfirm) }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

/**
 * 수입·지출 추가/수정. One dialog for a one-off entry and for a 매달 고정
 * item (용돈, 월세 …); switching "매달 반복" converts between the two.
 */
@Composable
internal fun MoneyDialog(
    entry: com.bettertick.ui.screens.tasks.MoneyEntry? = null,
    recurring: com.bettertick.ui.screens.tasks.RecurringMoney? = null,
    defaultIncome: Boolean = false,
    defaultDate: java.time.LocalDate = java.time.LocalDate.now(),
    defaultRepeat: Boolean = false,
    onDismiss: () -> Unit
) {
    val editing = entry != null || recurring != null
    var income by remember { mutableStateOf(entry?.income ?: recurring?.income ?: defaultIncome) }
    var amountText by remember {
        mutableStateOf((entry?.amount ?: recurring?.amount)?.takeIf { it > 0 }?.toString() ?: "")
    }
    var category by remember { mutableStateOf(entry?.category ?: recurring?.category ?: if (defaultIncome) "용돈" else "식비") }
    var memo by remember { mutableStateOf(entry?.memo ?: recurring?.name ?: "") }
    var monthly by remember { mutableStateOf(recurring != null || (entry == null && defaultRepeat)) }
    var date by remember { mutableStateOf(entry?.date ?: defaultDate) }
    var dayText by remember { mutableStateOf((recurring?.day ?: defaultDate.dayOfMonth).toString()) }
    var from by remember { mutableStateOf(recurring?.from) }
    var to by remember { mutableStateOf(recurring?.to) }
    var pickDate by remember { mutableStateOf(false) }
    var pickMonth by remember { mutableStateOf<String?>(null) }

    val categories = if (income) com.bettertick.ui.screens.tasks.IncomeCategories else com.bettertick.ui.screens.tasks.ExpenseCategories
    if (category !in categories) category = categories.first()
    val amount = amountText.toLongOrNull() ?: 0L
    val day = dayText.toIntOrNull()
    val f = from
    val t = to
    val valid = amount > 0 && (!monthly || (day != null && day in 1..31 && (f == null || t == null || !t.isBefore(f))))

    fun save() {
        if (monthly) {
            entry?.let { com.bettertick.ui.screens.tasks.MoneyPrefs.deleteEntry(it.id) }
            val base = recurring ?: com.bettertick.ui.screens.tasks.RecurringMoney(name = "", amount = 0, income = income, category = category)
            com.bettertick.ui.screens.tasks.MoneyPrefs.saveRecurring(
                base.copy(
                    name = memo.trim().ifBlank { category }, amount = amount, income = income,
                    category = category, day = day ?: 1, from = from, to = to
                )
            )
        } else {
            recurring?.let { com.bettertick.ui.screens.tasks.MoneyPrefs.deleteRecurring(it.id) }
            val base = entry ?: com.bettertick.ui.screens.tasks.MoneyEntry(date = date, amount = 0, income = income, category = category)
            com.bettertick.ui.screens.tasks.MoneyPrefs.saveEntry(
                base.copy(date = date, amount = amount, income = income, category = category, memo = memo.trim())
            )
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) "내역 수정" else if (income) "수입 추가" else "지출 추가") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("지출", !income, Modifier.weight(1f)) { income = false }
                    Pill("수입", income, Modifier.weight(1f)) { income = true }
                }
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = digitsOnly(it) },
                    singleLine = true, label = { Text("금액 (원)") },
                    supportingText = { if (amount > 0) Text(won(amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                DialogLabel("분류")
                categories.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { c -> Pill(c, category == c, Modifier.weight(1f)) { category = c } }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                OutlinedTextField(
                    value = memo, onValueChange = { memo = it.take(30) },
                    singleLine = true, label = { Text(if (monthly) "이름" else "메모 (선택)") },
                    placeholder = { Text(if (income) "엄마 용돈, 중고 판매 …" else "점심, 택시 …") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("한 번", !monthly, Modifier.weight(1f)) { monthly = false }
                    Pill("매달 반복", monthly, Modifier.weight(1f)) { monthly = true }
                }
                if (!monthly) {
                    DialogLabel("날짜")
                    Pill(
                        "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 (${"월화수목금토일"[date.dayOfWeek.value - 1]})",
                        true, Modifier.fillMaxWidth()
                    ) { pickDate = true }
                } else {
                    OutlinedTextField(
                        value = dayText, onValueChange = { dayText = it.filter { c -> c.isDigit() }.take(2) },
                        singleLine = true, label = { Text("매달 며칠") }, suffix = { Text("일") },
                        supportingText = { Text("31 = 말일") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DialogLabel("기간 (비워 두면 계속)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(from?.let { ymLabel(it) } ?: "처음부터", from != null, Modifier.weight(1f)) { pickMonth = "from" }
                        Text("~", color = TextTertiary)
                        Pill(to?.let { ymLabel(it) } ?: "계속", to != null, Modifier.weight(1f)) { pickMonth = "to" }
                    }
                }
                if (editing) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "삭제", color = PayRed, fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                entry?.let { com.bettertick.ui.screens.tasks.MoneyPrefs.deleteEntry(it.id) }
                                recurring?.let { com.bettertick.ui.screens.tasks.MoneyPrefs.deleteRecurring(it.id) }
                                onDismiss()
                            }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { save() }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )

    if (pickDate) {
        WorkStartPickerDialog(
            initial = date,
            title = "날짜",
            onDismiss = { pickDate = false },
            onPick = { date = it; pickDate = false }
        )
    }
    pickMonth?.let { which ->
        MonthPickerDialog(
            title = if (which == "from") "시작 달" else "끝 달",
            initial = (if (which == "from") from else to) ?: YearMonth.now(),
            clearLabel = if (which == "from") "처음부터" else "계속",
            onClear = { if (which == "from") from = null else to = null; pickMonth = null },
            onDismiss = { pickMonth = null },
            onPick = { if (which == "from") from = it else to = it; pickMonth = null }
        )
    }
}

@Composable
private fun PayInfoRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun RankDialog(
    initial: com.bettertick.ui.screens.tasks.MilitaryRank?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (com.bettertick.ui.screens.tasks.MilitaryRank) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var monthsText by remember { mutableStateOf(initial?.months?.toString() ?: "") }
    var payText by remember { mutableStateOf(initial?.pay?.takeIf { it > 0 }?.toString() ?: "") }
    val months = monthsText.toIntOrNull()
    val pay = payText.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "계급 추가" else "${initial.name} 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(10) },
                    singleLine = true, label = { Text("계급") }, placeholder = { Text("병장") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthsText, onValueChange = { monthsText = it.filter { c -> c.isDigit() }.take(2) },
                    singleLine = true, label = { Text("기간") }, suffix = { Text("개월") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payText, onValueChange = { payText = digitsOnly(it) },
                    singleLine = true, label = { Text("한 달 월급 (원)") },
                    supportingText = { pay?.let { Text(won(it)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (canDelete) {
                    Text(
                        "계급 삭제", color = PayRed, fontSize = 14.sp,
                        modifier = Modifier.clickable { onDelete() }.padding(vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && months != null && months in 1..60 && pay != null,
                onClick = { onSave(com.bettertick.ui.screens.tasks.MilitaryRank(name.trim(), months!!, pay!!)) }
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
