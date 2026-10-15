package com.bettertick.ui.screens.tasks

import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.ThemeState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
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
    val buckets: TaskBuckets = remember(tasks, today, hiddenBySection, TaskListPrefs.sortBy) {
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
            // TickTick: 완료·포기한 항목은 맨 아래 '완료됨' 그룹으로 모은다
            // (목록 ⋮ > 완료 항목 표시/숨기기).
            if (t.isCompleted || t.isAbandoned) { completed += t; continue }
            val d = t.dueDate?.toLocalDate()
            when {
                d == null -> if (!isHidden(TaskSection.NoDate, t)) noDate += t
                d.isBefore(today) -> if (!isHidden(TaskSection.Overdue, t)) overdue += t
                d == today -> if (!isHidden(TaskSection.Today, t)) todayList += t
                else -> if (!isHidden(TaskSection.Future, t)) future += t
            }
        }
        val order = TaskListPrefs.comparator()
        TaskBuckets(
            overdue.sortedWith(order),
            todayList.sortedWith(order),
            future.sortedWith(order),
            noDate.sortedWith(order),
            completed.sortedByDescending { it.completedAt?.seconds ?: it.updatedAt.seconds }
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

    // 목록 ⋮ menu state
    var showListMenu by remember { mutableStateOf(false) }
    var showGroupSort by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var batchDatePicker by remember { mutableStateOf(false) }
    var batchMove by remember { mutableStateOf(false) }
    var batchDeleteConfirm by remember { mutableStateOf(false) }
    val groupExpanded = remember { mutableStateMapOf<String, Boolean>() }
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(selectionMode) {
        TaskListPrefs.selectionActive = selectionMode
        onDispose { TaskListPrefs.selectionActive = false }
    }
    fun exitSelection() { selectionMode = false; selectedIds.clear() }
    fun dateGroups(): List<TaskGroup> = sectionOrder.filter { hiddenSections[it] != true }.mapNotNull { s ->
        val b = when (s) {
            TaskSection.Overdue -> overdueTasks
            TaskSection.Today -> todayTasks
            TaskSection.Future -> futureTasks
            TaskSection.NoDate -> noDueDateTasks
        }
        if (b.isEmpty()) null
        else TaskGroup("date:${s.name}", s.label, b, isOverdue = s == TaskSection.Overdue)
    }
    fun currentGroups(open: List<Task>, lists: List<com.bettertick.data.model.TaskList>, tags: List<Tag>): List<TaskGroup> =
        if (TaskListPrefs.groupBy == TaskGroupBy.DATE) dateGroups()
        else groupOpenTasks(open, TaskListPrefs.groupBy, lists, tags, TaskListPrefs.comparator())
    androidx.activity.compose.BackHandler(enabled = selectionMode) { exitSelection() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar — selection mode swaps in a count + close/select-all bar.
        if (selectionMode) {
            TopAppBar(
                title = {
                    Text(
                        "${selectedIds.size}개 선택됨",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { exitSelection() }) {
                        Icon(Icons.Default.Close, contentDescription = "선택 종료", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    val allIds = tasks.filter { !it.isCompleted && !it.isAbandoned }.map { it.id }
                    androidx.compose.material3.TextButton(onClick = {
                        if (selectedIds.size == allIds.size) selectedIds.clear()
                        else { selectedIds.clear(); selectedIds.addAll(allIds) }
                    }) {
                        Text(
                            if (selectedIds.size == allIds.size && allIds.isNotEmpty()) "선택 해제" else "전체 선택",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        } else
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
                Box {
                    IconButton(onClick = { showListMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    TaskListMenu(
                        expanded = showListMenu,
                        onDismiss = { showListMenu = false },
                        onGroupSort = { showGroupSort = true },
                        onSelect = { selectionMode = true },
                        onSectionSettings = { showSectionSettings = true },
                        onShare = {
                            val groups = currentGroups(
                                tasks.filter { !it.isCompleted && !it.isAbandoned }, lists, tags
                            )
                            shareText(context, buildTaskListShareText(viewModel.filterTitle, groups))
                        }
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

        val openTasks = remember(tasks) { tasks.filter { !it.isCompleted && !it.isAbandoned } }
        val archive = currentFilter.isArchive
        // 완료 / 하지 않음 lists: finished tasks grouped by when they ended.
        val groups = if (archive) archiveGroups(completedTasks) else currentGroups(openTasks, lists, tags)
        val completedGroup = if (!archive && TaskListPrefs.showCompleted && completedTasks.isNotEmpty())
            TaskGroup("completed", "완료됨", completedTasks) else null

        // Task list — groups per 목록 ⋮ > 그룹 및 정렬, then 완료됨 at the bottom.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            (groups + listOfNotNull(completedGroup)).forEach { group ->
                // 완료됨 starts collapsed, like TickTick.
                val expanded = groupExpanded[group.key] ?: (group.key != "completed")
                if (group.title.isNotEmpty()) {
                    item(key = "header-${group.key}") {
                        SectionHeader(
                            title = group.title,
                            count = group.tasks.size,
                            color = if (group.isOverdue) OverdueRed else TextSecondary,
                            expanded = expanded,
                            onToggle = { groupExpanded[group.key] = !expanded }
                        )
                    }
                }
                if (expanded) {
                    items(group.tasks, key = { "${group.key}-${it.id}" }) { task ->
                        val taskTags = remember(task.tagIds, tags) {
                            task.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                        }
                        val listName = lists.firstOrNull { it.id == task.listId }?.name ?: "기본함"
                        if (selectionMode) {
                            SelectableTaskRow(
                                task = task,
                                selected = task.id in selectedIds,
                                listName = listName,
                                tags = taskTags,
                                onToggle = {
                                    if (task.id in selectedIds) selectedIds.remove(task.id) else selectedIds.add(task.id)
                                }
                            )
                        } else {
                            SwipeableTaskItem(
                                task = task,
                                onToggleComplete = {
                                    if (task.isAbandoned) viewModel.updateTask(task.copy(isAbandoned = false))
                                    else viewModel.toggleComplete(task.id, !task.isCompleted)
                                },
                                onMove = { moveFor = task },
                                onDelete = {
                                    if (task.repeatRule.isNullOrBlank()) viewModel.deleteTask(task.id)
                                    else recurringDeleteFor = task
                                },
                                onReschedule = { rescheduleFor = task },
                                onClick = { detailFor = task },
                                listName = listName,
                                resolvedTags = taskTags,
                                onLongClick = {
                                    selectionMode = true
                                    if (task.id !in selectedIds) selectedIds.add(task.id)
                                }
                            )
                        }
                    }
                }
            }
            if (groups.isEmpty() && completedGroup == null) {
                item(key = "empty") { EmptyTaskList() }
            }
        }

        if (selectionMode) {
            val selectedTasks = tasks.filter { it.id in selectedIds }
            BatchActionBar(
                enabled = selectedTasks.isNotEmpty(),
                onComplete = {
                    selectedTasks.forEach { viewModel.toggleComplete(it.id, true) }
                    exitSelection()
                },
                onDate = { batchDatePicker = true },
                onMove = { batchMove = true },
                onPriority = { lvl ->
                    selectedTasks.forEach { viewModel.updateTask(it.copy(priority = lvl)) }
                    exitSelection()
                },
                onDelete = { batchDeleteConfirm = true }
            )
        }
    }

    if (showGroupSort) {
        GroupSortSheet(onDismiss = { showGroupSort = false })
    }

    if (batchDatePicker) {
        TaskDatePickerSheet(
            initialDate = LocalDate.now(),
            onDismiss = { batchDatePicker = false },
            onDelete = {
                tasks.filter { it.id in selectedIds }.forEach { viewModel.updateTask(it.copy(dueDate = null)) }
                batchDatePicker = false
                exitSelection()
            },
            onConfirm = { d, _, _, _, _ ->
                tasks.filter { it.id in selectedIds }.forEach { viewModel.updateTask(it.copy(dueDate = rescheduleTo(it, d))) }
                batchDatePicker = false
                exitSelection()
            }
        )
    }

    if (batchMove) {
        MoveTaskListSheet(
            lists = lists,
            currentListId = "",
            onDismiss = { batchMove = false },
            onPick = { listId ->
                tasks.filter { it.id in selectedIds }.forEach { viewModel.updateTask(it.copy(listId = listId)) }
                batchMove = false
                exitSelection()
            }
        )
    }

    if (batchDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { batchDeleteConfirm = false },
            containerColor = DarkSurface,
            title = { Text("${selectedIds.size}개의 할일을 삭제할까요?", color = TextPrimary) },
            text = { Text("삭제한 할일은 되돌릴 수 없습니다.", color = TextSecondary) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    selectedIds.toList().forEach { viewModel.deleteTask(it) }
                    batchDeleteConfirm = false
                    exitSelection()
                }) { Text("삭제", color = OverdueRed) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { batchDeleteConfirm = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
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
    // TickTick section header: compact bold label + gray count on the left,
    // a small collapse chevron on the right. Overdue keeps its red label.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (color == OverdueRed) OverdueRed else TextPrimary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = count.toString(),
            fontSize = 13.sp,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                else Icons.Default.KeyboardArrowRight,
            contentDescription = if (expanded) "접기" else "펼치기",
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
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
                    color = TextPrimary,
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
                                        color = TextPrimary,
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
                                        tint = if (isHidden) TextSecondary else TextPrimary
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
                                        }.getOrDefault(MaterialTheme.colorScheme.primary)
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
                                                    color = TextPrimary
                                                )
                                            }
                                            Spacer(Modifier.weight(1f))
                                            IconButton(onClick = { onToggleTag(section, tag.id) }) {
                                                Icon(
                                                    imageVector = if (tagHidden) Icons.Outlined.VisibilityOff
                                                        else Icons.Outlined.Visibility,
                                                    contentDescription = if (tagHidden) "표시" else "숨기기",
                                                    tint = if (tagHidden) TextSecondary else TextPrimary
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
                            color = ThemeState.accentColor,
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

/** Groups finished tasks by completion day: 오늘 / 어제 / 최근 7일 / 이전. */
private fun archiveGroups(done: List<Task>): List<TaskGroup> {
    val today = LocalDate.now()
    fun dayOf(t: Task): LocalDate? = (t.completedAt ?: t.abandonedAt ?: t.updatedAt).toLocalDate()
    val buckets = linkedMapOf(
        "오늘" to mutableListOf<Task>(),
        "어제" to mutableListOf(),
        "최근 7일" to mutableListOf(),
        "이전" to mutableListOf()
    )
    done.sortedByDescending { (it.completedAt ?: it.abandonedAt ?: it.updatedAt).seconds }.forEach { t ->
        val d = dayOf(t)
        val key = when {
            d == today -> "오늘"
            d == today.minusDays(1) -> "어제"
            d != null && d.isAfter(today.minusDays(7)) -> "최근 7일"
            else -> "이전"
        }
        buckets.getValue(key) += t
    }
    return buckets.filterValues { it.isNotEmpty() }.map { (k, v) -> TaskGroup("archive:$k", k, v) }
}
