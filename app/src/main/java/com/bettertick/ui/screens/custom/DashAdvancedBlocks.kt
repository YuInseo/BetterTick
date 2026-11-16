package com.bettertick.ui.screens.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.screens.tasks.ExpenseCategories
import com.bettertick.ui.screens.tasks.IncomeCategories
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary

// ───────────────────────── 값 블록 ─────────────────────────

/** Full value of a gauge: the chosen variable, a fixed number, or a sensible default. */
private fun gaugeTarget(cfg: ValueConfig, ctx: DashContext, kind: VarKind?): Double? {
    if (cfg.targetVariable.isNotBlank()) DashVars.value(cfg.targetVariable, ctx).number?.let { return it }
    if (cfg.target > 0) return cfg.target
    if (kind == VarKind.PERCENT) return 100.0
    val auto = when (cfg.variable) {
        "today.done", "today.left" -> "today.total"
        "week.done" -> "week.total"
        "habit.done" -> "habit.total"
        "money.expense" -> "money.income"
        "date.month_left" -> null
        else -> null
    }
    return auto?.let { DashVars.value(it, ctx).number }
}

private fun ruleColor(cfg: ValueConfig, number: Double?, accent: Color): Color {
    if (number != null) {
        cfg.rules.firstOrNull { r ->
            when (r.op) {
                ">=" -> number >= r.threshold
                "<=" -> number <= r.threshold
                ">" -> number > r.threshold
                "<" -> number < r.threshold
                else -> number == r.threshold
            }
        }?.let { return dashColor(it.color, accent) }
    }
    return dashColor(cfg.color, accent)
}

@Composable
internal fun ValueBlockView(block: DashBlock, ctx: DashContext, fill: Boolean, onNavigate: (String) -> Unit) {
    val cfg = remember(block.text) { ValueConfig.decode(block.text) }
    val v = DashVars.value(cfg.variable, ctx)
    val variable = DashVars.find(cfg.variable, ctx.lists)
    val accent = MaterialTheme.colorScheme.primary
    val color = ruleColor(cfg, v.number, accent)
    val title = block.title.ifBlank { variable?.label ?: "값" }
    val shown = cfg.prefix + v.text + cfg.suffix
    val target = gaugeTarget(cfg, ctx, variable?.kind)
    val progress = if (v.number != null && target != null && target > 0) (v.number / target).toFloat().coerceIn(0f, 1f) else null

    Column(
        Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)
            .clip(RoundedCornerShape(16.dp)).background(blockBackground())
            .then(if (cfg.tapTarget.isNotBlank()) Modifier.clickable { onNavigate(cfg.tapTarget) } else Modifier)
            .padding(16.dp)
    ) {
        when (cfg.display) {
            ValueDisplay.TEXT -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextSecondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(shown, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            ValueDisplay.CHIP -> Column {
                Text(title, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(shown, color = color, fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp))
            }
            ValueDisplay.RING -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    val track = DarkSurfaceVariant
                    Canvas(Modifier.size(64.dp)) {
                        val stroke = 7.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                        drawArc(color, -90f, 360f * (progress ?: 0f), false, Offset(inset, inset), arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Text(progress?.let { "${Math.round(it * 100)}%" } ?: "-", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(shown, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
                    target?.let { Text("목표 " + DashVars.formatNumber(it, variable?.kind ?: VarKind.NUMBER), color = TextSecondary, fontSize = 12.sp) }
                }
            }
            ValueDisplay.TREND -> Column {
                // The same variable worked out for each of the last 7 days (as if each were "today").
                val days = (6L downTo 0L).map { ctx.today.minusDays(it) }
                val values = days.map { d ->
                    DashVars.value(cfg.variable, DashContext(ctx.tasks, ctx.lists, ctx.habits, ctx.diary, d)).number
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(title, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(shown, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(10.dp))
                if (values.all { it == null }) {
                    Text("이 변수는 날짜별 값이 없어요", color = TextSecondary, fontSize = 12.sp)
                } else {
                    val max = values.filterNotNull().maxOrNull()?.takeIf { it > 0 } ?: 1.0
                    Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom) {
                        values.forEachIndexed { i, v ->
                            val frac = ((v ?: 0.0) / max).toFloat().coerceIn(0.04f, 1f)
                            Box(Modifier.weight(1f).fillMaxHeight(frac).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (i == values.lastIndex) color else color.copy(alpha = 0.35f)))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        days.forEachIndexed { i, d ->
                            Text(if (i == days.lastIndex) "오늘" else d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.KOREAN),
                                fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            ValueDisplay.BAR -> Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(title, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(shown, color = color, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress ?: 0f }, color = color, trackColor = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                target?.let {
                    Text("목표 " + DashVars.formatNumber(it, variable?.kind ?: VarKind.NUMBER), color = TextSecondary,
                        fontSize = 11.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                }
            }
            else -> Column {
                Text(title, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(shown, color = color, fontWeight = FontWeight.Bold, fontSize = 30.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ───────────────────────── 텍스트 템플릿 ─────────────────────────

@Composable
internal fun TemplateBlockView(block: DashBlock, ctx: DashContext, fill: Boolean) {
    val cfg = remember(block.text) { TemplateConfig.decode(block.text) }
    val size = when (cfg.size) { "small" -> 14.sp; "large" -> 24.sp; else -> 18.sp }
    Column(
        Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)
            .clip(RoundedCornerShape(16.dp)).background(blockBackground()).padding(16.dp)
    ) {
        if (block.title.isNotBlank()) {
            Text(block.title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
        }
        Text(DashVars.fill(cfg.text, ctx), fontSize = size, lineHeight = size * 1.35f,
            fontWeight = if (cfg.size == "large") FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = if (cfg.center) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth())
    }
}

// ───────────────────────── 매크로 버튼 ─────────────────────────

@Composable
internal fun MacroBlockView(block: DashBlock, fill: Boolean, onRun: (MacroConfig) -> Unit) {
    val cfg = remember(block.text) { MacroConfig.decode(block.text) }
    val color = dashColor(cfg.color, MaterialTheme.colorScheme.primary)
    var confirm by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)
            .clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.16f))
            .clickable { if (cfg.steps.isEmpty()) return@clickable; if (cfg.confirm) confirm = true else onRun(cfg) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(cfg.label, color = color, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val auto = cfg.triggers.filter { it.enabled }.joinToString(", ") { t ->
                if (t.type == TriggerType.TIME) "매일".takeIf { t.days.isBlank() }.orEmpty().let { d ->
                    (if (t.days.isBlank()) d else t.days.map { c -> "월화수목금토일"[c - '1'] }.joinToString("")) + " " + t.time
                } else TriggerType.label(t.type).removeSuffix(" 때").removeSuffix("에")
            }
            Text(if (cfg.steps.isEmpty()) "편집 › 블록 설정에서 단계를 추가하세요"
                else (if (auto.isNotEmpty()) "자동: $auto · " else "") + cfg.steps.joinToString(" → ") { MacroType.label(it.type) },
                color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("'${cfg.label}' 실행할까요?") },
            text = { Text(cfg.steps.mapIndexed { i, s -> "${i + 1}. ${MacroType.label(s.type)}" }.joinToString("\n")) },
            confirmButton = { TextButton(onClick = { confirm = false; onRun(cfg) }) { Text("실행") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소") } }
        )
    }
}

// ───────────────────────── 설정 UI ─────────────────────────

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

/** A plain input box that keeps its own text so typing never jumps. */
@Composable
internal fun DashField(
    initial: String,
    placeholder: String,
    key: Any,
    singleLine: Boolean = true,
    number: Boolean = false,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    var text by remember(key) { mutableStateOf(initial) }
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant).padding(12.dp)) {
        if (text.isEmpty()) Text(placeholder, color = TextSecondary)
        BasicTextField(
            value = text,
            onValueChange = { v -> text = if (number) v.filter { it.isDigit() || it == '.' || it == '-' } else v; onChange(text) },
            singleLine = singleLine,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Chips(choices: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { (value, label) ->
            val on = value == selected
            Text(label, fontSize = 13.sp,
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else DarkSurfaceVariant)
                    .clickable { onPick(value) }.padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

@Composable
private fun ColorDots(selected: String, onPick: (String) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DashColors.forEach { (key, _) ->
            val c = dashColor(key, accent)
            Box(Modifier.size(30.dp).clip(CircleShape).background(c)
                .then(if (key == selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
                .clickable { onPick(key) })
        }
    }
}

/** Picks a variable, grouped (할 일 · 근무 · 습관 · 가계부 …), with its current value beside it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VariablePickerSheet(ctx: DashContext, onDismiss: () -> Unit, onPick: (DashVar) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
            Text("변수 고르기", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            var query by remember { mutableStateOf("") }
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant).padding(12.dp)) {
                if (query.isEmpty()) Text("검색 (예: 근무, 지출, 습관)", color = TextSecondary)
                BasicTextField(value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
            }
            val q = query.trim()
            val found = DashVars.all(ctx.lists).filter { v ->
                q.isEmpty() || v.label.contains(q, ignoreCase = true) || v.key.contains(q, ignoreCase = true) || v.group.contains(q)
            }
            if (found.isEmpty()) Text("맞는 변수가 없어요", color = TextSecondary, modifier = Modifier.padding(20.dp))
            found.groupBy { it.group }.forEach { (group, vars) ->
                Text(group, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))
                vars.forEach { v ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(v) }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v.label, color = MaterialTheme.colorScheme.onBackground)
                            Text("{${v.key}}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text(DashVars.value(v.key, ctx).text, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

private val RuleOps = listOf(">=" to "이상", "<=" to "이하", ">" to "초과", "<" to "미만", "==" to "같음")

@Composable
internal fun ValueSettings(tabId: String, block: DashBlock, ctx: DashContext) {
    val cfg = ValueConfig.decode(block.text)
    fun save(c: ValueConfig) = updateDashBlock(tabId, block.id) { it.copy(text = c.encode()) }
    var picking by remember { mutableStateOf<String?>(null) } // "variable" | "target"

    Label("연결할 변수")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)
        .clickable { picking = "variable" }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(DashVars.find(cfg.variable, ctx.lists)?.label ?: cfg.variable, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f))
        Text("지금 " + DashVars.value(cfg.variable, ctx).text, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
    }

    Label("표시 방식")
    Chips(ValueDisplay.all, cfg.display) { save(cfg.copy(display = it)) }

    Label("앞 · 뒤에 붙일 글자")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DashField(cfg.prefix, "앞 (예: 남은 )", "pre${block.id}", modifier = Modifier.weight(1f)) { save(ValueConfig.decode(currentText(tabId, block.id)).copy(prefix = it)) }
        DashField(cfg.suffix, "뒤 (예: 개)", "suf${block.id}", modifier = Modifier.weight(1f)) { save(ValueConfig.decode(currentText(tabId, block.id)).copy(suffix = it)) }
    }

    if (cfg.display == ValueDisplay.BAR || cfg.display == ValueDisplay.RING) {
        Label("목표값 (게이지 가득)")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashField(if (cfg.target > 0) DashVars.formatNumber(cfg.target, VarKind.NUMBER).replace(",", "") else "",
                "자동", "tgt${block.id}", number = true, modifier = Modifier.weight(1f)) { v ->
                save(ValueConfig.decode(currentText(tabId, block.id)).copy(target = v.toDoubleOrNull() ?: 0.0))
            }
            Text(if (cfg.targetVariable.isBlank()) "변수로" else (DashVars.find(cfg.targetVariable, ctx.lists)?.label ?: "변수"),
                color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)
                    .clickable { picking = "target" }.padding(12.dp))
            if (cfg.targetVariable.isNotBlank()) {
                Icon(Icons.Outlined.Close, "변수 해제", tint = TextSecondary,
                    modifier = Modifier.size(20.dp).clickable { save(cfg.copy(targetVariable = "")) })
            }
        }
    }

    Label("색")
    ColorDots(cfg.color) { save(cfg.copy(color = it)) }

    Label("조건부 색 (위에서부터 처음 맞는 규칙)")
    cfg.rules.forEachIndexed { i, rule ->
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f)).padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("값이", color = TextSecondary, fontSize = 13.sp)
                DashField(DashVars.formatNumber(rule.threshold, VarKind.NUMBER).replace(",", ""), "0",
                    "rule${block.id}$i", number = true, modifier = Modifier.weight(1f)) { v ->
                    val c = ValueConfig.decode(currentText(tabId, block.id))
                    save(c.copy(rules = c.rules.mapIndexed { j, r -> if (j == i) r.copy(threshold = v.toDoubleOrNull() ?: 0.0) else r }))
                }
                Icon(Icons.Outlined.Close, "규칙 삭제", tint = OverdueRed, modifier = Modifier.size(20.dp)
                    .clickable { save(cfg.copy(rules = cfg.rules.filterIndexed { j, _ -> j != i })) })
            }
            Spacer(Modifier.height(6.dp))
            Chips(RuleOps, rule.op) { op -> save(cfg.copy(rules = cfg.rules.mapIndexed { j, r -> if (j == i) r.copy(op = op) else r })) }
            Spacer(Modifier.height(6.dp))
            ColorDots(rule.color) { col -> save(cfg.copy(rules = cfg.rules.mapIndexed { j, r -> if (j == i) r.copy(color = col) else r })) }
        }
    }
    Text("+ 규칙 추가", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { save(cfg.copy(rules = cfg.rules + ColorRule())) }.padding(8.dp))

    Label("누르면 이동")
    Chips(listOf("" to "없음") + ShortcutTargets, cfg.tapTarget) { save(cfg.copy(tapTarget = it)) }

    picking?.let { which ->
        VariablePickerSheet(ctx, onDismiss = { picking = null }) { v ->
            picking = null
            val c = ValueConfig.decode(currentText(tabId, block.id))
            save(if (which == "variable") c.copy(variable = v.key) else c.copy(targetVariable = v.key))
        }
    }
}

@Composable
internal fun TemplateSettings(tabId: String, block: DashBlock, ctx: DashContext) {
    val cfg = TemplateConfig.decode(block.text)
    fun save(c: TemplateConfig) = updateDashBlock(tabId, block.id) { it.copy(text = c.encode()) }
    var picking by remember { mutableStateOf(false) }
    var fieldKey by remember { mutableStateOf(0) }

    Label("문장 · {변수}는 지금 값으로 바뀌어요")
    DashField(cfg.text, "예: 오늘 {shift.today}, 할 일 {today.left}개", "tpl${block.id}$fieldKey", singleLine = false) {
        save(TemplateConfig.decode(currentText(tabId, block.id)).copy(text = it))
    }
    Text("+ 변수 넣기", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { picking = true }.padding(8.dp))
    Label("미리보기")
    Text(DashVars.fill(cfg.text, ctx), color = MaterialTheme.colorScheme.onBackground)
    Label("글자 크기")
    Chips(listOf("small" to "작게", "medium" to "보통", "large" to "크게"), cfg.size) { save(cfg.copy(size = it)) }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("가운데 정렬", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Switch(checked = cfg.center, onCheckedChange = { save(cfg.copy(center = it)) })
    }
    if (picking) {
        VariablePickerSheet(ctx, onDismiss = { picking = false }) { v ->
            picking = false
            val c = TemplateConfig.decode(currentText(tabId, block.id))
            save(c.copy(text = c.text + "{${v.key}}"))
            fieldKey++ // show the inserted variable in the box
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MacroSettings(tabId: String, block: DashBlock, ctx: DashContext) {
    val cfg = MacroConfig.decode(block.text)
    fun latest() = MacroConfig.decode(currentText(tabId, block.id))
    fun save(c: MacroConfig) = updateDashBlock(tabId, block.id) { it.copy(text = c.encode()) }
    fun saveStep(id: String, change: (MacroStep) -> MacroStep) {
        val c = latest(); save(c.copy(steps = c.steps.map { if (it.id == id) change(it) else it }))
    }
    var addingStep by remember { mutableStateOf(false) }
    val memoBlocks = CustomTabsPrefs.tab(tabId)?.blocks.orEmpty().filter { it.type == DashBlockType.MEMO }

    val context = androidx.compose.ui.platform.LocalContext.current
    // Try it now: same run as the button (실행 조건 included), so what you see is what an event does.
    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
        .clickable(enabled = cfg.steps.isNotEmpty()) { MacroEngine.fire(context, tabId, block.id, latest(), "테스트 실행", ctx) }
        .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("지금 실행해 보기", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(if (cfg.steps.isEmpty()) "단계를 먼저 추가하세요" else "실제로 실행돼요 (할 일·가계부에 기록됨)",
                color = TextSecondary, fontSize = 12.sp)
        }
    }
    MacroEngine.nextTimedRun(cfg)?.let {
        Text("다음 자동 실행: $it", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }

    Label("버튼 이름")
    DashField(cfg.label, "매크로 실행", "mlabel${block.id}") { v -> save(latest().copy(label = v.ifBlank { "매크로 실행" })) }
    Label("색")
    ColorDots(cfg.color) { save(cfg.copy(color = it)) }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("실행 전에 확인", color = MaterialTheme.colorScheme.onBackground)
            Text("실수로 눌러도 바로 실행되지 않아요", color = TextSecondary, fontSize = 12.sp)
        }
        Switch(checked = cfg.confirm, onCheckedChange = { save(cfg.copy(confirm = it)) })
    }

    MacroTriggerSettings(tabId, block, ctx, cfg, { latest() }, { save(it) })

    Label("단계 · 위에서부터 차례로 실행 (문구에 {변수} 사용 가능)")
    cfg.steps.forEachIndexed { i, step ->
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}. ${MacroType.label(step.type)}", color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.KeyboardArrowUp, "위로", tint = if (i > 0) TextSecondary else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp).clickable(enabled = i > 0) {
                        val c = latest(); val l = c.steps.toMutableList(); l.add(i - 1, l.removeAt(i)); save(c.copy(steps = l))
                    })
                Icon(Icons.Outlined.KeyboardArrowDown, "아래로",
                    tint = if (i < cfg.steps.lastIndex) TextSecondary else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp).clickable(enabled = i < cfg.steps.lastIndex) {
                        val c = latest(); val l = c.steps.toMutableList(); l.add(i + 1, l.removeAt(i)); save(c.copy(steps = l))
                    })
                Icon(Icons.Outlined.Close, "단계 삭제", tint = OverdueRed, modifier = Modifier.size(24.dp).clickable {
                    val c = latest(); save(c.copy(steps = c.steps.filterNot { it.id == step.id }))
                })
            }
            when (step.type) {
                MacroType.ADD_TASK -> {
                    Label("제목")
                    DashField(step.text, "예: 물 마시기 ({time.now})", "t${step.id}") { v -> saveStep(step.id) { it.copy(text = v) } }
                    Label("날짜")
                    Chips(listOf("0" to "오늘", "1" to "내일", "2" to "모레", "7" to "일주일 뒤"), step.amount.toString()) { v ->
                        saveStep(step.id) { it.copy(amount = v.toLong()) }
                    }
                    Label("시각 (HH:mm, 비우면 시간 없음)")
                    DashField(step.time, "예: 09:00", "tm${step.id}") { v -> saveStep(step.id) { it.copy(time = v.trim()) } }
                    Label("목록")
                    Chips(listOf("" to "기본함") + ctx.lists.map { it.id to it.name }, step.target) { v -> saveStep(step.id) { it.copy(target = v) } }
                }
                MacroType.ADD_MONEY -> {
                    Label("구분")
                    Chips(listOf("false" to "지출", "true" to "수입"), step.flag.toString()) { v ->
                        saveStep(step.id) { it.copy(flag = v == "true", target = "") }
                    }
                    Label("금액 (원)")
                    DashField(if (step.amount > 0) step.amount.toString() else "", "예: 4500", "a${step.id}", number = true) { v ->
                        saveStep(step.id) { it.copy(amount = v.filter { c -> c.isDigit() }.toLongOrNull() ?: 0) }
                    }
                    Label("분류")
                    Chips((if (step.flag) IncomeCategories else ExpenseCategories).map { it to it }, step.target) { v ->
                        saveStep(step.id) { it.copy(target = v) }
                    }
                    Label("메모")
                    DashField(step.text, "예: 출근길 커피", "m${step.id}") { v -> saveStep(step.id) { it.copy(text = v) } }
                }
                MacroType.APPEND_MEMO -> {
                    Label("기록할 문구 (앞에 시각이 붙어요)")
                    DashField(step.text, "예: 출근 · 오늘 {shift.today}", "n${step.id}") { v -> saveStep(step.id) { it.copy(text = v) } }
                    Label("메모 블록")
                    if (memoBlocks.isEmpty()) Text("이 탭에 메모 블록을 먼저 추가하세요", color = OverdueRed, fontSize = 12.sp)
                    else Chips(memoBlocks.mapIndexed { j, b -> b.id to (b.title.ifBlank { "메모 ${j + 1}" }) },
                        step.target.ifBlank { memoBlocks.first().id }) { v -> saveStep(step.id) { it.copy(target = v) } }
                }
                MacroType.TOAST -> {
                    Label("메시지")
                    DashField(step.text, "예: 오늘 남은 할 일 {today.left}개", "o${step.id}") { v -> saveStep(step.id) { it.copy(text = v) } }
                }
                MacroType.NAVIGATE -> {
                    Label("이동할 화면 (마지막에 이동)")
                    Chips(ShortcutTargets, step.target) { v -> saveStep(step.id) { it.copy(target = v) } }
                }
                else -> Text(
                    if (step.type == MacroType.COMPLETE_TODAY) "오늘 날짜의 남은 할 일을 전부 완료로 표시해요"
                    else "오늘 할 습관 중 안 한 것을 전부 체크해요",
                    color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { addingStep = true }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("단계 추가", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }

    if (addingStep) {
        ModalBottomSheet(onDismissRequest = { addingStep = false }, containerColor = DarkSurface) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
                Text("어떤 동작을 넣을까요?", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                MacroType.all.forEach { (type, label) ->
                    Text(label, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth().clickable {
                            addingStep = false
                            val c = latest()
                            val step = when (type) {
                                MacroType.NAVIGATE -> MacroStep(type, target = ShortcutTargets.first().first)
                                MacroType.ADD_MONEY -> MacroStep(type, target = ExpenseCategories.first())
                                else -> MacroStep(type)
                            }
                            save(c.copy(steps = c.steps + step))
                        }.padding(horizontal = 20.dp, vertical = 14.dp))
                }
            }
        }
    }
}

private val WeekdayNames = listOf("1" to "월", "2" to "화", "3" to "수", "4" to "목", "5" to "금", "6" to "토", "7" to "일")

/** "언제 실행할까요" (events) and "실행 조건" of a macro, plus its last run. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroTriggerSettings(
    tabId: String,
    block: DashBlock,
    ctx: DashContext,
    cfg: MacroConfig,
    latest: () -> MacroConfig,
    save: (MacroConfig) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun saveTrigger(id: String, change: (MacroTrigger) -> MacroTrigger) {
        val c = latest(); save(c.copy(triggers = c.triggers.map { if (it.id == id) change(it) else it }))
    }
    fun saveCondition(id: String, change: (MacroCondition) -> MacroCondition) {
        val c = latest(); save(c.copy(conditions = c.conditions.map { if (it.id == id) change(it) else it }))
    }
    var addingTrigger by remember { mutableStateOf(false) }
    var pickVarFor by remember { mutableStateOf<String?>(null) } // "t:<id>" | "c:<id>"

    Label("언제 실행할까요 · 버튼은 항상 동작해요")
    if (cfg.triggers.isEmpty()) Text("이벤트 없음 — 버튼을 눌러야만 실행돼요", color = TextSecondary, fontSize = 12.sp)
    cfg.triggers.forEach { t ->
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(TriggerType.label(t.type), color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = t.enabled, onCheckedChange = { on -> saveTrigger(t.id) { it.copy(enabled = on) } })
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Outlined.Close, "이벤트 삭제", tint = OverdueRed, modifier = Modifier.size(24.dp).clickable {
                    val c = latest(); save(c.copy(triggers = c.triggers.filterNot { it.id == t.id }))
                })
            }
            when (t.type) {
                TriggerType.TIME -> {
                    Label("시각 (HH:mm)")
                    DashField(t.time, "09:00", "tt${t.id}") { v -> saveTrigger(t.id) { it.copy(time = v.trim()) } }
                    Label("요일 · 아무것도 안 고르면 매일")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WeekdayNames.forEach { (d, name) ->
                            val on = d in t.days
                            Box(Modifier.size(34.dp).clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else DarkSurfaceVariant)
                                .clickable {
                                    saveTrigger(t.id) {
                                        val set = it.days.toSet().let { s -> if (on) s - d[0] else s + d[0] }
                                        it.copy(days = set.sorted().joinToString(""))
                                    }
                                }, contentAlignment = Alignment.Center) {
                                Text(name, fontSize = 12.sp, color = if (on) Color.White else MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                    Label("근무 · 그 근무인 날에만")
                    Chips(listOf("" to "상관없음", "주" to "주간", "야" to "야간", "비" to "비번", "휴" to "휴무", "당직" to "당직", "일근" to "일근"),
                        t.shift) { v -> saveTrigger(t.id) { it.copy(shift = v) } }
                    Text("앱이 꺼져 있어도 그 시각에 실행돼요 (화면 이동 단계는 건너뜀)", color = TextSecondary, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp))
                }
                TriggerType.APP_OPEN, TriggerType.TAB_OPEN -> Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("하루에 한 번만", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    Switch(checked = t.oncePerDay, onCheckedChange = { on -> saveTrigger(t.id) { it.copy(oncePerDay = on) } })
                }
                TriggerType.VALUE -> {
                    Label("변수")
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)
                        .clickable { pickVarFor = "t:${t.id}" }.padding(12.dp)) {
                        Text(DashVars.find(t.variable, ctx.lists)?.label ?: t.variable, color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f))
                        Text("지금 " + DashVars.value(t.variable, ctx).text, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Chips(CompareOps, t.op) { v -> saveTrigger(t.id) { it.copy(op = v) } }
                    Spacer(Modifier.height(6.dp))
                    DashField(t.value, "비교할 값 (숫자나 글자)", "tv${t.id}") { v -> saveTrigger(t.id) { it.copy(value = v) } }
                    Text("조건이 맞게 되면 하루 한 번 실행돼요", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
                else -> Text("하루 한 번 실행돼요", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { addingTrigger = true }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("이벤트 추가", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }

    Label("실행 조건 · 모두 맞을 때만 실행")
    if (cfg.conditions.isEmpty()) Text("조건 없음 — 항상 실행", color = TextSecondary, fontSize = 12.sp)
    cfg.conditions.forEach { c ->
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)
                    .clickable { pickVarFor = "c:${c.id}" }.padding(10.dp)) {
                    Text(DashVars.find(c.variable, ctx.lists)?.label ?: c.variable, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(DashVars.value(c.variable, ctx).text, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, maxLines = 1)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.Close, "조건 삭제", tint = OverdueRed, modifier = Modifier.size(24.dp).clickable {
                    val m = latest(); save(m.copy(conditions = m.conditions.filterNot { it.id == c.id }))
                })
            }
            Spacer(Modifier.height(6.dp))
            Chips(CompareOps, c.op) { v -> saveCondition(c.id) { it.copy(op = v) } }
            Spacer(Modifier.height(6.dp))
            DashField(c.value, "비교할 값 (예: 주간, 0, 50000)", "cv${c.id}") { v -> saveCondition(c.id) { it.copy(value = v) } }
            val ok = compareValue(DashVars.value(c.variable, ctx), c.op, c.value)
            Text(if (ok) "지금은 맞아요" else "지금은 안 맞아요", fontSize = 11.sp,
                color = if (ok) Color(0xFF35B36A) else TextSecondary, modifier = Modifier.padding(top = 6.dp))
        }
    }
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable {
        val m = latest(); save(m.copy(conditions = m.conditions + MacroCondition()))
    }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("조건 추가", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }
    MacroEngine.lastRun(context, block.id)?.let {
        Text("최근 실행: $it", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }

    if (addingTrigger) {
        ModalBottomSheet(onDismissRequest = { addingTrigger = false }, containerColor = DarkSurface) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
                Text("언제 실행할까요?", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                TriggerType.all.forEach { (type, label) ->
                    Text(label, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth().clickable {
                            addingTrigger = false
                            val m = latest(); save(m.copy(triggers = m.triggers + MacroTrigger(type)))
                        }.padding(horizontal = 20.dp, vertical = 14.dp))
                }
            }
        }
    }
    pickVarFor?.let { target ->
        VariablePickerSheet(ctx, onDismiss = { pickVarFor = null }) { v ->
            pickVarFor = null
            val id = target.substringAfter(':')
            if (target.startsWith("t:")) saveTrigger(id) { it.copy(variable = v.key) }
            else saveCondition(id) { it.copy(variable = v.key) }
        }
    }
}

/** The block's stored text as it is now (settings fields save one at a time). */
private fun currentText(tabId: String, blockId: String): String =
    CustomTabsPrefs.tab(tabId)?.blocks?.firstOrNull { it.id == blockId }?.text.orEmpty()

internal fun updateDashBlock(tabId: String, blockId: String, change: (DashBlock) -> DashBlock) {
    val tab = CustomTabsPrefs.tab(tabId) ?: return
    CustomTabsPrefs.update(tab.copy(blocks = tab.blocks.map { if (it.id == blockId) change(it) else it }))
}
