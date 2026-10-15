package com.bettertick.ui.screens.tasks

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.ui.screens.tasks.components.TaskItem
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DividerColor
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextPrimary
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import com.bettertick.ui.theme.priorityColor

/** 목록 ⋮ — TickTick list menu. */
@Composable
fun TaskListMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onGroupSort: () -> Unit,
    onSelect: () -> Unit,
    onSectionSettings: () -> Unit,
    onShare: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(DarkSurface)
    ) {
        MenuRow(
            if (TaskListPrefs.showDetails) Icons.Outlined.VisibilityOff else Icons.Outlined.Notes,
            if (TaskListPrefs.showDetails) "세부 정보 숨기기" else "세부 정보 표시"
        ) { TaskListPrefs.updateShowDetails(!TaskListPrefs.showDetails); onDismiss() }
        MenuRow(
            if (TaskListPrefs.showCompleted) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            if (TaskListPrefs.showCompleted) "완료 항목 숨기기" else "완료 항목 표시"
        ) { TaskListPrefs.updateShowCompleted(!TaskListPrefs.showCompleted); onDismiss() }
        HorizontalDivider(color = DividerColor)
        MenuRow(Icons.AutoMirrored.Outlined.Sort, "그룹 및 정렬") { onDismiss(); onGroupSort() }
        if (TaskListPrefs.groupBy == TaskGroupBy.DATE) {
            MenuRow(Icons.Outlined.Tune, "섹션 설정") { onDismiss(); onSectionSettings() }
        }
        MenuRow(Icons.Outlined.Checklist, "선택") { onDismiss(); onSelect() }
        MenuRow(Icons.Outlined.Share, "공유") { onDismiss(); onShare() }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = TextPrimary, fontSize = 15.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
        onClick = onClick
    )
}

/** 그룹 및 정렬 bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSortSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "그룹 및 정렬", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            SheetSectionTitle("그룹")
            TaskGroupBy.entries.forEach { g ->
                RadioRow(g.label, g == TaskListPrefs.groupBy) { TaskListPrefs.updateGroupBy(g) }
            }
            SheetSectionTitle("정렬")
            TaskSortBy.entries.forEach { s ->
                RadioRow(s.label, s == TaskListPrefs.sortBy) { TaskListPrefs.updateSortBy(s) }
            }
        }
    }
}

@Composable
private fun SheetSectionTitle(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextTertiary,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, fontSize = 15.sp,
            color = if (selected) ThemeState.accentColor else TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Default.Check, null, tint = ThemeState.accentColor, modifier = Modifier.size(20.dp))
    }
}

/** Row used in 선택 mode: round selection mark + the regular task row. */
@Composable
fun SelectableTaskRow(
    task: Task,
    selected: Boolean,
    listName: String,
    tags: List<Tag>,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) ThemeState.accentColor.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onToggle() }
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(ThemeState.accentColor)
                    else Modifier.border(1.5.dp, TextTertiary, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Box(Modifier.weight(1f)) {
            TaskItem(
                task = task,
                onToggleComplete = onToggle,
                onClick = onToggle,
                listName = listName,
                resolvedTags = tags
            )
        }
    }
}

/** Bottom bar shown in 선택 mode. */
@Composable
fun BatchActionBar(
    enabled: Boolean,
    onComplete: () -> Unit,
    onDate: () -> Unit,
    onMove: () -> Unit,
    onPriority: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var priorityMenu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
        HorizontalDivider(color = DividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatchButton(Icons.Outlined.TaskAlt, "완료", enabled, onComplete)
            BatchButton(Icons.Outlined.CalendarToday, "날짜", enabled, onDate)
            BatchButton(Icons.AutoMirrored.Outlined.DriveFileMove, "이동", enabled, onMove)
            Box {
                BatchButton(Icons.Outlined.Flag, "우선순위", enabled, onClick = { priorityMenu = true })
                DropdownMenu(
                    expanded = priorityMenu,
                    onDismissRequest = { priorityMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    listOf(3 to "높은 우선순위", 2 to "중간 우선순위", 1 to "낮은 우선순위", 0 to "우선순위 없음").forEach { (lvl, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = TextPrimary) },
                            leadingIcon = { Icon(if (lvl > 0) Icons.Filled.Flag else Icons.Outlined.Flag, null, tint = priorityColor(lvl)) },
                            onClick = { priorityMenu = false; onPriority(lvl) }
                        )
                    }
                }
            }
            BatchButton(Icons.Outlined.Delete, "삭제", enabled, onDelete, tint = OverdueRed)
        }
    }
}

@Composable
private fun BatchButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = TextSecondary
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val c = if (enabled) tint else TextTertiary
        Icon(icon, contentDescription = label, tint = c, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = c)
    }
}

/** Empty state shown when a list has nothing to show. */
@Composable
fun EmptyTaskList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(ThemeState.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = ThemeState.accentColor, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("할일이 없습니다", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("+ 버튼을 눌러 할일을 추가하세요", fontSize = 13.sp, color = TextTertiary)
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
