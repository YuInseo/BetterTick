package com.bettertick.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.TabBarConfig
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.DarkSurfaceVariant
import com.bettertick.ui.theme.OverdueRed
import com.bettertick.ui.theme.TextSecondary

data class TabItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector
)

/** Single source of truth for every user-toggleable tab. The navigation host
 *  maps these ids to real routes — ids without a matching route are allowed
 *  in the picker but simply skipped when rendering the bottom bar, so adding
 *  a placeholder tab here doesn't require a route change. */
fun tabCatalog(): List<TabItem> = listOf(
    TabItem("tasks", "과제", "리스트와 필터로 작업을 관리하세요.", Icons.Outlined.CheckBox),
    TabItem("calendar", "달력", "5가지 캘린더 뷰로 작업을 관리하세요.", Icons.Outlined.CalendarMonth),
    TabItem("eisenhower", "아이젠하워 매트릭스", "중요하고 긴급한 일에 집중하세요.", Icons.Outlined.GridView),
    TabItem("pomodoro", "포모도로", "포모 타이머나 스톱워치를 사용하여 집중력을 유지하세요.", Icons.Outlined.RadioButtonUnchecked),
    TabItem("more", "설정", "현재 설정을 변경하고 확인하세요.", Icons.Outlined.MoreHoriz),
    TabItem("habits", "습관", "습관을 기르고 그것을 추적하십시오.", Icons.Outlined.Schedule),
    TabItem("diary", "일기", "매일의 생각과 감정을 기록하세요.", Icons.Outlined.Book),
    TabItem("dday", "디데이", "특별한 날을 기억하세요.", Icons.Outlined.Star),
    TabItem("search", "검색", "빠르게 작업을 검색하세요.", Icons.Outlined.Search),
    TabItem("location", "동선", "오늘 어디에 있었는지 기록하세요.", Icons.Outlined.LocationOn)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabBarScreen(
    onBack: () -> Unit,
    viewModel: TabBarViewModel = hiltViewModel()
) {
    val catalog = remember { tabCatalog() }

    val persisted by viewModel.config.collectAsState()

    val pickerCatalog = remember(catalog) { catalog.filter { it.id != "more" } }

    val enabledTabs = remember { mutableStateListOf<TabItem>() }
    var maxTabs by remember { mutableIntStateOf(persisted.maxTabs) }
    LaunchedEffect(persisted) {
        enabledTabs.clear()
        persisted.enabledIds
            .filter { it != "more" }
            .mapNotNull { id -> pickerCatalog.firstOrNull { it.id == id } }
            .forEach { enabledTabs.add(it) }
        maxTabs = persisted.maxTabs
    }

    fun commit() {
        viewModel.saveConfig(
            TabBarConfig(
                enabledIds = enabledTabs.map { it.id },
                maxTabs = maxTabs
            )
        )
    }

    val unusedTabs = pickerCatalog.filter { tab ->
        enabledTabs.none { it.id == tab.id }
    }
    var showMaxTabsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "탭 바",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReorderableEnabledList(
                enabledTabs = enabledTabs,
                onToggle = { tab ->
                    if (enabledTabs.size > 2) {
                        enabledTabs.remove(tab)
                        commit()
                    }
                },
                onReorderCommit = { commit() }
            )

            if (unusedTabs.isNotEmpty()) {
                Text(
                    text = "사용 안 함",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
            ) {
                unusedTabs.forEach { tab ->
                    TabListItem(
                        tab = tab,
                        isEnabled = false,
                        onToggle = {
                            enabledTabs.add(tab)
                            commit()
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .clickable { showMaxTabsDialog = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "탭의 최대 개수",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = maxTabs.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "최대한도를 초과한 탭은 '더보기'에서 표시됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
            )
        }

        TabBarPreview(
            enabled = enabledTabs,
            maxTabs = maxTabs
        )
    }

    if (showMaxTabsDialog) {
        AlertDialog(
            onDismissRequest = { showMaxTabsDialog = false },
            title = {
                Text(
                    "탭의 최대 개수",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf(3, 4, 5).forEach { count ->
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (count == maxTabs) FontWeight.Bold else FontWeight.Normal,
                            color = if (count == maxTabs) MaterialTheme.colorScheme.onBackground else TextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    maxTabs = count
                                    showMaxTabsDialog = false
                                    commit()
                                }
                                .padding(vertical = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMaxTabsDialog = false }) {
                    Text("확인", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMaxTabsDialog = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
private fun TabBarPreview(
    enabled: List<TabItem>,
    maxTabs: Int
) {
    val userCap = (maxTabs - 1).coerceAtLeast(0)
    val visible = enabled.take(userCap)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        visible.forEach { tab ->
            PreviewTile(icon = tab.icon)
        }
        PreviewTile(icon = Icons.Outlined.MoreHoriz)
    }
}

@Composable
private fun ReorderableEnabledList(
    enabledTabs: androidx.compose.runtime.snapshots.SnapshotStateList<TabItem>,
    onToggle: (TabItem) -> Unit,
    onReorderCommit: () -> Unit
) {
    var itemHeightPx by remember { mutableStateOf(0f) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
    ) {
        enabledTabs.forEach { tab ->
            key(tab.id) {
            val isDragging = draggingId == tab.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        if (itemHeightPx == 0f) itemHeightPx = coords.size.height.toFloat()
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                    }
                    .background(
                        if (isDragging) DarkSurfaceVariant else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Remove",
                    tint = OverdueRed,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onToggle(tab) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tab.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = tab.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = "Reorder",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInput(tab.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = tab.id
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragOffsetY += drag.y
                                    val h = itemHeightPx
                                    if (h <= 0f) return@detectDragGesturesAfterLongPress
                                    while (true) {
                                        val idx = enabledTabs.indexOfFirst { it.id == tab.id }
                                        if (idx < 0) break
                                        when {
                                            dragOffsetY > h / 2 && idx + 1 < enabledTabs.size -> {
                                                val item = enabledTabs.removeAt(idx)
                                                enabledTabs.add(idx + 1, item)
                                                dragOffsetY -= h
                                            }
                                            dragOffsetY < -h / 2 && idx > 0 -> {
                                                val item = enabledTabs.removeAt(idx)
                                                enabledTabs.add(idx - 1, item)
                                                dragOffsetY += h
                                            }
                                            else -> return@detectDragGesturesAfterLongPress
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffsetY = 0f
                                    onReorderCommit()
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffsetY = 0f
                                }
                            )
                        }
                )
            }
            }
        }
    }
}

@Composable
private fun PreviewTile(icon: ImageVector) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun TabListItem(
    tab: TabItem,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Outlined.RadioButtonUnchecked else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (isEnabled) "Remove" else "Add",
            tint = if (isEnabled) OverdueRed else Color(0xFF2ECC71),
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable { onToggle() }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = tab.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = "Reorder",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
