package com.bettertick.ui.screens.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.Task
import com.bettertick.data.model.completedOn
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.occursOn
import com.bettertick.data.model.shiftKindOn
import com.bettertick.data.model.shiftTitleOn
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * 내 탭: a tab the user built out of blocks. "편집" switches to arrange mode, where
 * blocks are dragged into order (long-press the handle), removed or added, and the
 * tab's name and icon are changed.
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
    var editing by rememberSaveable(tabId) {
        mutableStateOf((CustomTabsPrefs.justCreated == tabId).also { if (it) CustomTabsPrefs.justCreated = null })
    }
    var showAdd by remember { mutableStateOf(false) }
    var pickShortcut by remember { mutableStateOf(false) }
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
                .padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (editing) {
                TabSettings(tab)
                Text("블록 · 핸들을 길게 눌러 순서를 바꾸세요", style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                ArrangeBlocks(tab)
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
                tab.blocks.forEach { block ->
                    key(block.id) {
                        DashBlockView(tab, block, tasks, onToggle = viewModel::toggle, onNavigate = onNavigate)
                    }
                }
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }, containerColor = DarkSurface) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
                Text("블록 추가", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                DashBlockType.catalog.forEach { (type, label) ->
                    Text(label, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth().clickable {
                            showAdd = false
                            if (type == DashBlockType.SHORTCUT) pickShortcut = true
                            else CustomTabsPrefs.update(tab.copy(blocks = tab.blocks + DashBlock(type = type)))
                        }.padding(horizontal = 20.dp, vertical = 14.dp))
                }
            }
        }
    }
    if (pickShortcut) {
        ModalBottomSheet(onDismissRequest = { pickShortcut = false }, containerColor = DarkSurface) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp)) {
                Text("어디로 가는 버튼인가요?", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                ShortcutTargets.forEach { (target, label) ->
                    Text(label, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth().clickable {
                            pickShortcut = false
                            val fresh = CustomTabsPrefs.tab(tab.id) ?: return@clickable
                            CustomTabsPrefs.update(fresh.copy(blocks = fresh.blocks + DashBlock(type = DashBlockType.SHORTCUT, text = target)))
                        }.padding(horizontal = 20.dp, vertical = 14.dp))
                }
            }
        }
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
private fun ArrangeBlocks(tab: CustomTab) {
    val order = remember(tab.id) { mutableStateListOf<DashBlock>().apply { addAll(tab.blocks) } }
    // Pick up blocks added / removed elsewhere while keeping the dragged order.
    if (order.map { it.id }.toSet() != tab.blocks.map { it.id }.toSet()) {
        order.clear(); order.addAll(tab.blocks)
    }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    var draggingId by remember { mutableStateOf<String?>(null) }
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
                        }
                        .background(if (dragging) DarkSurfaceVariant else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.RemoveCircleOutline, "삭제", tint = OverdueRed,
                        modifier = Modifier.size(22.dp).clip(CircleShape).clickable {
                            order.remove(block); commit()
                        })
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (block.type == DashBlockType.SHORTCUT)
                            "바로가기 · " + (ShortcutTargets.firstOrNull { it.first == block.text }?.second ?: block.text)
                        else DashBlockType.label(block.type),
                        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Outlined.DragHandle, "순서 바꾸기", tint = TextSecondary,
                        modifier = Modifier.size(28.dp).pointerInput(block.id) {
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
                                onDragEnd = { draggingId = null; dragY = 0f; commit() },
                                onDragCancel = { draggingId = null; dragY = 0f; commit() }
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
private fun Card(title: String?, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GroupedCard).padding(16.dp)) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun DashBlockView(
    tab: CustomTab,
    block: DashBlock,
    tasks: List<Task>,
    onToggle: (Task, LocalDate) -> Unit,
    onNavigate: (String) -> Unit
) {
    val today = remember { LocalDate.now() }
    when (block.type) {
        DashBlockType.TODAY -> Card(null) {
            val shift = tasks.firstOrNull { it.isShiftTask() && it.occursOn(today) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${today.monthValue}월 ${today.dayOfMonth}일 " +
                        today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    val left = tasks.count { !it.isShiftTask() && it.parentId.isBlank() && it.occursOn(today) && !it.completedOn(today) }
                    Text(if (left == 0) "오늘 남은 할 일이 없어요" else "오늘 남은 할 일 ${left}개",
                        color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                if (shift != null) {
                    val kind = shift.shiftKindOn(today)
                    val c = shiftColor(kind) ?: MaterialTheme.colorScheme.primary
                    Text(shift.shiftTitleOn(today), color = c, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
        DashBlockType.TODAY_TASKS -> Card("오늘 할 일") {
            val list = remember(tasks) {
                tasks.filter { !it.isShiftTask() && it.parentId.isBlank() && !it.isAbandoned && it.occursOn(today) }
                    .sortedWith(compareBy({ it.completedOn(today) }, { it.timeLabel() != null }, { it.dueDate?.seconds ?: 0L }))
            }
            if (list.isEmpty()) Text("오늘 할 일이 없어요", color = TextSecondary)
            list.take(12).forEach { t ->
                val done = t.completedOn(today)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onToggle(t, today) }.padding(vertical = 6.dp),
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
            if (list.size > 12) Text("+${list.size - 12}개 더", color = TextSecondary, fontSize = 13.sp,
                modifier = Modifier.clickable { onNavigate("tasks") }.padding(top = 4.dp))
        }
        DashBlockType.WEEK_SHIFTS -> Card("이번 주 근무") {
            val monday = remember(today) { today.with(DayOfWeek.MONDAY) }
            val shifts = remember(tasks) { tasks.filter { it.isShiftTask() } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0L until 7L).forEach { offset ->
                    val day = monday.plusDays(offset)
                    val s = shifts.firstOrNull { it.occursOn(day) }
                    val kind = s?.shiftKindOn(day)
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
        DashBlockType.UPCOMING -> Card("다가오는 일정") {
            val items = remember(tasks) {
                (1L..14L).flatMap { d ->
                    val day = today.plusDays(d)
                    tasks.filter { !it.isShiftTask() && it.parentId.isBlank() && !it.isAbandoned && it.occursOn(day) && !it.completedOn(day) }
                        .map { day to it }
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
        DashBlockType.MONEY -> Card("이번 달 가계부") {
            val month = remember(today) { YearMonth.from(today) }
            val book = MoneyPrefs.book
            val income = book.incomeIn(month) + SalaryPrefs.total(month)
            val expense = book.expenseIn(month)
            Row(Modifier.fillMaxWidth().clickable { onNavigate("money") }) {
                MoneyStat("수입", income, Color(0xFF4CA6F5), Modifier.weight(1f))
                MoneyStat("지출", expense, OverdueRed, Modifier.weight(1f))
                MoneyStat("남은 돈", income - expense, MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
            }
        }
        DashBlockType.MEMO -> Card(null) {
            var text by remember(block.id) { mutableStateOf(block.text) }
            Box {
                if (text.isEmpty()) Text("메모를 적어 두세요", color = TextSecondary)
                BasicTextField(
                    value = text,
                    onValueChange = { v ->
                        text = v
                        val fresh = CustomTabsPrefs.tab(tab.id) ?: return@BasicTextField
                        CustomTabsPrefs.update(fresh.copy(blocks = fresh.blocks.map { if (it.id == block.id) it.copy(text = v) else it }))
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        DashBlockType.SHORTCUT -> {
            val label = ShortcutTargets.firstOrNull { it.first == block.text }?.second ?: block.text
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .clickable { onNavigate(block.text) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$label 열기", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
            }
        }
        else -> Unit
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
