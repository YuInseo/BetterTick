package com.bettertick.ui.screens.custom

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.DiaryEntry
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.completedOn
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.occursOn
import com.bettertick.data.model.shiftKindOn
import com.bettertick.data.model.shiftTitleOn
import com.bettertick.ui.screens.tasks.HabitTodayItem
import com.bettertick.ui.screens.tasks.MoneyPrefs
import com.bettertick.ui.screens.tasks.SalaryPrefs
import com.bettertick.ui.screens.tasks.components.shiftColor
import com.bettertick.ui.screens.tasks.won
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.GroupedBackground
import com.bettertick.ui.theme.GroupedCard
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Everything the blocks read, gathered once per screen. */
private class DashData(
    val tasks: List<Task>,
    val lists: List<TaskList>,
    val habits: List<HabitTodayItem>,
    val diary: DiaryEntry?
) {
    val ctx = DashContext(tasks, lists, habits, diary)
}

/** Saves a change to one block of [tabId], reading the latest copy of the tab. */
private fun updateBlock(tabId: String, blockId: String, change: (DashBlock) -> DashBlock) {
    val tab = CustomTabsPrefs.tab(tabId) ?: return
    CustomTabsPrefs.update(tab.copy(blocks = tab.blocks.map { if (it.id == blockId) change(it) else it }))
}

/**
 * 내 탭: a tab the user built out of blocks. "편집" switches to arrange mode, where
 * blocks are dragged into order (long-press the handle), set up (⚙), removed or
 * added, and the tab's name and icon are changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTabScreen(
    tabId: String,
    onNavigate: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: CustomTabViewModel = hiltViewModel()
) {
    val tab = CustomTabsPrefs.tabs.firstOrNull { it.id == tabId }
    if (tab == null) {
        Box(Modifier.fillMaxSize().background(GroupedBackground), contentAlignment = Alignment.Center) {
            Text("탭을 찾을 수 없어요", color = TextSecondary)
        }
        return
    }
    val tasks by viewModel.tasks.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val diary by viewModel.diary.collectAsState()
    val data = DashData(tasks, lists, habits, diary)
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.messages.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
    }

    var editing by rememberSaveable(tabId) {
        mutableStateOf((CustomTabsPrefs.justCreated == tabId).also { if (it) CustomTabsPrefs.justCreated = null })
    }
    var showAdd by remember { mutableStateOf(false) }
    var settingsFor by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(GroupedBackground).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(customTabIcon(tab.icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(tab.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            if (editing) {
                TextButton(onClick = { editing = false }) { Text("완료", fontWeight = FontWeight.SemiBold) }
            } else {
                IconButton(onClick = { editing = true }) {
                    Icon(Icons.Outlined.Edit, "편집", tint = TextSecondary)
                }
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (editing) {
                TabSettings(tab)
                Text("블록 · 핸들을 길게 눌러 끌면 순서가 바뀌어요", style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                ArrangeBlocks(tab, lists, onSettings = { settingsFor = it })
                AddRow("블록 추가") { showAdd = true }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GroupedCard)
                        .clickable { confirmDelete = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = OverdueRed)
                    Spacer(Modifier.width(12.dp))
                    Text("이 탭 삭제", color = OverdueRed)
                }
            } else {
                if (tab.blocks.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("아직 블록이 없어요", color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { editing = true; showAdd = true }) { Text("블록 추가") }
                    }
                }
                // Half blocks next to each other share a row; a lone half block keeps its half.
                blockRows(tab.blocks).forEach { row ->
                    key(row.first().id) {
                        if (row.size == 1 && !row[0].half) {
                            DashBlockView(tab, row[0], data, viewModel, onNavigate)
                        } else {
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { b ->
                                    Box(Modifier.weight(1f).fillMaxHeight()) { DashBlockView(tab, b, data, viewModel, onNavigate, fill = true) }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }, containerColor = DarkSurface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
                Text("블록 추가", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                DashBlockType.catalog.forEach { (type, label) ->
                    Column(Modifier.fillMaxWidth().clickable {
                        showAdd = false
                        val fresh = CustomTabsPrefs.tab(tab.id) ?: return@clickable
                        val block = DashBlock(type = type, half = DashBlockType.halfByDefault(type),
                            text = when (type) {
                                DashBlockType.SHORTCUT -> ShortcutTargets.first().first
                                DashBlockType.VALUE -> ValueConfig().encode()
                                DashBlockType.TEMPLATE -> TemplateConfig().encode()
                                DashBlockType.MACRO -> MacroConfig().encode()
                                else -> ""
                            })
                        CustomTabsPrefs.update(fresh.copy(blocks = fresh.blocks + block))
                        editing = true
                        if (DashBlockType.needsSetup(type)) settingsFor = block.id
                    }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(label, color = MaterialTheme.colorScheme.onBackground)
                        Text(DashBlockType.hint(type), color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
    settingsFor?.let { id ->
        val block = tab.blocks.firstOrNull { it.id == id }
        if (block == null) settingsFor = null
        else BlockSettingsSheet(tab.id, block, lists, data.ctx, onDismiss = { settingsFor = null })
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("'${tab.name}' 탭을 삭제할까요?") },
            text = { Text("탭과 그 안의 블록이 사라져요. 할 일·일정은 그대로 남아요.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; CustomTabsPrefs.delete(tab.id); onDeleted() }) {
                    Text("삭제", color = OverdueRed)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } }
        )
    }
}

/** Groups blocks into display rows: two adjacent half blocks share one. */
private fun blockRows(blocks: List<DashBlock>): List<List<DashBlock>> {
    val rows = mutableListOf<List<DashBlock>>()
    var i = 0
    while (i < blocks.size) {
        val b = blocks[i]
        if (b.half && i + 1 < blocks.size && blocks[i + 1].half) { rows += listOf(b, blocks[i + 1]); i += 2 }
        else { rows += listOf(b); i++ }
    }
    return rows
}

private fun blockName(block: DashBlock, lists: List<TaskList>): String = block.title.ifBlank {
    when (block.type) {
        DashBlockType.SHORTCUT -> "바로가기 · " + (ShortcutTargets.firstOrNull { it.first == block.text }?.second ?: block.text)
        DashBlockType.LIST_TASKS -> lists.firstOrNull { it.id == block.text }?.name ?: "목록 할 일"
        DashBlockType.DDAY -> parseDday(block.text)?.second?.ifBlank { null } ?: "디데이"
        DashBlockType.VALUE -> "값 · " + (DashVars.find(ValueConfig.decode(block.text).variable, lists)?.label ?: "변수")
        DashBlockType.MACRO -> "매크로 · " + MacroConfig.decode(block.text).label
        DashBlockType.TEMPLATE -> "템플릿 · " + TemplateConfig.decode(block.text).text.take(20)
        else -> DashBlockType.label(block.type)
    }
}

/** "yyyy-MM-dd|제목" → (date, title). */
private fun parseDday(text: String): Pair<LocalDate, String>? {
    val date = runCatching { LocalDate.parse(text.substringBefore('|')) }.getOrNull() ?: return null
    return date to text.substringAfter('|', "")
}

@Composable
private fun TabSettings(tab: CustomTab) {
    var name by remember(tab.id) { mutableStateOf(tab.name) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GroupedCard).padding(16.dp)
    ) {
        Text("탭 이름", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = name,
            onValueChange = { v ->
                name = v.take(20)
                CustomTabsPrefs.tab(tab.id)?.let { CustomTabsPrefs.update(it.copy(name = name.ifBlank { "내 탭" })) }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Text("아이콘", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CustomTabIcons.forEach { (key, icon) ->
                val selected = key == tab.icon
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else DarkSurfaceVariant)
                        .then(if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        .clickable { CustomTabsPrefs.tab(tab.id)?.let { CustomTabsPrefs.update(it.copy(icon = key)) } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Arrange mode: one compact row per block, dragged into place by its handle. */
@Composable
private fun ArrangeBlocks(tab: CustomTab, lists: List<TaskList>, onSettings: (String) -> Unit) {
    val order = remember(tab.id) { mutableStateListOf<DashBlock>().apply { addAll(tab.blocks) } }
    var draggingId by remember { mutableStateOf<String?>(null) }
    // Pick up edits made elsewhere (added, removed, renamed) unless a drag is running.
    if (draggingId == null && order.toList() != tab.blocks) {
        order.clear(); order.addAll(tab.blocks)
    }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    fun commit() { CustomTabsPrefs.tab(tab.id)?.let { CustomTabsPrefs.update(it.copy(blocks = order.toList())) } }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GroupedCard)) {
        order.forEach { block ->
            key(block.id) {
                val dragging = draggingId == block.id
                Row(
                    Modifier.fillMaxWidth()
                        .onGloballyPositioned { if (rowHeight == 0f) rowHeight = it.size.height.toFloat() }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) dragY else 0f
                            shadowElevation = if (dragging) 12f else 0f
                            scaleX = if (dragging) 1.02f else 1f
                            scaleY = if (dragging) 1.02f else 1f
                        }
                        .background(if (dragging) DarkSurfaceVariant else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.RemoveCircleOutline, "삭제", tint = OverdueRed,
                        modifier = Modifier.size(22.dp).clip(CircleShape).clickable {
                            order.remove(block); commit()
                        })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(blockName(block, lists), color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (block.half) "절반 너비" else "전체 너비", color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.Tune, "블록 설정", tint = TextSecondary,
                        modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onSettings(block.id) }.padding(7.dp))
                    Icon(Icons.Outlined.DragHandle, "순서 바꾸기", tint = TextSecondary,
                        modifier = Modifier.size(36.dp).padding(4.dp).pointerInput(block.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingId = block.id; dragY = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragY += drag.y
                                    val h = rowHeight
                                    if (h <= 0f) return@detectDragGesturesAfterLongPress
                                    while (true) {
                                        val i = order.indexOfFirst { it.id == block.id }
                                        if (i < 0) break
                                        when {
                                            dragY > h / 2 && i + 1 < order.size -> { order.add(i + 1, order.removeAt(i)); dragY -= h }
                                            dragY < -h / 2 && i > 0 -> { order.add(i - 1, order.removeAt(i)); dragY += h }
                                            else -> return@detectDragGesturesAfterLongPress
                                        }
                                    }
                                },
                                onDragEnd = { commit(); draggingId = null; dragY = 0f },
                                onDragCancel = { commit(); draggingId = null; dragY = 0f }
                            )
                        })
                }
            }
        }
        if (order.isEmpty()) {
            Text("블록이 없어요", color = TextSecondary, modifier = Modifier.padding(16.dp))
        }
    }
}

/** ⚙ for one block: its heading, width, and what it shows (list, date, target). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockSettingsSheet(tabId: String, block: DashBlock, lists: List<TaskList>, ctx: DashContext, onDismiss: () -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text(DashBlockType.label(block.type) + " 설정", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(vertical = 8.dp))

            SettingLabel("제목")
            var title by remember(block.id) { mutableStateOf(block.title) }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant).padding(12.dp)) {
                if (title.isEmpty()) Text(blockName(block.copy(title = ""), lists), color = TextSecondary)
                BasicTextField(
                    value = title,
                    onValueChange = { v -> title = v.take(30); updateBlock(tabId, block.id) { it.copy(title = title) } },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("절반 너비", color = MaterialTheme.colorScheme.onBackground)
                    Text("절반 블록 두 개가 이어지면 나란히 놓여요", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(checked = block.half, onCheckedChange = { on -> updateBlock(tabId, block.id) { it.copy(half = on) } })
            }

            when (block.type) {
                DashBlockType.SHORTCUT -> {
                    SettingLabel("이동할 화면")
                    ChoiceList(ShortcutTargets, block.text) { v -> updateBlock(tabId, block.id) { it.copy(text = v) } }
                }
                DashBlockType.LIST_TASKS, DashBlockType.QUICK_ADD -> {
                    SettingLabel(if (block.type == DashBlockType.QUICK_ADD) "추가할 목록" else "보여줄 목록")
                    val choices = listOf("" to (if (block.type == DashBlockType.QUICK_ADD) "기본함" else "모든 목록")) +
                        lists.map { it.id to it.name }
                    ChoiceList(choices, block.text) { v -> updateBlock(tabId, block.id) { it.copy(text = v) } }
                }
                DashBlockType.DDAY -> {
                    val dday = parseDday(block.text)
                    SettingLabel("날짜")
                    Text(dday?.first?.let { "${it.year}년 ${it.monthValue}월 ${it.dayOfMonth}일" } ?: "날짜를 골라 주세요",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)
                            .clickable { showDate = true }.padding(12.dp))
                    SettingLabel("무슨 날인가요?")
                    var name by remember(block.id) { mutableStateOf(dday?.second.orEmpty()) }
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant).padding(12.dp)) {
                        if (name.isEmpty()) Text("예: 여행, 시험, 기념일", color = TextSecondary)
                        BasicTextField(
                            value = name,
                            onValueChange = { v ->
                                name = v.take(20)
                                updateBlock(tabId, block.id) {
                                    val date = parseDday(it.text)?.first ?: LocalDate.now()
                                    it.copy(text = "$date|$name")
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                DashBlockType.VALUE -> ValueSettings(tabId, block, ctx)
                DashBlockType.TEMPLATE -> TemplateSettings(tabId, block, ctx)
                DashBlockType.MACRO -> MacroSettings(tabId, block, ctx)
                else -> Unit
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("완료") }
        }
    }
    if (showDate) {
        val initial = parseDday(block.text)?.first ?: LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    showDate = false
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    updateBlock(tabId, block.id) { it.copy(text = "$date|" + (parseDday(it.text)?.second ?: "")) }
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("취소") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
private fun ChoiceList(choices: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)) {
        choices.forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clickable { onPick(value) }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                if (value == selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GroupedCard).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Card(title: String?, fill: Boolean, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)
            .clip(RoundedCornerShape(16.dp)).background(GroupedCard)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp)
    ) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

private fun Task.timeLabel(): String? {
    val t = dueDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: return null
    if (t == java.time.LocalTime.MIDNIGHT) return null
    return "%02d:%02d".format(t.hour, t.minute)
}

private fun Task.isPlainTask() = !isShiftTask() && parentId.isBlank() && !isAbandoned

private fun parseHex(hex: String): Color? =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()

@Composable
private fun TaskCheckRow(t: Task, day: LocalDate, onToggle: (Task, LocalDate) -> Unit) {
    val done = t.completedOn(day)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onToggle(t, day) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null,
            tint = if (done) TextSecondary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(t.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (done) TextSecondary else MaterialTheme.colorScheme.onBackground,
            textDecoration = if (done) TextDecoration.LineThrough else null)
        t.timeLabel()?.let { Text(it, color = TextSecondary, fontSize = 13.sp) }
    }
}

@Composable
private fun DashBlockView(
    tab: CustomTab,
    block: DashBlock,
    data: DashData,
    viewModel: CustomTabViewModel,
    onNavigate: (String) -> Unit,
    fill: Boolean = false
) {
    val today = remember { LocalDate.now() }
    val tasks = data.tasks
    val heading = block.title.ifBlank { null }
    when (block.type) {
        DashBlockType.TODAY -> Card(heading, fill) {
            val shift = tasks.firstOrNull { it.isShiftTask() && it.occursOn(today) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${today.monthValue}월 ${today.dayOfMonth}일 " +
                        today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    val left = tasks.count { it.isPlainTask() && it.occursOn(today) && !it.completedOn(today) }
                    Text(if (left == 0) "오늘 남은 할 일이 없어요" else "오늘 남은 할 일 ${left}개",
                        color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                if (shift != null) {
                    val c = shiftColor(shift.shiftKindOn(today)) ?: MaterialTheme.colorScheme.primary
                    Text(shift.shiftTitleOn(today), color = c, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
        DashBlockType.TODAY_TASKS -> Card(heading ?: "오늘 할 일", fill) {
            val list = remember(tasks) {
                tasks.filter { it.isPlainTask() && it.occursOn(today) }
                    .sortedWith(compareBy({ it.completedOn(today) }, { it.timeLabel() != null }, { it.dueDate?.seconds ?: 0L }))
            }
            if (list.isEmpty()) Text("오늘 할 일이 없어요", color = TextSecondary)
            list.take(12).forEach { TaskCheckRow(it, today, viewModel::toggle) }
            if (list.size > 12) Text("+${list.size - 12}개 더", color = TextSecondary, fontSize = 13.sp,
                modifier = Modifier.clickable { onNavigate("tasks") }.padding(top = 4.dp))
        }
        DashBlockType.LIST_TASKS -> {
            val listName = data.lists.firstOrNull { it.id == block.text }?.name
            Card(heading ?: listName ?: "모든 목록", fill) {
                val list = remember(tasks, block.text) {
                    tasks.filter { it.isPlainTask() && !it.isCompleted && (block.text.isBlank() || it.listId == block.text) }
                        .sortedBy { it.dueDate?.seconds ?: Long.MAX_VALUE }
                }
                if (list.isEmpty()) Text("남은 할 일이 없어요", color = TextSecondary)
                list.take(10).forEach { TaskCheckRow(it, today, viewModel::toggle) }
                if (list.size > 10) Text("+${list.size - 10}개 더", color = TextSecondary, fontSize = 13.sp,
                    modifier = Modifier.clickable { onNavigate("tasks") }.padding(top = 4.dp))
            }
        }
        DashBlockType.QUICK_ADD -> Card(heading, fill) {
            var text by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) Text("오늘 할 일 추가", color = TextSecondary)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.addToday(text, block.text); text = "" }),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (text.isNotBlank()) {
                    TextButton(onClick = { viewModel.addToday(text, block.text); text = "" }) { Text("추가") }
                }
            }
        }
        DashBlockType.HABITS -> Card(heading ?: "오늘 습관", fill) {
            val habits = data.habits
            if (habits.isEmpty()) Text("오늘 할 습관이 없어요", color = TextSecondary,
                modifier = Modifier.clickable { onNavigate("habits") })
            val done = habits.count { it.done }
            if (habits.isNotEmpty()) {
                LinearProgressIndicator(progress = { done / habits.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    trackColor = DarkSurfaceVariant)
                Spacer(Modifier.height(10.dp))
            }
            habits.forEach { item ->
                val c = parseHex(item.habit.color) ?: MaterialTheme.colorScheme.primary
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { viewModel.toggleHabit(item.habit.id) }
                    .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(CircleShape)
                        .background(if (item.done) c else Color.Transparent)
                        .border(2.dp, c, CircleShape), contentAlignment = Alignment.Center) {
                        if (item.done) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(item.habit.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (item.done) TextSecondary else MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        DashBlockType.MINI_CALENDAR -> Card(heading ?: "${today.monthValue}월", fill, onClick = { onNavigate("calendar") }) {
            MiniMonth(today, tasks)
        }
        DashBlockType.WEEK_SHIFTS -> Card(heading ?: "이번 주 근무", fill) {
            val monday = remember(today) { today.with(DayOfWeek.MONDAY) }
            val shifts = remember(tasks) { tasks.filter { it.isShiftTask() } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0L until 7L).forEach { offset ->
                    val day = monday.plusDays(offset)
                    val kind = shifts.firstOrNull { it.occursOn(day) }?.shiftKindOn(day)
                    val c = kind?.let { shiftColor(it) } ?: TextSecondary
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN), fontSize = 12.sp,
                            color = if (day == today) MaterialTheme.colorScheme.primary else TextSecondary,
                            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.size(34.dp).clip(CircleShape)
                            .background(if (kind != null) c.copy(alpha = 0.16f) else Color.Transparent)
                            .then(if (day == today) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center) {
                            Text(kind ?: "·", color = c, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        DashBlockType.UPCOMING -> Card(heading ?: "다가오는 일정", fill) {
            val items = remember(tasks) {
                (1L..14L).flatMap { d ->
                    val day = today.plusDays(d)
                    tasks.filter { it.isPlainTask() && it.occursOn(day) && !it.completedOn(day) }.map { day to it }
                }.take(6)
            }
            if (items.isEmpty()) Text("2주 안에 잡힌 일정이 없어요", color = TextSecondary)
            items.forEach { (day, t) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${day.monthValue}/${day.dayOfMonth} " + day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                        color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(76.dp))
                    Text(t.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    t.timeLabel()?.let { Text(it, color = TextSecondary, fontSize = 13.sp) }
                }
            }
        }
        DashBlockType.DDAY -> {
            val dday = parseDday(block.text)
            Card(heading ?: dday?.second?.ifBlank { null } ?: "디데이", fill) {
                if (dday == null) {
                    Text("편집 › 블록 설정에서 날짜를 골라 주세요", color = TextSecondary)
                } else {
                    val days = ChronoUnit.DAYS.between(today, dday.first)
                    Text(when {
                        days == 0L -> "D-DAY"
                        days > 0 -> "D-$days"
                        else -> "D+${-days}"
                    }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = if (days == 0L) OverdueRed else MaterialTheme.colorScheme.primary)
                    Text("${dday.first.year}.${dday.first.monthValue}.${dday.first.dayOfMonth}", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        DashBlockType.STATS -> Card(heading ?: "이번 주 달성률", fill) {
            val monday = remember(today) { today.with(DayOfWeek.MONDAY) }
            val (done, total) = remember(tasks) {
                var d = 0
                var t = 0
                for (i in 0L until 7L) {
                    val day = monday.plusDays(i)
                    tasks.filter { it.isPlainTask() && it.occursOn(day) }.forEach { task ->
                        t++
                        if (task.completedOn(day)) d++
                    }
                }
                d to t
            }
            val pct = if (total == 0) 0 else done * 100 / total
            Text("$pct%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("$done / $total 완료", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { if (total == 0) 0f else done / total.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = DarkSurfaceVariant)
        }
        DashBlockType.DIARY -> Card(heading ?: "오늘 일기", fill, onClick = { onNavigate("diary") }) {
            val content = data.diary?.content?.trim().orEmpty()
            if (content.isEmpty()) Text("아직 오늘 일기를 안 썼어요 · 눌러서 쓰기", color = TextSecondary)
            else Text(content, color = MaterialTheme.colorScheme.onBackground, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
        DashBlockType.MONEY -> Card(heading ?: "이번 달 가계부", fill, onClick = { onNavigate("money") }) {
            val month = remember(today) { YearMonth.from(today) }
            val book = MoneyPrefs.book
            val income = book.incomeIn(month) + SalaryPrefs.total(month)
            val expense = book.expenseIn(month)
            Row(Modifier.fillMaxWidth()) {
                MoneyStat("수입", income, Color(0xFF4CA6F5), Modifier.weight(1f))
                MoneyStat("지출", expense, OverdueRed, Modifier.weight(1f))
                MoneyStat("남은 돈", income - expense, MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
            }
        }
        DashBlockType.MEMO -> Card(heading, fill) {
            var text by remember(block.id) { mutableStateOf(block.text) }
            Box {
                if (text.isEmpty()) Text("메모를 적어 두세요", color = TextSecondary)
                BasicTextField(
                    value = text,
                    onValueChange = { v -> text = v; updateBlock(tab.id, block.id) { it.copy(text = v) } },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        DashBlockType.SHORTCUT -> {
            val label = heading ?: ((ShortcutTargets.firstOrNull { it.first == block.text }?.second ?: block.text) + " 열기")
            Row(
                Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .clickable { onNavigate(block.text) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
            }
        }
        DashBlockType.VALUE -> ValueBlockView(block, data.ctx, fill, onNavigate)
        DashBlockType.TEMPLATE -> TemplateBlockView(block, data.ctx, fill)
        DashBlockType.MACRO -> MacroBlockView(block, fill) { cfg -> viewModel.runMacro(tab.id, cfg, data.ctx, onNavigate) }
        else -> Unit
    }
}

/** This month as a small grid: shift colour behind the day, a dot when tasks are due. */
@Composable
private fun MiniMonth(today: LocalDate, tasks: List<Task>) {
    val month = YearMonth.from(today)
    val shifts = remember(tasks) { tasks.filter { it.isShiftTask() } }
    val plain = remember(tasks) { tasks.filter { it.isPlainTask() } }
    val lead = month.atDay(1).dayOfWeek.value % 7 // Sunday first
    val cells = lead + month.lengthOfMonth()
    Row(Modifier.fillMaxWidth()) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { i, d ->
            Text(d, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                color = when (i) { 0 -> OverdueRed.copy(alpha = 0.8f); 6 -> Color(0xFF5E87C9); else -> TextSecondary })
        }
    }
    Spacer(Modifier.height(4.dp))
    for (week in 0 until (cells + 6) / 7) {
        Row(Modifier.fillMaxWidth()) {
            for (col in 0 until 7) {
                val dayNum = week * 7 + col - lead + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (dayNum in 1..month.lengthOfMonth()) {
                        val day = month.atDay(dayNum)
                        val kind = shifts.firstOrNull { it.occursOn(day) }?.shiftKindOn(day)
                        val sc = kind?.let { shiftColor(it) }
                        val isToday = day == today
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            .background(sc?.copy(alpha = 0.18f) ?: Color.Transparent)
                            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier),
                            contentAlignment = Alignment.Center) {
                            Text("$dayNum", fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                            if (plain.any { it.occursOn(day) }) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp)
                                    .clip(CircleShape).background(sc ?: MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoneyStat(label: String, amount: Long, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(won(amount), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}
