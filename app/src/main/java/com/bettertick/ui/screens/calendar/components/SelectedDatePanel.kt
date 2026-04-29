package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
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
import com.bettertick.ui.theme.Orange
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
    tags: List<Tag> = emptyList(),
    onCreateTag: suspend (String) -> String = { "" }
) {
    val listPadding = remember { PaddingValues(vertical = 10.dp) }
    val shape = remember { RoundedCornerShape(18.dp) }

    val active = tasks.filterNot { it.isCompleted || it.isAbandoned }
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = listPadding
                ) {
                    items(active, key = { it.id }) { task ->
                        val taskTags = remember(task.tagIds, tags) {
                            task.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                        }
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
                            onDragUpdate = onDragTaskUpdate,
                            onDragRelease = onDragTaskRelease,
                            resolvedTags = taskTags
                        )
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
                        resolvedTags = taskTags
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
            onCreateTag = onCreateTag
        )
    }

    actionsFor?.let { task ->
        TaskActionSheet(
            task = task,
            onDismiss = { actionsFor = null },
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
    resolvedTags: List<Tag> = emptyList()
) {
    // Track the item's position in the root Composition so drag offsets
    // can be translated to root-space for hit-testing against date cells
    // positioned elsewhere in the tree.
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var dragRootPointer by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> rootPos = coords.positionInRoot() }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        val rootOffset = rootPos + localOffset
                        dragRootPointer = rootOffset
                        onDragUpdate(ActiveDrag(task, rootOffset, rootOffset))
                    },
                    onDragEnd = {
                        onDragRelease(task)
                        dragRootPointer = null
                    },
                    onDragCancel = {
                        onDragUpdate(null)
                        dragRootPointer = null
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val next = (dragRootPointer ?: rootPos) + amount
                        dragRootPointer = next
                        onDragUpdate(ActiveDrag(task, rootPos, next))
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
            resolvedTags = resolvedTags
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskActionSheet(
    task: Task,
    onDismiss: () -> Unit,
    onAbandon: () -> Unit,
    onDelete: () -> Unit
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
            ActionTile(Icons.Outlined.PushPin, "상단 고정", Orange) { onDismiss() }
            ActionTile(Icons.Outlined.Share, "공유", Color(0xFF22C9A0)) { onDismiss() }
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
                .background(Color.White.copy(alpha = 0.04f)),
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
            text = "편하게 해요",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )
    }
}
