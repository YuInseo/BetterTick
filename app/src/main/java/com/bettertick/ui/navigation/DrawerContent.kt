package com.bettertick.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.bettertick.data.model.Tag
import com.bettertick.data.model.TaskList
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerContent(
    userName: String,
    userPhotoUrl: String?,
    todayCount: Int,
    inboxCount: Int,
    lists: List<TaskList>,
    tags: List<Tag>,
    taskCountByList: Map<String, Int>,
    taskCountByTag: Map<String, Int>,
    selectedFilter: String,
    onTodayClick: () -> Unit,
    onInboxClick: () -> Unit,
    onListClick: (TaskList) -> Unit,
    onTagClick: (Tag) -> Unit = {},
    onAddListClick: () -> Unit,
    onEditListClick: (TaskList) -> Unit = {},
    onTogglePin: (TaskList) -> Unit = {},
    onDeleteList: (TaskList) -> Unit = {},
    onAddFilterClick: () -> Unit = {},
    onAddTagClick: () -> Unit = {},
    onEditTagsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Popup anchored to the "추가" row. 목록 wires into the real add-list
    // flow; 필터 / 태그 are placeholders kept here so the menu shape matches
    // the reference even before those features ship.
    var addMenuOpen by remember { mutableStateOf(false) }
    // Long-press popup state for per-list actions. Stored by list id so the
    // DropdownMenu only opens on the row the user pressed.
    var openMenuForListId by remember { mutableStateOf<String?>(null) }
    // Tag section state — header long-press popup + expand/collapse toggle.
    var tagMenuOpen by remember { mutableStateOf(false) }
    var tagsExpanded by remember { mutableStateOf(true) }
    val (pinnedLists, unpinnedLists) = lists.partition { it.isPinned }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(DarkBackground)
            .padding(top = 48.dp)
    ) {
        // User header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!userPhotoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Orange)
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Orange)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = userName.ifEmpty { "User" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pinned-list tiles — horizontal row of icon chips above every
        // regular section, matching the reference "상단 고정" behaviour.
        if (pinnedLists.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pinnedLists) { list ->
                    PinnedListTile(
                        list = list,
                        isSelected = selectedFilter == list.id,
                        onClick = { onListClick(list) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Smart filters
        DrawerItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Orange, modifier = Modifier.size(24.dp)) },
            label = "오늘",
            count = todayCount,
            isSelected = selectedFilter == "today",
            onClick = onTodayClick
        )
        DrawerItem(
            icon = { Icon(Icons.Default.Inbox, contentDescription = null, tint = Orange, modifier = Modifier.size(24.dp)) },
            label = "기본함",
            count = inboxCount,
            isSelected = selectedFilter == "inbox",
            onClick = onInboxClick
        )

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = DarkSurface, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Tag section header — long-press opens a 편집 popup that jumps to
        // the tag management screen. Tapping the row toggles the children.
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (tagMenuOpen) Modifier.background(DarkSurface) else Modifier)
                    .combinedClickable(
                        onClick = { tagsExpanded = !tagsExpanded },
                        onLongClick = { tagMenuOpen = true }
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Label,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "태그",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (tagsExpanded) Icons.Default.KeyboardArrowDown
                        else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(
                expanded = tagMenuOpen,
                onDismissRequest = { tagMenuOpen = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("편집", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        tagMenuOpen = false
                        onEditTagsClick()
                    }
                )
            }
        }

        // Tag children — indented rows; tap filters the task list by that tag.
        if (tagsExpanded) {
            tags.forEach { tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagClick(tag) }
                        .padding(start = 40.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = runCatching {
                            Color(android.graphics.Color.parseColor(tag.color))
                        }.getOrDefault(Orange),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    val count = taskCountByTag[tag.id]
                    if (count != null && count > 0) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = DarkSurface, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Custom lists — long-press opens the per-list action menu (편집 /
        // 상단 고정 / 삭제). Pinned lists appear as the tile row at top
        // instead of here.
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(unpinnedLists) { list ->
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (selectedFilter == list.id) Modifier.background(DarkSurface)
                                else Modifier
                            )
                            .combinedClickable(
                                onClick = { onListClick(list) },
                                onLongClick = { openMenuForListId = list.id }
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = Orange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        val count = taskCountByList[list.id]
                        if (count != null && count > 0) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ">",
                            color = TextSecondary,
                            fontWeight = FontWeight.Light
                        )
                    }
                    DropdownMenu(
                        expanded = openMenuForListId == list.id,
                        onDismissRequest = { openMenuForListId = null },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("목록 추가", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            },
                            onClick = {
                                openMenuForListId = null
                                onAddListClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("편집", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                            },
                            onClick = {
                                openMenuForListId = null
                                onEditListClick(list)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (list.isPinned) "고정 해제" else "상단 고정",
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (list.isPinned) Icons.Default.PushPin else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                openMenuForListId = null
                                onTogglePin(list)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", color = Color(0xFFFF5D5D)) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5D5D))
                            },
                            onClick = {
                                openMenuForListId = null
                                onDeleteList(list)
                            }
                        )
                    }
                }
            }
        }

        // Add menu anchor — "추가" row with a floating popup that lets the
        // user pick what to create (목록 / 필터 / 태그).
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { addMenuOpen = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = TextSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("추가", color = TextSecondary, fontSize = 16.sp)
            }
            DropdownMenu(
                expanded = addMenuOpen,
                onDismissRequest = { addMenuOpen = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("목록", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Reorder, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        addMenuOpen = false
                        onAddListClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("필터", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        addMenuOpen = false
                        onAddFilterClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("태그", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        addMenuOpen = false
                        onAddTagClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun PinnedListTile(
    list: TaskList,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) DarkSurface else DarkSurfaceVariant
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = list.name.take(6),
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun DrawerItem(
    icon: @Composable () -> Unit,
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.background(DarkSurface) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (count != null && count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = ">",
            color = TextSecondary,
            fontWeight = FontWeight.Light
        )
    }
}
