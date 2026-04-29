package com.bettertick.ui.screens.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.QuadrantConfig
import com.bettertick.data.model.Task
import com.bettertick.ui.screens.tasks.components.TaskDetailSheet
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Drag state for a task picked up in the matrix. [sourceQuadrantId] is kept
 * so the release handler can call into the VM with the right "from" quadrant
 * (otherwise we'd lose track once the pointer leaves the source card).
 */
private data class MatrixDrag(
    val task: Task,
    val sourceQuadrantId: String,
    val currentOffset: Offset
)

/**
 * Eisenhower-matrix view. 2×2 quadrant cards, each rendering the tasks
 * matching its filter spec. Long-press on a task picks it up and drops it
 * into another quadrant — the VM patches priority/listId/tags on release so
 * the task now matches the destination filter.
 */
@Composable
fun MatrixScreen(
    onEdit: () -> Unit,
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val lists by viewModel.lists.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var detailFor by remember { mutableStateOf<Task?>(null) }

    val onToggleComplete: (Task) -> Unit = { task ->
        viewModel.toggleComplete(task.id, !task.isCompleted)
    }
    val onOpenTask: (Task) -> Unit = { task -> detailFor = task }

    // Drag + drop-target tracking. Quadrant bounds are reported in root
    // coordinates so the release hit-test compares against the same space
    // the drag pointer is in.
    var activeDrag by remember { mutableStateOf<MatrixDrag?>(null) }
    val quadrantBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootContainerPos by remember { mutableStateOf(Offset.Zero) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    val hoveredQuadrantId: String? = activeDrag?.let { drag ->
        quadrantBounds.entries
            .firstOrNull { (_, r) -> r.contains(drag.currentOffset) }
            ?.key
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootContainerPos = it.positionInRoot() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "아이젠하워 매트릭스",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "더 보기",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("편집", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White)
                            },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (config.hideCompleted) "완료된 할일 보이기" else "완료된 할일 숨기기",
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                menuOpen = false
                                viewModel.toggleHideCompleted()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (config.todayOnly) "모든 날짜 보기" else "오늘 날짜만 보기",
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Today,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                menuOpen = false
                                viewModel.toggleTodayOnly()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("사용자 가이드", color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.HelpOutline,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = { menuOpen = false /* stub — no guide yet */ }
                        )
                    }
                }
            }

            val quadrants = config.quadrants
            val row1 = quadrants.take(2)
            val row2 = quadrants.drop(2).take(2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row1.forEach { q ->
                        QuadrantCard(
                            quadrant = q,
                            tasks = filterTasks(tasks, q, config.hideCompleted, config.todayOnly),
                            isDragHover = hoveredQuadrantId == q.id &&
                                activeDrag?.sourceQuadrantId != q.id,
                            onBoundsChanged = { rect -> quadrantBounds[q.id] = rect },
                            onTaskClick = onOpenTask,
                            onToggleComplete = onToggleComplete,
                            onDragStart = { task, pointer ->
                                activeDrag = MatrixDrag(task, q.id, pointer)
                            },
                            onDragUpdate = { pointer ->
                                activeDrag = activeDrag?.copy(currentOffset = pointer)
                            },
                            onDragEnd = {
                                val drag = activeDrag
                                android.util.Log.d(
                                    "MatrixDrag",
                                    "onDragEnd drag=$drag boundsKeys=${quadrantBounds.keys}"
                                )
                                if (drag == null) return@QuadrantCard
                                val destId = quadrantBounds.entries
                                    .firstOrNull { (_, r) -> r.contains(drag.currentOffset) }?.key
                                android.util.Log.d(
                                    "MatrixDrag",
                                    "onDragEnd offset=${drag.currentOffset} destId=$destId src=${drag.sourceQuadrantId}"
                                )
                                val dest = quadrants.firstOrNull { it.id == destId }
                                val src = quadrants.firstOrNull { it.id == drag.sourceQuadrantId }
                                if (dest != null && src != null && dest.id != src.id) {
                                    viewModel.moveTaskToQuadrant(drag.task, src, dest)
                                }
                                activeDrag = null
                            },
                            onDragCancel = {
                                android.util.Log.d("MatrixDrag", "onDragCancel")
                                activeDrag = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2.forEach { q ->
                        QuadrantCard(
                            quadrant = q,
                            tasks = filterTasks(tasks, q, config.hideCompleted, config.todayOnly),
                            isDragHover = hoveredQuadrantId == q.id &&
                                activeDrag?.sourceQuadrantId != q.id,
                            onBoundsChanged = { rect -> quadrantBounds[q.id] = rect },
                            onTaskClick = onOpenTask,
                            onToggleComplete = onToggleComplete,
                            onDragStart = { task, pointer ->
                                activeDrag = MatrixDrag(task, q.id, pointer)
                            },
                            onDragUpdate = { pointer ->
                                activeDrag = activeDrag?.copy(currentOffset = pointer)
                            },
                            onDragEnd = {
                                val drag = activeDrag
                                android.util.Log.d(
                                    "MatrixDrag",
                                    "onDragEnd drag=$drag boundsKeys=${quadrantBounds.keys}"
                                )
                                if (drag == null) return@QuadrantCard
                                val destId = quadrantBounds.entries
                                    .firstOrNull { (_, r) -> r.contains(drag.currentOffset) }?.key
                                android.util.Log.d(
                                    "MatrixDrag",
                                    "onDragEnd offset=${drag.currentOffset} destId=$destId src=${drag.sourceQuadrantId}"
                                )
                                val dest = quadrants.firstOrNull { it.id == destId }
                                val src = quadrants.firstOrNull { it.id == drag.sourceQuadrantId }
                                if (dest != null && src != null && dest.id != src.id) {
                                    viewModel.moveTaskToQuadrant(drag.task, src, dest)
                                }
                                activeDrag = null
                            },
                            onDragCancel = {
                                android.util.Log.d("MatrixDrag", "onDragCancel")
                                activeDrag = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }

        // Floating drag preview — root-space positioned, centered on the
        // finger.
        activeDrag?.let { drag ->
            Box(
                modifier = Modifier
                    .onGloballyPositioned { previewSize = it.size }
                    .offset {
                        IntOffset(
                            (drag.currentOffset.x - rootContainerPos.x).roundToInt() -
                                previewSize.width / 2,
                            (drag.currentOffset.y - rootContainerPos.y).roundToInt() -
                                previewSize.height / 2
                        )
                    }
                    .background(DarkCard.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = drag.task.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

    // Detail sheet — opened from a task row tap. Shares the TaskDetailSheet
    // used in Kanban + Calendar views so editing behaves identically.
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
private fun QuadrantCard(
    quadrant: QuadrantConfig,
    tasks: List<Task>,
    isDragHover: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onDragStart: (Task, Offset) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(quadrant.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard)
            // Report root-space bounds so MatrixScreen can hit-test a drop
            // against the same coordinate system the drag pointer reports.
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onBoundsChanged(
                    Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + coords.size.width,
                        bottom = pos.y + coords.size.height
                    )
                )
            }
            .then(
                if (isDragHover) Modifier.border(2.dp, accent, RoundedCornerShape(18.dp))
                else Modifier
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RomanBadge(roman = quadrant.id, color = accent)
            Spacer(Modifier.width(8.dp))
            Text(
                text = quadrant.nameEn.ifEmpty { quadrant.nameKo },
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (tasks.isEmpty()) {
                Text(
                    text = "비어있음",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                tasks.forEach { task ->
                    DraggableTaskRow(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onToggleComplete = { onToggleComplete(task) },
                        onDragStart = { pointer -> onDragStart(task, pointer) },
                        onDragUpdate = onDragUpdate,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableTaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    // Translate local pointer offsets to root-space so parent hit-testing
    // works — same pattern SelectedDatePanel.DraggableTaskRow uses.
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var dragRootPointer by remember { mutableStateOf<Offset?>(null) }

    // When completed, the whole row dims to signal "done". Alpha applies to
    // the parent Row so the checkbox, title, and date all fade together.
    val rowAlpha = if (task.isCompleted) 0.45f else 1f

    // The outer Row owns ONLY the drag detector — putting .clickable on the
    // same node competes with the long-press for the down event, which made
    // the drag fire `onDragCancel` (instead of `onDragEnd`) on release and
    // the dropped task vanished without committing the move. Click handling
    // lives on the inner title Column instead, mirroring the calendar's
    // SwipeableTaskItem pattern.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .alpha(rowAlpha)
            .onGloballyPositioned { coords -> rootPos = coords.positionInRoot() }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        val rootOffset = rootPos + localOffset
                        dragRootPointer = rootOffset
                        onDragStart(rootOffset)
                    },
                    onDragEnd = {
                        onDragEnd()
                        dragRootPointer = null
                    },
                    onDragCancel = {
                        onDragCancel()
                        dragRootPointer = null
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val next = (dragRootPointer ?: rootPos) + amount
                        dragRootPointer = next
                        onDragUpdate(next)
                    }
                )
            }
    ) {
        val checkboxShape = RoundedCornerShape(4.dp)
        // Checkbox has its own clickable so tapping it toggles completion
        // without also triggering the row's onClick (opens task detail).
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(checkboxShape)
                .then(
                    if (task.isCompleted) Modifier.background(TextSecondary)
                    else Modifier.border(1.5.dp, TextSecondary, checkboxShape)
                )
                .clickable { onToggleComplete() },
            contentAlignment = Alignment.Center
        ) {
            if (task.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
        ) {
            Text(
                text = task.title,
                color = if (task.isCompleted) TextTertiary else Color.White,
                fontSize = 13.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2
            )
            task.dueDate?.let { ts ->
                val label = formatTaskDueLabel(ts)
                if (label.isNotBlank()) {
                    Text(
                        text = label,
                        color = TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun RomanBadge(roman: String, color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = roman,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTaskDueLabel(ts: Timestamp): String {
    val zdt = ts.toDate().toInstant().atZone(ZoneId.systemDefault())
    val date = zdt.toLocalDate()
    val today = LocalDate.now()
    val day = when (date) {
        today -> "오늘"
        today.plusDays(1) -> "내일"
        today.minusDays(1) -> "어제"
        else -> "${date.monthValue}월 ${date.dayOfMonth}일"
    }
    return day
}
