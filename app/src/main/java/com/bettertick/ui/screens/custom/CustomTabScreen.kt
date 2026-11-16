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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.Work
import kotlinx.coroutines.launch
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
    val diary: DiaryEntry?,
    val today: LocalDate
) {
    val ctx = DashContext(tasks, lists, habits, diary, today)
}

/** Saves a change to one block of [tabId], reading the latest copy of the tab. */
private fun updateBlock(tabId: String, blockId: String, change: (DashBlock) -> DashBlock) =
    CustomTabsPrefs.updateBlock(tabId, blockId, change)

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
    // "오늘" follows the clock: a tab left open past midnight moves on to the new day.
    val today by androidx.compose.runtime.produceState(LocalDate.now()) {
        while (true) {
            val now = java.time.LocalDateTime.now()
            val next = now.toLocalDate().plusDays(1).atStartOfDay()
            kotlinx.coroutines.delay(java.time.Duration.between(now, next).toMillis() + 1_000)
            value = LocalDate.now()
        }
    }
    val data = DashData(tasks, lists, habits, diary, today)
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    /** Checks a task off; "되돌리기" in the snackbar unchecks it again. */
    val toggleWithUndo: (Task, LocalDate) -> Unit = { t, day ->
        val wasDone = t.completedOn(day)
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        viewModel.toggle(t, day)
        if (!wasDone) scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar("'${t.title.take(20)}' 완료", actionLabel = "되돌리기",
                duration = androidx.compose.material3.SnackbarDuration.Short)
            if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.toggle(t.copy(isCompleted = true, completedDates = t.completedDates + day.toString()), day)
            }
        }
    }
    var menuFor by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    // 이벤트: 이 탭을 열 때.
    androidx.compose.runtime.LaunchedEffect(tabId) {
        MacroEngine.onOpen(context, TriggerType.TAB_OPEN, tabId, onNavigate = onNavigate)
    }

    var editing by rememberSaveable(tabId) {
        mutableStateOf((CustomTabsPrefs.justCreated == tabId).also { if (it) CustomTabsPrefs.justCreated = null })
    }
    var showAdd by remember { mutableStateOf(false) }
    var settingsFor by remember { mutableStateOf<String?>(null) }
    val tags by viewModel.tags.collectAsState()
    // A task tapped in a 할 일 블록: opens the same detail sheet as everywhere else.
    var openTask by remember { mutableStateOf<Pair<String, LocalDate?>?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
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
                // Room for the floating + button so the last block is never covered.
                .padding(horizontal = 16.dp).padding(bottom = 104.dp)
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
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GroupedCard)) {
                    TabActionRow("탭 복제", "블록·설정 그대로 새 탭으로", Icons.Outlined.ContentCopy) {
                        CustomTabsPrefs.duplicate(tab.id)?.let { copy ->
                            viewModel.addToTabBar(copy.id)
                            CustomTabsPrefs.justCreated = copy.id
                            onNavigate(copy.id)
                        }
                    }
                    TabActionRow("공유용으로 복사", "다른 폰에서 탭 바 › 내 탭 만들기 › 붙여넣기", Icons.Outlined.IosShare) {
                        CustomTabsPrefs.export(tab.id)?.let { text ->
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
                            android.widget.Toast.makeText(context, "탭을 복사했어요", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
                val showBlock: @Composable (DashBlock, Boolean) -> Unit = { b, fill ->
                    BlockFrame(b, data.lists, onLongPress = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        menuFor = b.id
                    }, onExpand = { CustomTabsPrefs.updateBlock(tab.id, b.id) { it.copy(collapsed = false) } }) {
                        DashBlockView(tab, b, data, viewModel, onNavigate, fill = fill,
                            onOpenTask = { t, d -> openTask = t.id to d }, onToggle = toggleWithUndo)
                    }
                }
                blockRows(tab.blocks).forEach { row ->
                    key(row.first().id) {
                        if (row.size == 1 && !row[0].half) {
                            showBlock(row[0], false)
                        } else {
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { b ->
                                    Box(Modifier.weight(1f).fillMaxHeight()) { showBlock(b, !b.collapsed) }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (tab.blocks.isNotEmpty()) {
                    Text("블록을 길게 누르면 설정 · 이동 · 복제 · 접기", color = TextSecondary.copy(alpha = 0.6f), fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
    androidx.compose.material3.SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }, containerColor = DarkSurface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
                Text("블록 추가", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                DashBlockType.sections.forEach { (section, types) ->
                    Text(section, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp))
                    types.forEach { type ->
                        Row(Modifier.fillMaxWidth().clickable {
                            showAdd = false
                            val fresh = CustomTabsPrefs.tab(tab.id) ?: return@clickable
                            val block = DashBlock(type = type, half = DashBlockType.halfByDefault(type),
                                text = DashBlockType.initialText(type))
                            CustomTabsPrefs.update(fresh.copy(blocks = fresh.blocks + block))
                            if (DashBlockType.needsSetup(type)) settingsFor = block.id
                        }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(blockIcon(type), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(DashBlockType.label(type), color = MaterialTheme.colorScheme.onBackground)
                                Text(DashBlockType.hint(type), color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    settingsFor?.let { id ->
        val block = tab.blocks.firstOrNull { it.id == id }
        if (block == null) settingsFor = null
        else BlockSettingsSheet(tab.id, block, lists, data.ctx, tags, onDismiss = { settingsFor = null }) {
            // Live preview: the block exactly as it will look, updating with every change.
            DashBlockView(tab, block.copy(collapsed = false), data, viewModel, onNavigate = {}, onToggle = { _, _ -> })
        }
    }
    menuFor?.let { id ->
        val block = tab.blocks.firstOrNull { it.id == id }
        if (block == null) menuFor = null
        else BlockMenuSheet(
            name = blockName(block, lists),
            block = block,
            index = tab.blocks.indexOf(block),
            count = tab.blocks.size,
            onDismiss = { menuFor = null },
            onSettings = { menuFor = null; settingsFor = id },
            onMove = { by -> CustomTabsPrefs.moveBlock(tab.id, id, by) },
            onToggleHalf = { CustomTabsPrefs.updateBlock(tab.id, id) { it.copy(half = !it.half) } },
            onToggleCollapse = { menuFor = null; CustomTabsPrefs.updateBlock(tab.id, id) { it.copy(collapsed = !it.collapsed) } },
            onDuplicate = { menuFor = null; CustomTabsPrefs.duplicateBlock(tab.id, id) },
            onDelete = {
                menuFor = null
                val before = CustomTabsPrefs.tab(tab.id)
                CustomTabsPrefs.removeBlock(tab.id, id)
                scope.launch {
                    val r = snackbar.showSnackbar("블록을 삭제했어요", actionLabel = "되돌리기",
                        duration = androidx.compose.material3.SnackbarDuration.Short)
                    if (r == androidx.compose.material3.SnackbarResult.ActionPerformed && before != null) {
                        CustomTabsPrefs.tab(tab.id)?.let { now ->
                            // Put it back where it was, keeping any other edits made since.
                            val restored = before.blocks.filter { b -> b.id == id || now.blocks.any { it.id == b.id } }
                                .map { b -> now.blocks.firstOrNull { it.id == b.id } ?: b }
                            CustomTabsPrefs.update(now.copy(blocks = restored + now.blocks.filter { n -> restored.none { it.id == n.id } }))
                        }
                    }
                }
            }
        )
    }
    openTask?.let { (id, day) ->
        val t = tasks.firstOrNull { it.id == id }
        if (t == null) openTask = null
        else com.bettertick.ui.screens.tasks.components.TaskDetailSheet(
            task = t,
            listName = lists.firstOrNull { it.id == t.listId }?.name ?: "기본함",
            onDismiss = { openTask = null },
            onUpdateTask = viewModel::updateTask,
            onToggleComplete = { viewModel.toggleFromDetail(t, day) },
            tags = tags,
            onCreateTag = { name -> viewModel.createTag(name) },
            lists = lists,
            occurrenceDate = day,
            onDelete = { del -> viewModel.deleteTask(del); openTask = null }
        )
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
        DashBlockType.LIST_TASKS, DashBlockType.TODAY_TASKS -> TaskViewConfig.decode(block.text, block.type).let { c ->
            "할 일 · " + (TaskRanges.firstOrNull { it.first == c.range }?.second ?: "") +
                (c.listIds.singleOrNull()?.let { id -> lists.firstOrNull { it.id == id }?.name?.let { " · $it" } } ?: "")
        }
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
                        .clickable { onSettings(block.id) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.RemoveCircleOutline, "삭제", tint = OverdueRed,
                        modifier = Modifier.size(22.dp).clip(CircleShape).clickable {
                            order.remove(block); commit()
                        })
                    Spacer(Modifier.width(12.dp))
                    Icon(blockIcon(block.type), null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(blockName(block, lists), color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(if (block.half) "절반 너비" else "전체 너비", "접힘".takeIf { block.collapsed })
                            .joinToString(" · "), color = TextSecondary, fontSize = 12.sp)
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
private fun BlockSettingsSheet(tabId: String, block: DashBlock, lists: List<TaskList>, ctx: DashContext,
                               tags: List<com.bettertick.data.model.Tag>, onDismiss: () -> Unit,
                               preview: @Composable () -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text(DashBlockType.label(block.type) + " 설정", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(vertical = 8.dp))
            SettingLabel("미리보기")
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))) {
                // Half blocks preview at half width so the layout reads true.
                Box(Modifier.fillMaxWidth(if (block.half) 0.5f else 1f)) { preview() }
            }

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

            SettingLabel("블록 색")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val accent = MaterialTheme.colorScheme.primary
                Box(Modifier.size(30.dp).clip(CircleShape).background(GroupedCard)
                    .border(if (block.color.isBlank()) 3.dp else 1.dp,
                        if (block.color.isBlank()) MaterialTheme.colorScheme.onBackground else TextSecondary, CircleShape)
                    .clickable { updateBlock(tabId, block.id) { it.copy(color = "") } })
                DashColors.forEach { (key, _) ->
                    Box(Modifier.size(30.dp).clip(CircleShape).background(dashColor(key, accent))
                        .then(if (key == block.color) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
                        .clickable { updateBlock(tabId, block.id) { it.copy(color = key) } })
                }
            }

            when (block.type) {
                DashBlockType.SHORTCUT -> {
                    SettingLabel("이동할 화면")
                    ChoiceList(ShortcutTargets, block.text) { v -> updateBlock(tabId, block.id) { it.copy(text = v) } }
                }
                DashBlockType.TODAY_TASKS, DashBlockType.LIST_TASKS -> TaskViewSettings(tabId, block, lists, tags, ctx.tasks)
                DashBlockType.QUICK_ADD -> {
                    SettingLabel("추가할 목록")
                    val choices = listOf("" to "기본함") + lists.map { it.id to it.name }
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
            .clip(RoundedCornerShape(16.dp)).background(blockBackground())
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp)
    ) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = blockHeadingColor(), fontWeight = FontWeight.SemiBold,
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
    fill: Boolean = false,
    onOpenTask: (Task, LocalDate?) -> Unit = { _, _ -> },
    onToggle: (Task, LocalDate) -> Unit = viewModel::toggle
) {
    val tint = block.color.takeIf { it.isNotBlank() }?.let { dashColor(it, MaterialTheme.colorScheme.primary) }
    androidx.compose.runtime.CompositionLocalProvider(LocalBlockTint provides tint) {
        DashBlockBody(tab, block, data, viewModel, onNavigate, fill, onOpenTask, onToggle)
    }
}

@Composable
private fun DashBlockBody(
    tab: CustomTab,
    block: DashBlock,
    data: DashData,
    viewModel: CustomTabViewModel,
    onNavigate: (String) -> Unit,
    fill: Boolean,
    onOpenTask: (Task, LocalDate?) -> Unit,
    onToggle: (Task, LocalDate) -> Unit
) {
    val today = data.today
    val tasks = data.tasks
    val heading = block.title.ifBlank { null }
    val context = androidx.compose.ui.platform.LocalContext.current
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
        DashBlockType.TODAY_TASKS, DashBlockType.LIST_TASKS -> TaskViewBlockView(
            block, tasks, data.lists, fill,
            onToggle = onToggle,
            onOpen = onOpenTask,
            onAdd = { title, listId, date -> viewModel.addTask(title, listId, date) },
            today = today
        )
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
            val streaks by viewModel.streaks.collectAsState()
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
                    Text(item.habit.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        color = if (item.done) TextSecondary else MaterialTheme.colorScheme.onBackground)
                    val streak = streaks[item.habit.id] ?: 0
                    if (streak >= 2) Text("🔥 ${streak}일", fontSize = 12.sp, color = Color(0xFFF2994A), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        DashBlockType.MINI_CALENDAR -> Card(heading, fill) {
            MiniMonth(today, tasks, onOpenTask = onOpenTask, onOpenCalendar = { onNavigate("calendar") })
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
            val items = remember(tasks, today) {
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
            val (done, total) = remember(tasks, today) {
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
        DashBlockType.MONEY -> Card(heading ?: "이번 달 가계부", fill) {
            val month = remember(today) { YearMonth.from(today) }
            val book = MoneyPrefs.book
            val income = book.incomeIn(month) + SalaryPrefs.total(month)
            val expense = book.expenseIn(month)
            val todayLines = book.linesIn(month).filter { it.date == today }
            var adding by remember { mutableStateOf<Boolean?>(null) } // null = closed, true = 수입, false = 지출
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onNavigate("money") }) {
                MoneyStat("수입", income, Color(0xFF4CA6F5), Modifier.weight(1f))
                MoneyStat("지출", expense, OverdueRed, Modifier.weight(1f))
                MoneyStat("남은 돈", income - expense, MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
            }
            if (income > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { (expense.toFloat() / income).coerceIn(0f, 1f) },
                    color = if (expense > income) OverdueRed else MaterialTheme.colorScheme.primary,
                    trackColor = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                Text("수입의 ${(expense * 100 / income)}% 사용", color = TextSecondary, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
            if (todayLines.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("오늘", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                todayLines.take(4).forEach { l ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(l.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text((if (l.signed > 0) "+" else "") + won(l.signed), fontSize = 13.sp,
                            color = if (l.signed > 0) Color(0xFF4CA6F5) else MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("− 지출 기록", color = OverdueRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(OverdueRed.copy(alpha = 0.12f))
                        .clickable { adding = false }.padding(horizontal = 12.dp, vertical = 7.dp))
                Text("+ 수입 기록", color = Color(0xFF4CA6F5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF4CA6F5).copy(alpha = 0.12f))
                        .clickable { adding = true }.padding(horizontal = 12.dp, vertical = 7.dp))
            }
            adding?.let { isIncome -> QuickMoneyDialog(isIncome, today, onDismiss = { adding = null }) }
        }
        DashBlockType.MEMO -> Card(heading, fill) {
            var text by remember(block.id) { mutableStateOf(block.text) }
            // A macro (메모에 기록) can add lines while the tab is open: show them.
            androidx.compose.runtime.LaunchedEffect(block.text) { if (block.text != text) text = block.text }
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
        DashBlockType.MACRO -> MacroBlockView(block, fill) { cfg -> viewModel.runMacro(context, tab.id, block.id, cfg, data.ctx, onNavigate) }
        else -> Unit
    }
}

/** This month as a small grid: shift colour behind the day, a dot when tasks are due. */
@Composable
private fun MiniMonth(
    today: LocalDate,
    tasks: List<Task>,
    onOpenTask: (Task, LocalDate?) -> Unit = { _, _ -> },
    onOpenCalendar: () -> Unit = {}
) {
    // ‹ › change the month; tapping a day lists that day right inside the block.
    var month by remember(today) { mutableStateOf(YearMonth.from(today)) }
    var selected by remember(today) { mutableStateOf<LocalDate?>(null) }
    val shifts = remember(tasks) { tasks.filter { it.isShiftTask() } }
    val plain = remember(tasks) { tasks.filter { it.isPlainTask() } }
    val lead = month.atDay(1).dayOfWeek.value % 7 // Sunday first
    val cells = lead + month.lengthOfMonth()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${month.year}년 ${month.monthValue}월", color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("‹", fontSize = 20.sp, color = TextSecondary,
            modifier = Modifier.clip(CircleShape).clickable { month = month.minusMonths(1); selected = null }.padding(horizontal = 10.dp))
        if (month != YearMonth.from(today)) {
            Text("오늘", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { month = YearMonth.from(today); selected = null }
                    .padding(horizontal = 6.dp, vertical = 4.dp))
        }
        Text("›", fontSize = 20.sp, color = TextSecondary,
            modifier = Modifier.clip(CircleShape).clickable { month = month.plusMonths(1); selected = null }.padding(horizontal = 10.dp))
    }
    Spacer(Modifier.height(6.dp))
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
                        val isSelected = day == selected
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            .background(when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                sc != null -> sc.copy(alpha = 0.18f)
                                else -> Color.Transparent
                            })
                            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { selected = if (isSelected) null else day },
                            contentAlignment = Alignment.Center) {
                            Text("$dayNum", fontSize = 12.sp,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> Color.White
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onBackground
                                })
                            if (plain.any { it.occursOn(day) }) {
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp)
                                    .clip(CircleShape).background(if (isSelected) Color.White else sc ?: MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
        }
    }
    selected?.let { day ->
        Spacer(Modifier.height(8.dp))
        val shift = shifts.firstOrNull { it.occursOn(day) }
        val dayTasks = plain.filter { it.occursOn(day) }.sortedBy { it.timeLabel() ?: "" }
        Text("${day.monthValue}월 ${day.dayOfMonth}일 " + day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN) +
            (shift?.let { " · " + it.shiftTitleOn(day) } ?: ""),
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (dayTasks.isEmpty()) Text("일정 없음", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        dayTasks.take(8).forEach { t ->
            val done = t.completedOn(day)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onOpenTask(t, day) }.padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(t.timeLabel() ?: "종일", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(44.dp))
                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    color = if (done) TextSecondary else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (done) TextDecoration.LineThrough else null)
            }
        }
        if (dayTasks.size > 8) Text("+${dayTasks.size - 8}개", color = TextSecondary, fontSize = 12.sp)
    }
    Text("달력 열기 ›", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenCalendar).padding(vertical = 4.dp))
}

/** 지출·수입 바로 기록: amount, category and a memo, saved to 가계부 for [day]. */
@Composable
private fun QuickMoneyDialog(income: Boolean, day: LocalDate, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    val categories = if (income) com.bettertick.ui.screens.tasks.IncomeCategories else com.bettertick.ui.screens.tasks.ExpenseCategories
    var category by remember { mutableStateOf(categories.first()) }
    var memo by remember { mutableStateOf("") }
    val value = amount.filter { it.isDigit() }.toLongOrNull() ?: 0L
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (income) "수입 기록" else "지출 기록") },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant).padding(12.dp)) {
                    if (amount.isEmpty()) Text("금액 (원)", color = TextSecondary)
                    BasicTextField(
                        value = amount,
                        onValueChange = { v -> amount = v.filter { it.isDigit() }.take(10) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (value > 0) Text(won(value), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c ->
                        val on = c == category
                        Text(c, fontSize = 13.sp, color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else DarkSurfaceVariant)
                                .clickable { category = c }.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant).padding(12.dp)) {
                    if (memo.isEmpty()) Text("메모 (선택)", color = TextSecondary)
                    BasicTextField(value = memo, onValueChange = { memo = it.take(40) }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(enabled = value > 0, onClick = {
                MoneyPrefs.saveEntry(com.bettertick.ui.screens.tasks.MoneyEntry(date = day, amount = value, income = income,
                    category = category, memo = memo.trim()))
                onDismiss()
            }) { Text("기록") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun MoneyStat(label: String, amount: Long, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(won(amount), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}


/** Icon shown for a block type in the add sheet, arrange list and folded blocks. */
internal fun blockIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    DashBlockType.TODAY -> Icons.Outlined.Today
    DashBlockType.TODAY_TASKS -> Icons.Outlined.CheckBox
    DashBlockType.LIST_TASKS -> Icons.Outlined.FilterList
    DashBlockType.QUICK_ADD -> Icons.Outlined.AddTask
    DashBlockType.STATS -> Icons.Outlined.DonutLarge
    DashBlockType.MINI_CALENDAR -> Icons.Outlined.CalendarMonth
    DashBlockType.WEEK_SHIFTS -> Icons.Outlined.Work
    DashBlockType.UPCOMING -> Icons.Outlined.Event
    DashBlockType.DDAY -> Icons.Outlined.Flag
    DashBlockType.HABITS -> Icons.Outlined.Repeat
    DashBlockType.DIARY -> Icons.Outlined.Book
    DashBlockType.MEMO -> Icons.Outlined.StickyNote2
    DashBlockType.MONEY -> Icons.Outlined.AccountBalanceWallet
    DashBlockType.VALUE -> Icons.Outlined.Functions
    DashBlockType.TEMPLATE -> Icons.Outlined.TextFields
    DashBlockType.MACRO -> Icons.Outlined.PlayCircle
    DashBlockType.SHORTCUT -> Icons.Outlined.OpenInNew
    else -> Icons.Outlined.Dashboard
}

/** Blocks with a text box of their own keep long-press for text selection. */
private fun hasOwnTextBox(type: String) = type == DashBlockType.MEMO || type == DashBlockType.QUICK_ADD

/**
 * Wraps a block in view mode: a long press anywhere opens its menu (watched in the
 * Initial pass so the block's own taps keep working, and the click under the finger
 * is swallowed once the menu opened). A folded block shows only its heading.
 */
@Composable
private fun BlockFrame(
    block: DashBlock,
    lists: List<TaskList>,
    onLongPress: () -> Unit,
    onExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    val longPressTimeout = androidx.compose.ui.platform.LocalViewConfiguration.current.longPressTimeoutMillis
    val latestLongPress = androidx.compose.runtime.rememberUpdatedState(onLongPress)
    val watch = if (hasOwnTextBox(block.type) && !block.collapsed) Modifier else Modifier.pointerInput(block.id) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            val slop = viewConfiguration.touchSlop
            var fired = false
            val released = withTimeoutOrNull(longPressTimeout) {
                while (true) {
                    val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                    if (!c.pressed) return@withTimeoutOrNull true
                    if ((c.position - down.position).getDistance() > slop) return@withTimeoutOrNull true
                }
                true
            }
            if (released == null) {
                fired = true
                latestLongPress.value()
            }
            if (fired) {
                // Swallow the rest of this touch so the row under the finger isn't also tapped.
                while (true) {
                    val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    e.changes.forEach { it.consume() }
                    if (e.changes.none { it.pressed }) break
                }
            }
        }
    }
    if (block.collapsed) {
        Row(
            Modifier.fillMaxWidth().then(watch).clip(RoundedCornerShape(14.dp)).background(GroupedCard)
                .clickable(onClick = onExpand).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(blockIcon(block.type), null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(blockName(block, lists), color = TextSecondary, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, "펼치기", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    } else {
        Box(Modifier.fillMaxWidth().then(watch)) { content() }
    }
}

/** Long-press menu of a block. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockMenuSheet(
    name: String,
    block: DashBlock,
    index: Int,
    count: Int,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onMove: (Int) -> Unit,
    onToggleHalf: () -> Unit,
    onToggleCollapse: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(blockIcon(block.type), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Up / down stay in the sheet so a block can be nudged several places in a row.
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuChip("위로", Icons.Outlined.KeyboardArrowUp, enabled = index > 0, modifier = Modifier.weight(1f)) { onMove(-1) }
                MenuChip("아래로", Icons.Outlined.KeyboardArrowDown, enabled = index < count - 1, modifier = Modifier.weight(1f)) { onMove(1) }
                MenuChip(if (block.half) "전체 너비" else "절반 너비", Icons.Outlined.ViewColumn, modifier = Modifier.weight(1f)) { onToggleHalf() }
            }
            MenuRow("블록 설정", Icons.Outlined.Tune, onSettings)
            MenuRow(if (block.collapsed) "펼치기" else "접기", if (block.collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess, onToggleCollapse)
            MenuRow("복제", Icons.Outlined.ContentCopy, onDuplicate)
            MenuRow("삭제", Icons.Outlined.Delete, onDelete, danger = true)
        }
    }
}

@Composable
private fun MenuChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true,
                     modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = if (enabled) MaterialTheme.colorScheme.onBackground else TextSecondary.copy(alpha = 0.4f)
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(DarkSurfaceVariant)
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, fontSize = 12.sp)
    }
}

@Composable
private fun MenuRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) OverdueRed else TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = if (danger) OverdueRed else MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun TabActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
