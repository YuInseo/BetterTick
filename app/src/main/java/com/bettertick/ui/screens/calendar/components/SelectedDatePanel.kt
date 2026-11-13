package com.bettertick.ui.screens.calendar.components

import com.bettertick.ui.theme.TextPrimary
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.withLeaveToggled
import com.bettertick.data.model.canNestUnder
import com.bettertick.data.model.groupedUnderParents
import com.bettertick.data.model.isSubtask
import com.bettertick.ui.screens.tasks.components.QuickDateSettingsDialog
import com.bettertick.ui.screens.tasks.components.RecurringScope
import com.bettertick.ui.screens.tasks.components.RecurringScopeDialog
import com.bettertick.ui.screens.tasks.components.RescheduleQuickPickSheet
import com.bettertick.ui.screens.tasks.components.SwipeableTaskItem
import com.bettertick.ui.screens.tasks.components.TaskDatePickerSheet
import com.bettertick.ui.screens.tasks.components.TaskDetailSheet
import com.bettertick.ui.screens.tasks.components.TaskItem
import com.bettertick.ui.theme.AbandonedBlue
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Holds a task being dragged over the calendar so the parent (CalendarScreen)
 * can render a floating preview and let date cells claim it on release.
 */
data class ActiveDrag(
    val task: Task,
    val startOffset: androidx.compose.ui.geometry.Offset,
    val currentOffset: androidx.compose.ui.geometry.Offset
)

/**
 * Inline task list for the selected calendar date.
 *
 * Active tasks render in the top card; completed + abandoned tasks appear
 * below in a muted "완료" section. Long-press on any row opens an action
 * sheet (상단 고정 / 공유 / 계획 취소 / 삭제). Long-press + drag emits a
 * floating preview that the caller can handle — drop on a date cell moves
 * the task's due date.
 */
@Composable
fun SelectedDatePanel(
    tasks: List<Task>,
    selectedDate: LocalDate,
    onToggleComplete: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
    detailListName: (Task) -> String = { "기본함" },
    onUpdateTask: (Task) -> Unit = {},
    onAbandon: (Task) -> Unit = {},
    onUnabandon: (Task) -> Unit = {},
    onDelete: (Task) -> Unit = {},
    onSkipOccurrence: (Task, LocalDate) -> Unit = { _, _ -> },
    onDragTaskUpdate: (ActiveDrag?) -> Unit = {},
    onDragTaskRelease: (Task) -> Unit = {},
    /** Drag released inside the list — new top-to-bottom ids for the day. */
    onReorder: (List<String>) -> Unit = {},
    /** Checkbox long-press: 완료 / 취소됨 / 내일로 미루기 (task, action). */
    onStatusAction: (Task, com.bettertick.data.model.TaskStatusAction) -> Unit = { _, _ -> },
    /** Dropped onto the middle of another row: nest (child, parent). */
    onNest: (Task, Task) -> Unit = { _, _ -> },
    /** Subtask dragged out of its parent's group (or 분리): (child, day). */
    onUnnest: (Task, LocalDate) -> Unit = { _, _ -> },
    tags: List<Tag> = emptyList(),
    onCreateTag: suspend (String) -> String = { "" }
) {
    val listPadding = remember { PaddingValues(vertical = 10.dp) }
    val shape = remember { RoundedCornerShape(18.dp) }

    // Subtasks sit right under their parent.
    val active = tasks.filterNot { it.isCompleted || it.isAbandoned }.groupedUnderParents()
    val done = tasks.filter { it.isCompleted || it.isAbandoned }

    var rescheduleFor by remember { mutableStateOf<Task?>(null) }
    var datePickerFor by remember { mutableStateOf<Task?>(null) }
    var detailFor by remember { mutableStateOf<Task?>(null) }
    var actionsFor by remember { mutableStateOf<Task?>(null) }
    var showQuickDateSettings by remember { mutableStateOf(false) }
    // Pending delete on a recurring task — routed through the scope dialog
    // so the user picks between skipping this occurrence vs deleting the
    // whole series.
    var recurringDeleteFor by remember { mutableStateOf<Task?>(null) }

    // Drag-to-reorder: row / list bounds in root space, plus the live drag.
    val rowBounds = remember { mutableStateMapOf<String, Rect>() }
    var listBounds by remember { mutableStateOf<Rect?>(null) }
    var localDrag by remember { mutableStateOf<ActiveDrag?>(null) }
    val dragged = localDrag?.task?.takeIf { d -> active.any { it.id == d.id } }
    val others = if (dragged == null) active else active.filterNot { it.id == dragged.id }
    // Over the middle of a row = nest under it; near a row's edge = reorder.
    val nestTarget: Task? = localDrag?.currentOffset?.let { p ->
        if (dragged == null || listBounds?.contains(p) != true) null
        else others.firstOrNull { t ->
            val r = rowBounds[t.id] ?: return@firstOrNull false
            val band = r.height * 0.28f
            p.y > r.top + band && p.y < r.bottom - band && dragged.canNestUnder(t, tasks)
        }
    }
    val insertAt: Int? = localDrag?.currentOffset?.let { p ->
        if (dragged == null || nestTarget != null || listBounds?.contains(p) != true) null
        else others.count { t -> rowBounds[t.id]?.let { it.center.y < p.y } == true }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Active (top) card — rounded both ends, scrollable.
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(DarkCard)
        ) {
            if (active.isEmpty() && done.isEmpty()) {
                EmptyState(modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp))
            } else if (active.isEmpty()) {
                Spacer(Modifier.height(8.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { c ->
                            val pos = c.positionInRoot()
                            listBounds = Rect(pos.x, pos.y, pos.x + c.size.width, pos.y + c.size.height)
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = listPadding
                ) {
                    items(active, key = { it.id }) { task ->
                        val taskTags = remember(task.tagIds, tags) {
                            task.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                        }
                        val othersIdx = others.indexOfFirst { it.id == task.id }
                        val lineAbove = insertAt != null && othersIdx >= 0 && othersIdx == insertAt
                        val lineBelow = insertAt != null && othersIdx >= 0 &&
                            othersIdx == others.lastIndex && insertAt == others.size
                        val nestedRow = task.isSubtask && active.any { it.id == task.parentId }
                        val isNestTarget = nestTarget?.id == task.id
                        val accent = MaterialTheme.colorScheme.primary
                        Column(
                            modifier = Modifier
                                .onGloballyPositioned { c ->
                                    val pos = c.positionInRoot()
                                    rowBounds[task.id] = Rect(pos.x, pos.y, pos.x + c.size.width, pos.y + c.size.height)
                                }
                                .alpha(if (dragged?.id == task.id) 0.35f else 1f)
                                .then(
                                    if (nestedRow) Modifier
                                        .drawBehind {
                                            val x = 14.dp.toPx()
                                            drawLine(
                                                SubtaskGuide,
                                                Offset(x, 0f),
                                                Offset(x, size.height),
                                                strokeWidth = 1.5.dp.toPx()
                                            )
                                        }
                                        .padding(start = 22.dp)
                                    else Modifier
                                )
                                .then(
                                    if (isNestTarget) Modifier
                                        .padding(horizontal = 6.dp)
                                        .border(2.dp, accent, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                        ) {
                        if (lineAbove) ReorderLine()
                        DraggableTaskRow(
                            task = task,
                            selectedDate = selectedDate,
                            listName = detailListName(task),
                            onToggleComplete = { onToggleComplete(task) },
                            onClick = {
                                onTaskClick(task)
                                detailFor = task
                            },
                            onLongPressActions = { actionsFor = task },
                            onReschedule = { rescheduleFor = task },
                            onDelete = {
                                // Recurring tasks route through the scope
                                // dialog; non-recurring go straight to
                                // delete like before.
                                if (task.repeatRule.isNullOrBlank()) onDelete(task)
                                else recurringDeleteFor = task
                            },
                            onDragUpdate = { d ->
                                localDrag = d
                                onDragTaskUpdate(d)
                            },
                            onDragRelease = { t ->
                                val idx = insertAt
                                val target = nestTarget
                                if (target != null && dragged != null) {
                                    onNest(dragged, target)
                                } else if (idx != null && dragged != null) {
                                    val newOrder = others.toMutableList().apply { add(idx.coerceIn(0, size), dragged) }
                                    val ids = newOrder.map { it.id }
                                    if (ids != active.map { it.id }) onReorder(ids)
                                    // A subtask dropped outside its parent's group is let go.
                                    if (dragged.isSubtask) {
                                        val parentIdx = others.indexOfFirst { it.id == dragged.parentId }
                                        if (parentIdx >= 0) {
                                            var groupEnd = parentIdx + 1
                                            while (groupEnd < others.size && others[groupEnd].parentId == dragged.parentId) groupEnd++
                                            if (idx !in (parentIdx + 1)..groupEnd) onUnnest(dragged, selectedDate)
                                        }
                                    }
                                }
                                localDrag = null
                                onDragTaskRelease(t)
                            },
                            resolvedTags = taskTags,
                            onToggleLeave = { day, type -> onUpdateTask(task.withLeaveToggled(day, type)) },
                            onStatusAction = { action -> onStatusAction(task, action) }
                        )
                        if (isNestTarget) {
                            Text(
                                text = "'${task.title}'의 하위 할일로 넣기",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accent,
                                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                            )
                        }
                        if (lineBelow) ReorderLine()
                        }
                    }
                }
            }
        }

        // Completed / abandoned card — only if any
        if (done.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(DarkCard)
            ) {
                Text(
                    text = "완료 & 포기",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                done.forEach { task ->
                    val taskTags = remember(task.tagIds, tags) {
                        task.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                    }
                    TaskItem(
                        task = task,
                        onToggleComplete = {
                            // Toggling a completed/abandoned row restores it
                            when {
                                task.isCompleted -> onToggleComplete(task)
                                task.isAbandoned -> onUnabandon(task)
                            }
                        },
                        onClick = { detailFor = task },
                        overrideDate = selectedDate,
                        listName = detailListName(task),
                        resolvedTags = taskTags,
                        onStatusAction = { action -> onStatusAction(task, action) }
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    rescheduleFor?.let { task ->
        RescheduleQuickPickSheet(
            onDismiss = { rescheduleFor = null },
            onToday = {
                onUpdateTask(task.copy(dueDate = rescheduleTo(task, LocalDate.now())))
                rescheduleFor = null
            },
            onTomorrow = {
                onUpdateTask(task.copy(dueDate = rescheduleTo(task, LocalDate.now().plusDays(1))))
                rescheduleFor = null
            },
            onNextMonday = {
                onUpdateTask(task.copy(dueDate = rescheduleTo(task, nextMondayFrom(LocalDate.now()))))
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

    detailFor?.let { task ->
        TaskDetailSheet(
            task = task,
            listName = detailListName(task),
            onDismiss = { detailFor = null },
            onUpdateTask = { updated -> onUpdateTask(updated) },
            onToggleComplete = { onToggleComplete(task) },
            tags = tags,
            onCreateTag = onCreateTag,
            occurrenceDate = selectedDate,
            parentTitle = if (task.isSubtask) {
                tasks.firstOrNull { it.id == task.parentId }?.title ?: "상위 할일"
            } else null,
            onUnnest = if (task.isSubtask) {
                {
                    onUnnest(task, selectedDate)
                    detailFor = null
                }
            } else null
        )
    }

    actionsFor?.let { task ->
        TaskActionSheet(
            task = task,
            onDismiss = { actionsFor = null },
            onUnnest = if (task.isSubtask) {
                {
                    onUnnest(task, selectedDate)
                    actionsFor = null
                }
            } else null,
            onAbandon = {
                onAbandon(task)
                actionsFor = null
            },
            onDelete = {
                if (task.repeatRule.isNullOrBlank()) onDelete(task)
                else recurringDeleteFor = task
                actionsFor = null
            }
        )
    }

    recurringDeleteFor?.let { task ->
        RecurringScopeDialog(
            title = "반복 할일 삭제",
            body = "반복 작업을 삭제하고 있습니다. 삭제 범위를 확인해주세요.",
            onDismiss = { recurringDeleteFor = null },
            onChoice = { scope ->
                when (scope) {
                    RecurringScope.ThisOccurrence -> onSkipOccurrence(task, selectedDate)
                    RecurringScope.AllIncomplete -> onDelete(task)
                }
                recurringDeleteFor = null
            }
        )
    }
}

@Composable
private fun DraggableTaskRow(
    task: Task,
    selectedDate: LocalDate,
    listName: String,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onLongPressActions: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit,
    onDragUpdate: (ActiveDrag?) -> Unit,
    onDragRelease: (Task) -> Unit,
    resolvedTags: List<Tag> = emptyList(),
    onToggleLeave: ((LocalDate, String) -> Unit)? = null,
    onStatusAction: ((com.bettertick.data.model.TaskStatusAction) -> Unit)? = null
) {
    // Track the item's position in the root Composition so drag offsets
    // can be translated to root-space for hit-testing against date cells
    // positioned elsewhere in the tree.
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var dragRootPointer by remember { mutableStateOf<Offset?>(null) }
    val latestDragUpdate by androidx.compose.runtime.rememberUpdatedState(onDragUpdate)
    val latestDragRelease by androidx.compose.runtime.rememberUpdatedState(onDragRelease)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> rootPos = coords.positionInRoot() }
            .pointerInput(task.id) {
                // Long-press on the checkbox (left 48dp) opens the status
                // menu instead of starting a drag.
                val checkboxZone = 48.dp.toPx()
                var ignored = false
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        ignored = localOffset.x < checkboxZone
                        if (ignored) return@detectDragGesturesAfterLongPress
                        val rootOffset = rootPos + localOffset
                        dragRootPointer = rootOffset
                        latestDragUpdate(ActiveDrag(task, rootOffset, rootOffset))
                    },
                    onDragEnd = {
                        if (ignored) return@detectDragGesturesAfterLongPress
                        latestDragRelease(task)
                        dragRootPointer = null
                    },
                    onDragCancel = {
                        if (ignored) return@detectDragGesturesAfterLongPress
                        latestDragUpdate(null)
                        dragRootPointer = null
                    },
                    onDrag = { change, amount ->
                        if (ignored) return@detectDragGesturesAfterLongPress
                        change.consume()
                        val next = (dragRootPointer ?: rootPos) + amount
                        dragRootPointer = next
                        latestDragUpdate(ActiveDrag(task, rootPos, next))
                    }
                )
            }
    ) {
        SwipeableTaskItem(
            task = task,
            onToggleComplete = onToggleComplete,
            onClick = onClick,
            onMove = onLongPressActions,
            onDelete = onDelete,
            onReschedule = onReschedule,
            overrideDate = selectedDate,
            listName = listName,
            resolvedTags = resolvedTags,
            rowBackground = DarkCard,
            onToggleLeave = onToggleLeave,
            showLeaveChips = false,
            onStatusAction = onStatusAction
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskActionSheet(
    task: Task,
    onDismiss: () -> Unit,
    onAbandon: () -> Unit,
    onDelete: () -> Unit,
    /** Subtasks only: 상위 할일에서 분리. */
    onUnnest: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionTile(Icons.Outlined.PushPin, "상단 고정", MaterialTheme.colorScheme.primary) { onDismiss() }
            ActionTile(Icons.Outlined.Share, "공유", Color(0xFF22C9A0)) { onDismiss() }
            if (onUnnest != null) {
                ActionTile(Icons.Outlined.LinkOff, "분리", Color(0xFFF2994A)) { onUnnest() }
            }
            ActionTile(Icons.Outlined.Close, "계획 취소", AbandonedBlue) { onAbandon() }
            ActionTile(Icons.Outlined.Delete, "삭제", OverdueRed) { onDelete() }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TextPrimary.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/**
 * Upcoming Monday after [from]. If today is Monday, jumps a full 7 days so
 * "다음 월요일" always points to the next-week's Monday instead of today.
 */
private fun nextMondayFrom(from: java.time.LocalDate): java.time.LocalDate {
    val daysAhead = ((java.time.DayOfWeek.MONDAY.value - from.dayOfWeek.value + 7) % 7)
        .let { if (it == 0) 7 else it }
    return from.plusDays(daysAhead.toLong())
}

/**
 * Build a new `dueDate` Timestamp pinned to [date] while preserving the
 * task's existing time-of-day (falls back to 00:00 when the task was all-
 * day). This matches what TaskDatePickerSheet's confirm path produces.
 */
private fun rescheduleTo(
    task: com.bettertick.data.model.Task,
    date: java.time.LocalDate
): com.google.firebase.Timestamp {
    val existingLocal = task.dueDate?.toDate()?.toInstant()
        ?.atZone(java.time.ZoneId.systemDefault())
    val localTime = existingLocal?.toLocalTime() ?: java.time.LocalTime.MIDNIGHT
    val instant = date.atTime(localTime)
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
    return com.google.firebase.Timestamp(java.util.Date.from(instant))
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "\uD83D\uDCC5",
            fontSize = 56.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "이 날에는 일정이 없어요",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "오른쪽 아래 + 버튼으로 일정을 추가하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )
    }
}

/** Thin line down the left of a parent's subtask rows. */
private val SubtaskGuide = Color(0x667F7F7F)

/** Insertion marker shown between rows while dragging to reorder. */
@Composable
private fun ReorderLine() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.primary)
    )
}
