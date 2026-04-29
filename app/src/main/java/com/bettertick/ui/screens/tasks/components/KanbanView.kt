package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary

private val KanbanAccent = Color(0xFF4A90E2)

/** Kanban view for a list. Top row is the column chip strip (미분류 + user
 *  columns + "+"), which filters the card list below. Tapping "+" opens an
 *  inline dialog that appends a new column name onto the list. */
@Composable
fun KanbanView(
    list: TaskList,
    tasks: List<Task>,
    listNameById: (String) -> String,
    selectedColumn: String,
    onColumnSelected: (String) -> Unit,
    onAddColumn: (String) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit,
    modifier: Modifier = Modifier,
    tags: List<Tag> = emptyList(),
    onCreateTag: suspend (String) -> String = { "" }
) {
    var showNewColumnDialog by remember { mutableStateOf(false) }
    var detailFor by remember { mutableStateOf<Task?>(null) }

    val activeTasks = tasks.filter { !it.isCompleted && !it.isAbandoned }
    val columnNames = list.kanbanColumns
    val filtered = activeTasks.filter { it.kanbanColumn == selectedColumn }

    Column(modifier = modifier.fillMaxSize()) {
        // Column chip strip. "미분류" stays first so tasks without an assigned
        // column are always reachable; user columns come after; "+" at the end.
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                ColumnChip(
                    label = "미분류",
                    selected = selectedColumn == "",
                    onClick = { onColumnSelected("") }
                )
            }
            items(columnNames) { name ->
                ColumnChip(
                    label = name,
                    selected = selectedColumn == name,
                    onClick = { onColumnSelected(name) }
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { showNewColumnDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "새 열",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Cards. Uses the reusable TaskItem so interaction (checkbox, click)
        // stays in lockstep with the list view's rows.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.id }) { task ->
                val taskTags = remember(task.tagIds, tags) {
                    task.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id } }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                ) {
                    TaskItem(
                        task = task,
                        onToggleComplete = { onToggleComplete(task) },
                        onClick = { detailFor = task },
                        resolvedTags = taskTags
                    )
                }
            }
        }
    }

    detailFor?.let { task ->
        TaskDetailSheet(
            task = task,
            listName = listNameById(task.listId),
            onDismiss = { detailFor = null },
            onUpdateTask = { updated ->
                onUpdateTask(updated)
                detailFor = null
            },
            onToggleComplete = {
                onToggleComplete(task)
                detailFor = null
            },
            tags = tags,
            onCreateTag = onCreateTag
        )
    }

    if (showNewColumnDialog) {
        NewColumnDialog(
            existing = columnNames,
            onDismiss = { showNewColumnDialog = false },
            onConfirm = { name ->
                onAddColumn(name)
                onColumnSelected(name)
                showNewColumnDialog = false
            }
        )
    }
}

@Composable
private fun ColumnChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (selected) Modifier.background(KanbanAccent.copy(alpha = 0.2f))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) KanbanAccent else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun NewColumnDialog(
    existing: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val trimmed = name.trim()
    val duplicate = existing.any { it.equals(trimmed, ignoreCase = true) }
    val canSave = trimmed.isNotBlank() && !duplicate

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "새로운 열",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = "열 이름",
                        color = TextTertiary,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (duplicate) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "같은 이름의 열이 이미 있어요.",
                    color = Color(0xFFFF5D5D),
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "취소",
                    color = KanbanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "확인",
                    color = if (canSave) KanbanAccent else TextTertiary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .then(
                            if (canSave) Modifier.clickable { onConfirm(trimmed) }
                            else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
