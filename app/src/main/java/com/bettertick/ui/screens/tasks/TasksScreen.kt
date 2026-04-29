package com.bettertick.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import kotlin.math.roundToInt
import com.bettertick.ui.screens.calendar.components.WeekTimelineView
import com.bettertick.ui.screens.tasks.components.KanbanView
import com.bettertick.ui.screens.tasks.components.MoveTaskListSheet
import com.bettertick.ui.screens.tasks.components.QuickDateSettingsDialog
import com.bettertick.ui.screens.tasks.components.RecurringScope
import com.bettertick.ui.screens.tasks.components.RecurringScopeDialog
import com.bettertick.ui.screens.tasks.components.RescheduleQuickPickSheet
import com.bettertick.ui.screens.tasks.components.SwipeableTaskItem
import com.bettertick.ui.screens.tasks.components.TaskDatePickerSheet
import com.bettertick.ui.screens.tasks.components.TaskDetailSheet
import com.bettertick.ui.screens.tasks.components.TaskItem
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.util.DateUtils.toLocalDate
import com.bettertick.widget.util.WidgetDateUtils
import java.time.LocalDate

/** Sections rendered on the Tasks screen. Each maps to one of the date-based
 *  buckets computed from the task list; [label] + [color] drive the header
 *  render, and [order] is the canonical default order before any user
 *  customization. */
private enum class TaskSection(val label: String) {
    Overdue("기한 지남"),
    Today("오늘"),
    Future("예정"),
    NoDate("날짜 없음");
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onOpenDrawer: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val tags by viewModel.tags.collectAsState()

    // Per-section tag filter — `hiddenTagsBySection[section]` holds the
    // SnapshotStateMap of tagId → hidden bool for that section. A task is
    // dropped from a bucket if any of its tagIds appears with `true` in that
    // section's map. Each section keeps its own independent toggle state so
    // hiding "휴식" in 오늘 doesn't suppress it from 예정.
    val hiddenTagsBySection = remember {
        mutableStateMapOf<TaskSection, androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>>().also {
            TaskSection.entries.forEach { s -> it[s] = mutableStateMapOf() }
        }
    }

    // Pick the list view mode: defaults to "list", but if the current filter
    // is ByList and that list's viewType is "timetable", swap the body for a
    // week timetable instead of the bucketed task list.
    val activeListViewType = remember(currentFilter, lists) {
        (currentFilter as? TaskFilter.ByList)?.let { f ->
            lists.firstOrNull { it.id == f.listId }?.viewType
        } ?: "list"
    }

    val today = LocalDate.now()
    // Compute the five filtered buckets only when `tasks` changes, not on every
    // recomposition — in practice scroll triggers many recompositions and
    // re-filtering a long task list on each one produced visible jank.
    data class TaskBuckets(
        val overdue: List<Task>,
        val today: List<Task>,
        val future: List<Task>,
        val noDate: List<Task>,
        val completed: List<Task>
    )
    val hiddenBySection: Map<TaskSection, Set<String>> = TaskSection.entries.associateWith { s ->
        hiddenTagsBySection[s]?.filterValues { it }?.keys?.toSet() ?: emptySet()
    }
    val buckets: TaskBuckets = remember(tasks, today, hiddenBySection) {
        val overdue = mutableListOf<Task>()
        val todayList = mutableListOf<Task>()
        val future = mutableListOf<Task>()
        val noDate = mutableListOf<Task>()
        val completed = mutableListOf<Task>()
        fun isHidden(section: TaskSection, task: Task): Boolean {
            val hidden = hiddenBySection[section].orEmpty()
            return hidden.isNotEmpty() && task.tagIds.any { it in hidden }
        }
        for (t in tasks) {
            // 완료/포기 항목도 원래 속한 날짜 섹션에 그대로 둔다. 별도 버킷으로 빼면
            // 체크 직후 행이 사라진 듯 보여 UX가 어색함. TaskItem이 isCompleted/
            // isAbandoned 상태에 따라 체크 아이콘 + 취소선을 그리므로 시각적으론 충분.
            // 완료된 항목도 completedTasks에 함께 누적해 두면 다른 곳에서 필요 시
            // 참조 가능.
            if (t.isCompleted || t.isAbandoned) completed += t
            val d = t.dueDate?.toLocalDate()
            when {
                d == null -> if (!isHidden(TaskSection.NoDate, t)) noDate += t
                d.isBefore(today) -> if (!isHidden(TaskSection.Overdue, t)) overdue += t
                d == today -> if (!isHidden(TaskSection.Today, t)) todayList += t
                else -> if (!isHidden(TaskSection.Future, t)) future += t
            }
        }
        val repeatFirst = compareByDescending<Task> { !it.repeatRule.isNullOrBlank() }
            .thenBy { it.sortOrder }
        TaskBuckets(
            overdue.sortedWith(repeatFirst),
            todayList.sortedWith(repeatFirst),
            future.sortedWith(repeatFirst),
            noDate.sortedWith(repeatFirst),
            completed
        )
    }
    val overdueTasks = buckets.overdue
    val todayTasks = buckets.today
    val futureTasks = buckets.future
    val noDueDateTasks = buckets.noDate
    val completedTasks = buckets.completed

    // Section order + hidden set — hoisted here so both the list rendering
    // and the top-bar settings dialog can share the same state.
    val sectionOrder = remember {
        androidx.compose.runtime.mutableStateListOf<TaskSection>().also {
            it.addAll(TaskSection.entries)
        }
    }
    val hiddenSections = remember { mutableStateMapOf<TaskSection, Boolean>() }
    val expandedMap = remember {
        mutableStateMapOf<TaskSection, Boolean>().also {
            TaskSection.entries.forEach { s -> it[s] = true }
        }
    }
    var showSectionSettings by remember { mutableStateOf(false) }
    var detailFor by remember { mutableStateOf<Task?>(null) }
    var moveFor by remember { mutableStateOf<Task?>(null) }
    var rescheduleFor by remember { mutableStateOf<Task?>(null) }
    var datePickerFor by remember { mutableStateOf<Task?>(null) }
    var showQuickDateSettings by remember { mutableStateOf(false) }
    var recurringDeleteFor by remember { mutableStateOf<Task?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = viewModel.filterTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = { showSectionSettings = true }) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "섹션 설정",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground
            )
        )

        if (activeListViewType == "timetable") {
            // 시간표 view — reuse the calendar's week timeline to render the
            // filtered tasks as vertical time blocks. Its internal date state
            // defaults to today; this is view-only for now.
            val weekDates = remember(today) { WidgetDateUtils.weekDates(today) }
            var selectedDate by remember { mutableStateOf(today) }
            WeekTimelineView(
                weekDates = weekDates,
                allTasks = tasks,
                today = today,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            return@Column
        }

        if (activeListViewType == "kanban") {
            val activeList = remember(currentFilter, lists) {
                (currentFilter as? TaskFilter.ByList)?.let { f ->
                    lists.firstOrNull { it.id == f.listId }
                }
            }
            val selectedColumn by viewModel.selectedKanbanColumn.collectAsState()
            // Reset selection when we switch to a different list so a stale
            // column from the previous list doesn't leak through.
            LaunchedEffect(activeList?.id) {
                viewModel.setSelectedKanbanColumn("")
            }
            if (activeList != null) {
                KanbanView(
                    list = activeList,
                    tasks = tasks,
                    listNameById = { id -> lists.firstOrNull { it.id == id }?.name ?: "기본함" },
                    selectedColumn = selectedColumn,
                    onColumnSelected = { viewModel.setSelectedKanbanColumn(it) },
                    onAddColumn = { columnName ->
                        viewModel.updateList(
                            activeList.copy(
                                kanbanColumns = activeList.kanbanColumns + columnName
                            )
                        )
                    },
                    onToggleComplete = { task ->
                        viewModel.toggleComplete(task.id, !task.isCompleted)
                    },
                    onUpdateTask = { viewModel.updateTask(it) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    tags = viewModel.tags.collectAsState().value,
                    onCreateTag = { viewModel.createTag(it) }
                )
            }
            return@Column
        }

        fun bucketFor(s: TaskSection): List<Task> = when (s) {
            TaskSection.Overdue -> overdueTasks
            TaskSection.Today -> todayTasks
            TaskSection.Future -> futureTasks
            TaskSection.NoDate -> noDueDateTasks
        }
        fun colorFor(s: TaskSection): Color = when (s) {
            TaskSection.Overdue -> OverdueRed
            TaskSection.Today -> Orange
            TaskSection.Future, TaskSection.NoDate -> TextSecondary
        }

        // Task list — iterates over the configured order, skipping sections
        // the user has hidden or that have no items.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            sectionOrder.forEach { section ->
                if (hiddenSections[section] == true) return@forEach
                val bucket = bucketFor(section)
                if (bucket.isEmpty()) return@forEach
                val expanded = expandedMap[section] ?: true

                item(key = "header-${section.name}") {
                    SectionHeader(
                        title = section.label,
                        count = bucket.size,
                        color = colorFor(section),
                        expanded = expanded,
                        onToggle = { expandedMap[section] = !expanded }
                    )
                }
                if (expanded) {
                    items(bucket, key = { it.id }) { task ->
                        val taskTags = remember(task.tagIds, tags) {
                            task.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                        }
                        SwipeableTaskItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleComplete(task.id, !task.isCompleted) },
                            onMove = { moveFor = task },
                            onDelete = {
                                if (task.repeatRule.isNullOrBlank()) viewModel.deleteTask(task.id)
                                else recurringDeleteFor = task
                            },
                            onReschedule = { rescheduleFor = task },
                            onClick = { detailFor = task },
                            listName = lists.firstOrNull { it.id == task.listId }?.name ?: "기본함",
                            resolvedTags = taskTags
                        )
                    }
                }
            }
        }
    }

    if (showSectionSettings) {
        SectionSettingsDialog(
            order = sectionOrder,
            hidden = hiddenSections,
            onReorder = { newOrder ->
                sectionOrder.clear()
                sectionOrder.addAll(newOrder)
            },
            onToggleHidden = { s ->
                hiddenSections[s] = !(hiddenSections[s] ?: false)
            },
            tags = tags,
            hiddenTagsBySection = hiddenBySection,
            onToggleTag = { section, tagId ->
                val m = hiddenTagsBySection.getOrPut(section) { mutableStateMapOf() }
                m[tagId] = !(m[tagId] ?: false)
            },
            onDismiss = { showSectionSettings = false }
        )
    }

    moveFor?.let { task ->
        MoveTaskListSheet(
            lists = lists,
            currentListId = task.listId,
            onDismiss = { moveFor = null },
            onPick = { listId ->
                viewModel.updateTask(task.copy(listId = listId))
                moveFor = null
            }
        )
    }

    rescheduleFor?.let { task ->
        val scheduleToday = LocalDate.now()
        RescheduleQuickPickSheet(
            onDismiss = { rescheduleFor = null },
            onToday = {
                viewModel.updateTask(task.copy(dueDate = rescheduleTo(task, scheduleToday)))
                rescheduleFor = null
            },
            onTomorrow = {
                viewModel.updateTask(task.copy(dueDate = rescheduleTo(task, scheduleToday.plusDays(1))))
                rescheduleFor = null
            },
            onNextMonday = {
                viewModel.updateTask(task.copy(dueDate = rescheduleTo(task, nextMondayFrom(scheduleToday))))
                rescheduleFor = null
            },
            onPickDate = {
                rescheduleFor = null
                datePickerFor = task
            },
            onSkipRecurrence = { rescheduleFor = null },
            onDelete = { rescheduleFor = null },
            onCustomize = {
                rescheduleFor = null
                showQuickDateSettings = true
            }
        )
    }

    if (showQuickDateSettings) {
        QuickDateSettingsDialog(onDismiss = { showQuickDateSettings = false })
    }

    datePickerFor?.let { task ->
        val initial = remember(task.id) { LocalDate.now() }
        TaskDatePickerSheet(
            initialDate = initial,
            onDismiss = { datePickerFor = null },
            onDelete = { datePickerFor = null },
            onConfirm = { _, _, _, _, _ -> datePickerFor = null }
        )
    }

    recurringDeleteFor?.let { task ->
        RecurringScopeDialog(
            title = "반복 할일 삭제",
            body = "반복 작업을 삭제하고 있습니다. 삭제 범위를 확인해주세요.",
            onDismiss = { recurringDeleteFor = null },
            onChoice = { _ ->
                viewModel.deleteTask(task.id)
                recurringDeleteFor = null
            }
        )
    }

    detailFor?.let { task ->
        val listName = lists.firstOrNull { it.id == task.listId }?.name ?: "기본함"
        TaskDetailSheet(
            task = task,
            listName = listName,
            onDismiss = { detailFor = null },
            onUpdateTask = { updated ->
                viewModel.updateTask(updated)
                detailFor = null
            },
            onToggleComplete = {
                viewModel.toggleComplete(task.id, !task.isCompleted)
                detailFor = null
            },
            tags = tags,
            onCreateTag = { viewModel.createTag(it) }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chevron — down when expanded, right when collapsed.
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                else Icons.Default.KeyboardArrowRight,
            contentDescription = if (expanded) "접기" else "펼치기",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

/**
 * Settings dialog reached from the tasks top-bar gear. Each section row
 * carries a drag handle + a visibility toggle — long-press on the handle to
 * reorder, tap the eye to hide/show. Local-only state that roundtrips back
 * to TasksScreen via [onReorder] and [onToggleHidden].
 */
@Composable
private fun SectionSettingsDialog(
    order: List<TaskSection>,
    hidden: Map<TaskSection, Boolean>,
    onReorder: (List<TaskSection>) -> Unit,
    onToggleHidden: (TaskSection) -> Unit,
    tags: List<Tag>,
    hiddenTagsBySection: Map<TaskSection, Set<String>>,
    onToggleTag: (TaskSection, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Per-section expand state for the inline tag-filter sublist.
    val tagFilterExpanded = remember {
        mutableStateMapOf<TaskSection, Boolean>().also {
            TaskSection.entries.forEach { s -> it[s] = false }
        }
    }
    // Local draft of the order so we can animate items while the user drags
    // without sending a write on every pixel of movement.
    val draft = remember(order) {
        androidx.compose.runtime.mutableStateListOf<TaskSection>().also {
            it.addAll(order)
        }
    }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeightDp = 56.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeightDp.toPx() }

    Dialog(
        onDismissRequest = {
            onReorder(draft.toList())
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = "섹션 설정",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = "핸들을 길게 눌러 순서를 바꾸거나 👁을 눌러 숨길 수 있어요.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))

                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    draft.forEachIndexed { index, section ->
                        val isDragged = draggingIndex == index
                        val offsetY = if (isDragged) dragOffsetY.roundToInt() else 0
                        val isExpanded = tagFilterExpanded[section] == true
                        val sectionHiddenTags = hiddenTagsBySection[section].orEmpty()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, offsetY) }
                                .graphicsLayer {
                                    scaleX = if (isDragged) 1.02f else 1f
                                    scaleY = if (isDragged) 1.02f else 1f
                                    shadowElevation = if (isDragged) 10f else 0f
                                }
                                .background(
                                    if (isDragged) DarkSurfaceVariant else DarkCard,
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeightDp)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DragIndicator,
                                    contentDescription = "순서 변경",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerInput(section) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                    onReorder(draft.toList())
                                                },
                                                onDragCancel = {
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffsetY += amount.y
                                                    val draggedIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                    when {
                                                        dragOffsetY > rowHeightPx / 2 && draggedIdx < draft.lastIndex -> {
                                                            val moved = draft.removeAt(draggedIdx)
                                                            draft.add(draggedIdx + 1, moved)
                                                            draggingIndex = draggedIdx + 1
                                                            dragOffsetY -= rowHeightPx
                                                        }
                                                        dragOffsetY < -rowHeightPx / 2 && draggedIdx > 0 -> {
                                                            val moved = draft.removeAt(draggedIdx)
                                                            draft.add(draggedIdx - 1, moved)
                                                            draggingIndex = draggedIdx - 1
                                                            dragOffsetY += rowHeightPx
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )
                                Spacer(Modifier.width(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(enabled = tags.isNotEmpty()) {
                                            tagFilterExpanded[section] = !isExpanded
                                        }
                                ) {
                                    Text(
                                        text = section.label,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    if (tags.isNotEmpty()) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown
                                                else Icons.Default.KeyboardArrowRight,
                                            contentDescription = if (isExpanded) "태그 접기" else "태그 펼치기",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (sectionHiddenTags.isNotEmpty()) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "${sectionHiddenTags.size}개 숨김",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                val isHidden = hidden[section] == true
                                IconButton(onClick = { onToggleHidden(section) }) {
                                    Icon(
                                        imageVector = if (isHidden) Icons.Outlined.VisibilityOff
                                            else Icons.Outlined.Visibility,
                                        contentDescription = if (isHidden) "표시" else "숨기기",
                                        tint = if (isHidden) TextSecondary else Color.White
                                    )
                                }
                            }

                            if (isExpanded && tags.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 40.dp, end = 12.dp, bottom = 8.dp)
                                ) {
                                    tags.forEach { tag ->
                                        val tagHidden = tag.id in sectionHiddenTags
                                        val chipColor = runCatching {
                                            Color(android.graphics.Color.parseColor(tag.color))
                                        }.getOrDefault(Orange)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(chipColor.copy(alpha = 0.32f))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = tag.name,
                                                    fontSize = 12.sp,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(Modifier.weight(1f))
                                            IconButton(onClick = { onToggleTag(section, tag.id) }) {
                                                Icon(
                                                    imageVector = if (tagHidden) Icons.Outlined.VisibilityOff
                                                        else Icons.Outlined.Visibility,
                                                    contentDescription = if (tagHidden) "표시" else "숨기기",
                                                    tint = if (tagHidden) TextSecondary else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = {
                        onReorder(draft.toList())
                        onDismiss()
                    }) {
                        Text(
                            text = "완료",
                            color = Color(0xFF4A90E2),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun nextMondayFrom(from: LocalDate): LocalDate {
    val daysAhead = ((java.time.DayOfWeek.MONDAY.value - from.dayOfWeek.value + 7) % 7)
        .let { if (it == 0) 7 else it }
    return from.plusDays(daysAhead.toLong())
}

private fun rescheduleTo(task: Task, date: LocalDate): com.google.firebase.Timestamp {
    val existingLocal = task.dueDate?.toDate()?.toInstant()
        ?.atZone(java.time.ZoneId.systemDefault())
    val localTime = existingLocal?.toLocalTime() ?: java.time.LocalTime.MIDNIGHT
    val instant = date.atTime(localTime)
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
    return com.google.firebase.Timestamp(java.util.Date.from(instant))
}
