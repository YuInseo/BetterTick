package com.bettertick.ui.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.bettertick.data.model.TaskList
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.TextSecondary

/**
 * "Move to list" picker sheet. Shown when the user taps the blue folder
 * action on a swipeable task row. Mirrors the reference: a searchable
 * flat list of every [TaskList] with a pinned "기본함" (no-list) entry on
 * top, the current list checkmarked, and an "Add list" footer.
 */
@Composable
fun MoveTaskListSheet(
    lists: List<TaskList>,
    currentListId: String,
    onDismiss: () -> Unit,
    onPick: (listId: String) -> Unit,
    onAddList: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(lists, query) {
        if (query.isBlank()) lists
        else lists.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            // Header: X + "이동"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "닫기",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onDismiss() }
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "이동",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (query.isEmpty()) {
                    Text("검색", color = TextSecondary, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            // Scrollable list panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .verticalScroll(rememberScrollState())
            ) {
                // 기본함 — synthetic entry for tasks with no list assigned.
                val inboxSelected = currentListId.isBlank() ||
                    filtered.none { it.id == currentListId }
                if (query.isBlank() || "기본함".contains(query.trim(), ignoreCase = true)) {
                    ListPickerRow(
                        label = "기본함",
                        icon = { InboxIcon() },
                        selected = inboxSelected,
                        onClick = {
                            onPick("")
                            onDismiss()
                        }
                    )
                }

                filtered.forEach { list ->
                    ListPickerRow(
                        label = list.name,
                        icon = { ListIcon(list) },
                        selected = list.id == currentListId,
                        onClick = {
                            onPick(list.id)
                            onDismiss()
                        }
                    )
                }

                // 목록 추가 footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddList()
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = PickerAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "목록 추가",
                        color = PickerAccent,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ListPickerRow(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = if (selected) PickerAccent else Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PickerAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun InboxIcon() {
    Icon(
        imageVector = Icons.Outlined.Inbox,
        contentDescription = null,
        tint = PickerAccent,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
private fun ListIcon(list: TaskList) {
    // Use the icon string as an emoji when it looks like one, otherwise
    // fall back to a neutral folder-style glyph. Keeps the sheet readable
    // even for lists created before the emoji picker existed.
    val glyph = list.icon.takeIf { it.isNotBlank() && it != "folder" } ?: "≡"
    Text(
        text = glyph,
        fontSize = 16.sp,
        color = Color.White
    )
}

private val PickerAccent = Color(0xFF4A90E2)
