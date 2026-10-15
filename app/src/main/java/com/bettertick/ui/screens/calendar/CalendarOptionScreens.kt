package com.bettertick.ui.screens.calendar

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List as ListGlyph
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LabelOff
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.occursOn
import com.bettertick.ui.theme.DividerColor
import com.bettertick.ui.theme.GroupedBackground
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import com.bettertick.ui.theme.priorityColor
import com.bettertick.util.DateUtils.toLocalDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Shared TickTick settings-page building blocks
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OptionPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun OptionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GroupedCard)
            .padding(vertical = 6.dp),
        content = content
    )
}

@Composable
fun OptionNavRow(title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun TickSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = ThemeState.accentColor,
            checkedTrackColor = ThemeState.accentColor.copy(alpha = 0.3f),
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = TextTertiary.copy(alpha = 0.5f),
            uncheckedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun OptionSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    titleTrailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 16.sp, color = TextPrimary)
                if (titleTrailing != null) {
                    Spacer(Modifier.width(6.dp))
                    titleTrailing()
                }
            }
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextTertiary, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        TickSwitch(checked, onCheckedChange)
    }
}

@Composable
private fun ColorDot(color: Color?, size: androidx.compose.ui.unit.Dp = 28.dp, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .then(
                if (color == null) Modifier
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFDDDDDD), CircleShape)
                    .drawBehind {
                        drawLine(
                            Color(0xFFFF6160),
                            Offset(size.toPx() * 0.22f, size.toPx() * 0.78f),
                            Offset(size.toPx() * 0.78f, size.toPx() * 0.22f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                else Modifier.background(color)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "선택됨", tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 옵션 보기
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CalendarViewOptionsScreen(
    onBack: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenStyle: () -> Unit,
    onOpenTimeZone: () -> Unit
) {
    var focusHelp by remember { mutableStateOf(false) }
    OptionPage(title = "옵션 보기", onBack = onBack) {
        OptionCard {
            OptionNavRow("색상", value = CalendarPrefs.colorBy.label, onClick = onOpenColors)
            OptionNavRow("스타일", value = CalendarPrefs.chipStyle.label, onClick = onOpenStyle)
        }
        OptionCard {
            OptionSwitchRow(
                title = "자세히 보기",
                subtitle = "목록 보기 및 월 보기에 할일 상세 내역 표시.",
                checked = CalendarPrefs.showDetails,
                onCheckedChange = CalendarPrefs::updateShowDetails
            )
            OptionSwitchRow(
                title = "완료된 할일 보기",
                subtitle = "완료된 할일, 체크인된 습관 및 구독 캘린더 이벤트 포함.",
                checked = CalendarPrefs.showCompleted,
                onCheckedChange = CalendarPrefs::updateShowCompleted
            )
            OptionSwitchRow(
                title = "검사 대상 항목 표시",
                checked = CalendarPrefs.showCheckItems,
                onCheckedChange = CalendarPrefs::updateShowCheckItems
            )
            OptionSwitchRow(
                title = "반복 주기 표시",
                checked = CalendarPrefs.showRepeatCycles,
                onCheckedChange = CalendarPrefs::updateShowRepeatCycles
            )
        }
        OptionCard {
            OptionSwitchRow(
                title = "전념 기록 보기",
                checked = CalendarPrefs.showFocusRecords,
                onCheckedChange = CalendarPrefs::updateShowFocusRecords,
                titleTrailing = {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = "도움말",
                        tint = TextTertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable { focusHelp = true }
                    )
                }
            )
        }
        OptionCard {
            OptionNavRow(
                "보조 시간대",
                value = if (CalendarPrefs.secondaryTzEnabled) "켜짐" else null,
                onClick = onOpenTimeZone
            )
        }
    }
    if (focusHelp) {
        AlertDialog(
            onDismissRequest = { focusHelp = false },
            confirmButton = { TextButton(onClick = { focusHelp = false }) { Text("확인", color = ThemeState.accentColor) } },
            title = { Text("전념 기록 보기") },
            text = { Text("뽀모도로와 스톱워치로 집중한 기록을 주·3일·일 보기의 시간표에 블록으로 표시합니다.") },
            containerColor = GroupedCard
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 색상 (색상별 목록 / 태그 / 우선순위)  +  컬러 픽
// ─────────────────────────────────────────────────────────────────────────────

private sealed class ColorTarget(val title: String) {
    object Inbox : ColorTarget("기본함")
    object NoTag : ColorTarget("태그 없음")
    class ListTarget(val list: TaskList) : ColorTarget(list.name)
    class TagTarget(val tag: Tag) : ColorTarget(tag.name)
    class PriorityTarget(val level: Int, label: String) : ColorTarget(label)
}

@Composable
fun CalendarColorsScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val lists by viewModel.lists.collectAsState()
    val tags by viewModel.tags.collectAsState()
    var picking by remember { mutableStateOf<ColorTarget?>(null) }

    picking?.let { target ->
        val current: Color? = when (target) {
            ColorTarget.Inbox -> CalendarPrefs.inboxColorValue()
            ColorTarget.NoTag -> CalendarPrefs.noTagColorValue()
            is ColorTarget.ListTarget -> parseColorOrNull(target.list.color)
            is ColorTarget.TagTarget -> parseColorOrNull(target.tag.color)
            is ColorTarget.PriorityTarget -> CalendarPrefs.priorityColorFor(target.level)
        }
        ColorPickScreen(
            selected = current,
            onBack = { picking = null },
            onPick = { c ->
                when (target) {
                    ColorTarget.Inbox -> CalendarPrefs.updateInboxColor(c)
                    ColorTarget.NoTag -> CalendarPrefs.updateNoTagColor(c)
                    is ColorTarget.ListTarget -> viewModel.updateList(target.list.copy(color = (c ?: ThemeState.accentColor).toHex()))
                    is ColorTarget.TagTarget -> viewModel.updateTag(target.tag.copy(color = (c ?: ThemeState.accentColor).toHex()))
                    is ColorTarget.PriorityTarget -> CalendarPrefs.updatePriorityColor(target.level, c)
                }
                picking = null
            }
        )
        return
    }

    var menuOpen by remember { mutableStateOf(false) }
    OptionPage(title = "색상", onBack = onBack) {
        Box {
            OptionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { menuOpen = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("색상별", fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(CalendarPrefs.colorBy.label, fontSize = 15.sp, color = TextSecondary)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextTertiary)
                }
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(GroupedCard)
                ) {
                    CalendarColorBy.entries.forEach { opt ->
                        val sel = opt == CalendarPrefs.colorBy
                        DropdownMenuItem(
                            text = {
                                Text(opt.label, color = if (sel) ThemeState.accentColor else TextPrimary, fontSize = 16.sp)
                            },
                            trailingIcon = {
                                if (sel) Icon(Icons.Default.Check, null, tint = ThemeState.accentColor)
                            },
                            onClick = { CalendarPrefs.updateColorBy(opt); menuOpen = false }
                        )
                    }
                }
            }
        }
        Text(
            CalendarPrefs.colorBy.description,
            fontSize = 13.sp,
            color = TextTertiary,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        OptionCard {
            when (CalendarPrefs.colorBy) {
                CalendarColorBy.LIST -> {
                    ColorItemRow(Icons.Outlined.Inbox, "기본함", CalendarPrefs.inboxColorValue()) { picking = ColorTarget.Inbox }
                    lists.forEach { l ->
                        ColorItemRow(
                            icon = Icons.AutoMirrored.Filled.ListGlyph,
                            title = l.name,
                            color = parseColorOrNull(l.color) ?: CalendarPrefs.inboxColorValue()
                        ) { picking = ColorTarget.ListTarget(l) }
                    }
                }
                CalendarColorBy.TAG -> {
                    ColorItemRow(Icons.Outlined.LabelOff, "태그 없음", CalendarPrefs.noTagColorValue()) { picking = ColorTarget.NoTag }
                    tags.forEach { t ->
                        ColorItemRow(Icons.Outlined.LocalOffer, t.name, parseColorOrNull(t.color) ?: CalendarPrefs.noTagColorValue()) {
                            picking = ColorTarget.TagTarget(t)
                        }
                    }
                }
                CalendarColorBy.PRIORITY -> {
                    listOf(3 to "높은 우선순위", 2 to "중간 우선순위", 1 to "낮은 우선순위", 0 to "우선순위 없음").forEach { (lvl, label) ->
                        ColorItemRow(
                            Icons.Outlined.Flag, label, CalendarPrefs.priorityColorFor(lvl),
                            iconTint = priorityColor(lvl)
                        ) { picking = ColorTarget.PriorityTarget(lvl, label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorItemRow(
    icon: ImageVector,
    title: String,
    color: Color,
    iconTint: Color = TextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        ColorDot(color, size = 30.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickScreen(
    selected: Color?,
    onBack: () -> Unit,
    onPick: (Color?) -> Unit
) {
    var customDialog by remember { mutableStateOf(false) }
    OptionPage(title = "컬러 픽", onBack = onBack) {
        OptionCard {
            TickColorPalettes.forEach { palette ->
                Text(
                    palette.name,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 10.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    palette.colors.forEach { c ->
                        ColorDot(
                            color = c,
                            size = 34.dp,
                            selected = c != null && selected != null && c.toHex() == selected.toHex(),
                            onClick = { onPick(c) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(6.dp))
        }
        OptionCard {
            Text(
                "사용자 정의",
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 10.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TextTertiary.copy(alpha = 0.18f))
                        .clickable { customDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "색상 추가", tint = ThemeState.accentColor)
                }
                CalendarPrefs.customColors.forEach { argb ->
                    val c = Color(argb)
                    ColorDot(
                        color = c,
                        size = 34.dp,
                        selected = selected != null && c.toHex() == selected.toHex(),
                        onClick = { onPick(c) }
                    )
                }
            }
        }
    }
    if (customDialog) {
        CustomColorDialog(
            initial = selected ?: ThemeState.accentColor,
            onDismiss = { customDialog = false },
            onConfirm = { c ->
                CalendarPrefs.addCustomColor(c)
                customDialog = false
                onPick(c)
            }
        )
    }
}

@Composable
private fun CustomColorDialog(initial: Color, onDismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    val hsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgbInt(), it) }
    }
    var hue by remember { mutableStateOf(hsv[0]) }
    var sat by remember { mutableStateOf(hsv[1].coerceAtLeast(0.15f)) }
    var value by remember { mutableStateOf(hsv[2].coerceAtLeast(0.3f)) }
    var hexText by remember { mutableStateOf("") }
    val color = Color.hsv(hue, sat, value)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GroupedCard,
        title = { Text("사용자 정의 색상", color = TextPrimary) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                )
                Spacer(Modifier.height(12.dp))
                LabeledSlider("색상", hue, 0f..360f) { hue = it }
                LabeledSlider("채도", sat, 0f..1f) { sat = it }
                LabeledSlider("밝기", value, 0f..1f) { value = it }
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { t ->
                        hexText = t
                        parseColorOrNull(if (t.startsWith("#")) t else "#$t")?.let { c ->
                            val out = FloatArray(3)
                            android.graphics.Color.colorToHSV(c.toArgbInt(), out)
                            hue = out[0]; sat = out[1]; value = out[2]
                        }
                    },
                    singleLine = true,
                    placeholder = { Text(color.toHex(), color = TextTertiary) },
                    label = { Text("HEX") }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(color) }) { Text("확인", color = ThemeState.accentColor) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) } }
    )
}

private fun Color.toArgbInt(): Int = (alpha * 255).toInt() shl 24 or
    ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt()

@Composable
private fun LabeledSlider(label: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(40.dp))
        Slider(
            value = v,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = ThemeState.accentColor, activeTrackColor = ThemeState.accentColor)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 스타일
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CalendarStyleScreen(onBack: () -> Unit) {
    OptionPage(title = "스타일", onBack = onBack) {
        OptionCard {
            CalendarChipStyle.entries.forEach { style ->
                val sel = style == CalendarPrefs.chipStyle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { CalendarPrefs.updateChipStyle(style) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(style.label, fontSize = 16.sp, color = if (sel) ThemeState.accentColor else TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(style.description, fontSize = 12.sp, color = TextTertiary)
                    }
                    StylePreviewChip(style)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.width(22.dp)) {
                        if (sel) Icon(Icons.Default.Check, null, tint = ThemeState.accentColor)
                    }
                }
            }
        }
        Text(
            "캘린더의 월·주·일 보기에서 할일이 표시되는 모양을 바꿉니다.",
            fontSize = 13.sp,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun StylePreviewChip(style: CalendarChipStyle) {
    val base = ThemeState.accentColor
    // Colors as if [style] were active, without mutating the prefs.
    val colors = when (style) {
        CalendarChipStyle.FILLED -> ChipColors(base, Color.White)
        CalendarChipStyle.TINTED -> ChipColors(base.copy(alpha = 0.18f), base)
        CalendarChipStyle.OUTLINED -> ChipColors(base.copy(alpha = 0.12f), base, base)
    }
    Row(
        modifier = Modifier
            .width(64.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (colors.bar != null) {
            Box(Modifier.width(3.dp).height(22.dp).background(colors.bar))
        }
        Text("할일", fontSize = 11.sp, color = colors.content, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 시간대 설정 (보조 시간대)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CalendarTimeZoneScreen(onBack: () -> Unit) {
    var picker by remember { mutableStateOf(false) }
    val currentId = remember { ZoneId.systemDefault().id }
    OptionPage(title = "시간대 설정", onBack = onBack) {
        OptionCard {
            OptionSwitchRow(
                title = "보조 시간대",
                subtitle = "활성화하면, 캘린더 보기에서 여러 시간대를 볼 수 있습니다.",
                checked = CalendarPrefs.secondaryTzEnabled,
                onCheckedChange = CalendarPrefs::updateSecondaryTzEnabled
            )
        }
        OptionCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("현재 시간대", fontSize = 16.sp, color = TextPrimary)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.Place, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${zoneCityName(currentId)}, ${zoneOffsetLabel(currentId)}", fontSize = 13.sp, color = TextTertiary)
                }
            }
            CalendarPrefs.secondaryZones.forEach { id ->
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(zoneCityName(id), fontSize = 16.sp, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(zoneOffsetLabel(id), fontSize = 13.sp, color = TextTertiary)
                    }
                    IconButton(onClick = { CalendarPrefs.removeSecondaryZone(id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "삭제", tint = TextTertiary)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { picker = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = ThemeState.accentColor)
                Spacer(Modifier.width(8.dp))
                Text("시간대 추가", fontSize = 16.sp, color = ThemeState.accentColor)
            }
        }
    }
    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            containerColor = GroupedCard,
            title = { Text("시간대 추가", color = TextPrimary) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(TimeZoneChoices.filter { it.first != currentId && it.first !in CalendarPrefs.secondaryZones }) { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    CalendarPrefs.addSecondaryZone(id)
                                    if (!CalendarPrefs.secondaryTzEnabled) CalendarPrefs.updateSecondaryTzEnabled(true)
                                    picker = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(name, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(zoneOffsetLabel(id), fontSize = 13.sp, color = TextTertiary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picker = false }) { Text("닫기", color = ThemeState.accentColor) } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 필터 보기 범위
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterSheet(
    lists: List<TaskList>,
    tags: List<Tag>,
    onDismiss: () -> Unit
) {
    var hidden by remember { mutableStateOf(CalendarPrefs.hiddenListIds) }
    var tagSel by remember { mutableStateOf(CalendarPrefs.tagFilter) }
    val allLists = listOf("" to "기본함") + lists.map { it.id to it.name }
    ModalBottomSheet(
        onDismissRequest = {
            CalendarPrefs.updateFilter(hidden, tagSel)
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GroupedCard
    ) {
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("필터 보기 범위", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        CalendarPrefs.updateFilter(hidden, tagSel)
                        onDismiss()
                    }) { Text("완료", color = ThemeState.accentColor, fontWeight = FontWeight.SemiBold) }
                }
            }
            item { FilterSectionTitle("목록") }
            item {
                FilterCheckRow("모든 목록", checked = hidden.isEmpty()) {
                    hidden = if (hidden.isEmpty()) allLists.map { it.first }.toSet() else emptySet()
                }
            }
            items(allLists) { (id, name) ->
                FilterCheckRow(name, checked = id !in hidden, indent = true) {
                    hidden = if (id in hidden) hidden - id else hidden + id
                }
            }
            if (tags.isNotEmpty()) {
                item { FilterSectionTitle("태그") }
                item {
                    FilterCheckRow("모든 태그", checked = tagSel.isEmpty()) { tagSel = emptySet() }
                }
                items(tags) { t ->
                    FilterCheckRow("#${t.name}", checked = t.id in tagSel, indent = true) {
                        tagSel = if (t.id in tagSel) tagSel - t.id else tagSel + t.id
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextTertiary,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun FilterCheckRow(label: String, checked: Boolean, indent: Boolean = false, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(start = if (indent) 32.dp else 12.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = ThemeState.accentColor, uncheckedColor = TextTertiary)
        )
        Text(label, fontSize = 15.sp, color = TextPrimary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 할 일 배정하기 — unscheduled tasks to drop onto a date
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ArrangeTasksPanel(
    tasks: List<Task>,
    lists: List<TaskList>,
    selectedTaskId: String?,
    onSelect: (Task?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var listFilter by remember { mutableStateOf<String?>(null) }
    var listMenu by remember { mutableStateOf(false) }
    val shown = tasks.filter { listFilter == null || it.listId == listFilter }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(GroupedCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("할 일 배정하기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.width(8.dp))
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TextTertiary.copy(alpha = 0.15f))
                        .clickable { listMenu = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (listFilter) {
                            null -> "모든 목록"
                            "" -> "기본함"
                            else -> lists.firstOrNull { it.id == listFilter }?.name ?: "목록"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                    DropdownMenuItem(text = { Text("모든 목록") }, onClick = { listFilter = null; listMenu = false })
                    DropdownMenuItem(text = { Text("기본함") }, onClick = { listFilter = ""; listMenu = false })
                    lists.forEach { l ->
                        DropdownMenuItem(text = { Text(l.name) }, onClick = { listFilter = l.id; listMenu = false })
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = TextSecondary)
            }
        }
        Text(
            if (selectedTaskId == null) "할일을 선택한 뒤 캘린더의 날짜를 탭하면 그 날짜로 배정됩니다."
            else "배정할 날짜를 탭하세요.",
            fontSize = 12.sp,
            color = if (selectedTaskId == null) TextTertiary else ThemeState.accentColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(6.dp))
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("날짜가 없는 할일이 없습니다", fontSize = 14.sp, color = TextTertiary)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.id }) { task ->
                    val sel = task.id == selectedTaskId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) ThemeState.accentColor.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable { onSelect(if (sel) null else task) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.5.dp, priorityColor(task.priority), RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            task.title,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            lists.firstOrNull { it.id == task.listId }?.name ?: "기본함",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공유
// ─────────────────────────────────────────────────────────────────────────────

/** Plain-text agenda for [dates], one block per day that has tasks. */
fun buildCalendarShareText(title: String, dates: List<LocalDate>, tasks: List<Task>): String {
    val dayFmt = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    val sb = StringBuilder(title).append("\n")
    dates.forEach { d ->
        val day = tasks.filter { it.occursOn(d) && !it.isAbandoned }
        if (day.isEmpty()) return@forEach
        sb.append("\n").append(d.format(dayFmt)).append("\n")
        day.sortedBy { it.dueDate?.toDate()?.time ?: Long.MAX_VALUE }.forEach { t ->
            val time = t.dueDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime()
                ?.takeIf { it != LocalTime.MIDNIGHT }
                ?.let { " %02d:%02d".format(it.hour, it.minute) } ?: ""
            sb.append(if (t.isCompleted) "☑ " else "☐ ").append(t.title).append(time).append("\n")
        }
    }
    if (sb.lines().size <= 2) sb.append("\n일정이 없습니다.\n")
    return sb.toString().trimEnd()
}

fun shareCalendarText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** The task a calendar item stands for (check items map back to their task). */
fun parentTaskId(id: String): String = id.substringBefore(CHECK_ITEM_SEP)

const val CHECK_ITEM_SEP = "#ci"

/**
 * "검사 대상 항목 표시": expands a task's `- [ ]` checklist lines into
 * display-only all-day entries on the same day(s) as the task.
 */
fun Task.checkItemEntries(): List<Task> {
    val lines = notes.lines()
    val items = lines.mapNotNull { line ->
        when {
            line.startsWith("- [ ] ") -> line.removePrefix("- [ ] ") to false
            line.startsWith("- [x] ") || line.startsWith("- [X] ") -> line.drop(6) to true
            else -> null
        }
    }
    if (items.isEmpty()) return emptyList()
    val midnight = dueDate?.toLocalDate()?.let { d ->
        com.google.firebase.Timestamp(java.util.Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }
    return items.mapIndexed { i, (text, done) ->
        copy(
            id = "$id$CHECK_ITEM_SEP$i",
            title = "· $text",
            notes = "",
            isCompleted = done || isCompleted,
            dueDate = midnight,
            durationMinutes = 0
        )
    }
}
