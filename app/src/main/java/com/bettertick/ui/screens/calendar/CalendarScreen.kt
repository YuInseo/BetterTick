package com.bettertick.ui.screens.calendar

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.ui.screens.calendar.components.ActiveDrag
import com.bettertick.ui.screens.calendar.components.ScrollableMonthCalendar
import com.bettertick.ui.screens.calendar.components.SelectedDatePanel
import com.bettertick.ui.screens.calendar.components.TaskDateLookup
import com.bettertick.ui.screens.calendar.components.WeekRow
import com.bettertick.ui.screens.calendar.components.WeekTimelineView
import com.bettertick.ui.screens.calendar.components.YearView
import com.bettertick.ui.screens.calendar.components.weekContaining
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CalendarViewMode(val label: String, val icon: ImageVector) {
    LIST("목록", Icons.AutoMirrored.Outlined.ViewList),
    YEAR("연도", Icons.Outlined.GridView),
    MONTH("월", Icons.Outlined.CalendarViewMonth),
    WEEK("주", Icons.Outlined.ViewWeek),
    THREE_DAY("3일", Icons.Outlined.ViewDay),
    DAY("일", Icons.Outlined.ViewAgenda)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onSelectedDateChanged: (java.time.LocalDate?) -> Unit = {}
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    androidx.compose.runtime.LaunchedEffect(selectedDate) {
        onSelectedDateChanged(selectedDate)
    }
    val tasksByDate by viewModel.tasksByDate.collectAsState()
    // Wrap the raw Map in an @Immutable type so Compose can mark children
    // skippable. The new wrapper equals by reference identity of the
    // underlying map, which is exactly the contract a fresh Firestore
    // snapshot honors (new map on change, same map otherwise).
    val lookup = remember(tasksByDate) { TaskDateLookup(tasksByDate) }

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val monthFormatter = DateTimeFormatter.ofPattern("M월", Locale.KOREAN)
    val initialMonth = remember { selectedMonth }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        TopAppBar(
            title = {
                if (viewMode == CalendarViewMode.YEAR) {
                    Text(
                        text = "${selectedMonth.year}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    Text(
                        text = "${selectedMonth.year}년 ${selectedMonth.format(monthFormatter)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                // Jump-to-today shortcut. Selecting today snaps the header
                // to the current month and reveals the per-day panel — the
                // fastest path back to "now" from any view.
                IconButton(onClick = {
                    val today = java.time.LocalDate.now()
                    viewModel.onVisibleMonthChanged(java.time.YearMonth.from(today))
                    if (viewMode == CalendarViewMode.YEAR) viewMode = CalendarViewMode.MONTH
                    if (selectedDate != today) viewModel.selectDate(today)
                }) {
                    Icon(
                        Icons.Outlined.Today,
                        contentDescription = "오늘로 이동",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // View mode toggle
                Box {
                    IconButton(onClick = { showViewModeMenu = true }) {
                        Icon(
                            Icons.Outlined.CalendarViewMonth,
                            contentDescription = "View mode",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { showViewModeMenu = false }
                    ) {
                        CalendarViewMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    viewMode = mode
                                    showViewModeMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        mode.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (mode == viewMode) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Orange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // More options
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("필터 보기 범위") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = {
                                Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("옵션 보기") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = {
                                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("할 일 배정하기") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = {
                                Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("공유") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground
            )
        )

        // Sticky day-of-week header (hidden in YEAR/WEEK modes — WEEK renders its own)
        if (viewMode != CalendarViewMode.YEAR && viewMode != CalendarViewMode.WEEK) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // When no date is selected: full scrollable month.
        // When a date IS selected: compact Column layout:
        //   [selected week strip]
        //   [task card — fills the middle, rounded on all four sides]
        //   [peek row — just the numbers of the next week]
        // No overlay, no offset hacks — each section has its own space and
        // the card naturally stretches to fill whatever's between the two
        // week strips.
        val date = selectedDate
        val weekTasks by viewModel.allTasks.collectAsState()
        // Crossfade between the three calendar modes so swapping views
        // animates instead of snapping — mirrors the smooth month↔week
        // transition in Samsung/Google Calendar.
        if (viewMode == CalendarViewMode.WEEK) {
            val anchor = date ?: java.time.LocalDate.now()
            val week = remember(anchor) { weekContaining(anchor) }
            WeekTimelineView(
                weekDates = week,
                allTasks = weekTasks,
                today = java.time.LocalDate.now(),
                selectedDate = anchor,
                onDateSelected = { viewModel.selectDate(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else if (viewMode == CalendarViewMode.YEAR) {
            YearView(
                years = viewModel.yearsList,
                initialYear = initialMonth.year,
                lookup = lookup,
                onMonthSelected = { ym ->
                    viewModel.onVisibleMonthChanged(ym)
                    viewMode = CalendarViewMode.MONTH
                },
                onVisibleYearChanged = { viewModel.onVisibleYearChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else Crossfade(
            targetState = date,
            animationSpec = tween(durationMillis = 280),
            label = "calendar_mode",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { currentDate ->
          if (currentDate == null) {
            ScrollableMonthCalendar(
                months = viewModel.monthsList,
                selectedDate = null,
                lookup = lookup,
                initialMonth = initialMonth,
                onDateSelected = { viewModel.selectDate(it) },
                onVisibleMonthChanged = { viewModel.onVisibleMonthChanged(it) },
                modifier = Modifier.fillMaxSize()
            )
          } else {
            val today = remember { java.time.LocalDate.now() }

            // Drag state — floating preview + drop-target bounds registry.
            var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
            val dateBounds = remember { mutableStateMapOf<java.time.LocalDate, Rect>() }
            // Pending recurring-task drop — shown when user drags a repeating
            // task, deferred until they pick "지금 반복" vs "모든 미완료 주기".
            var pendingRecurringMove by remember {
                mutableStateOf<Triple<com.bettertick.data.model.Task, java.time.LocalDate, java.time.LocalDate>?>(null)
            }
            // The week strip previews other weeks during a drag without
            // changing selectedDate — that would remount the LazyColumn and
            // kill the in-flight pointerInput. displayedAnchor drives only
            // the strip; currentDate keeps owning the task list + focus.
            var displayedAnchor by remember { mutableStateOf(currentDate) }
            androidx.compose.runtime.LaunchedEffect(currentDate) {
                displayedAnchor = currentDate
            }
            androidx.compose.runtime.LaunchedEffect(activeDrag) {
                if (activeDrag == null) displayedAnchor = currentDate
            }
            val selectedWeek = remember(displayedAnchor) { weekContaining(displayedAnchor) }
            // Hovered date cell under the drag pointer — drives the
            // translucent highlight on the week strip.
            val hoveredDate: java.time.LocalDate? = activeDrag?.let { drag ->
                dateBounds.entries.firstOrNull { (_, r) -> r.contains(drag.currentOffset) }?.key
            }
            // Edge-scroll zones: while the drag pointer sits in the left or
            // right gutter of the screen, advance the PREVIEW week every
            // ~500ms. Only displayedAnchor changes — the task list stays
            // put so the drag gesture survives the week flip.
            val density = androidx.compose.ui.platform.LocalDensity.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val edgeZonePx = with(density) { 20.dp.toPx() }
            val inLeftEdge = activeDrag?.let { it.currentOffset.x < edgeZonePx } == true
            val inRightEdge = activeDrag?.let { it.currentOffset.x > screenWidthPx - edgeZonePx } == true
            androidx.compose.runtime.LaunchedEffect(inLeftEdge, inRightEdge, displayedAnchor) {
                if (!inLeftEdge && !inRightEdge) return@LaunchedEffect
                // Brief initial delay so a drag that merely passes through
                // the edge doesn't trigger an unwanted jump.
                kotlinx.coroutines.delay(400)
                while (inLeftEdge || inRightEdge) {
                    val shift = if (inLeftEdge) -7L else 7L
                    displayedAnchor = displayedAnchor.plusDays(shift)
                    kotlinx.coroutines.delay(500)
                }
            }
            // Row positions report via positionInRoot() (window-ish coords),
            // but Modifier.offset on the preview is applied relative to this
            // outer Box. Track the Box's own root position so we can
            // subtract it and keep the preview glued to the finger.
            var outerBoxRootPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            var previewSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

            // Outer Box stacks the floating drag preview on top of the column.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { outerBoxRootPos = it.positionInRoot() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Selected week strip — draggable via tap-drop.
                    DragAwareWeekStrip(
                        week = selectedWeek,
                        today = today,
                        selectedDate = currentDate,
                        lookup = lookup,
                        activeDrag = activeDrag,
                        hoveredDate = hoveredDate,
                        onDateSelected = { viewModel.selectDate(it) },
                        onDateBounds = { d, r -> dateBounds[d] = r },
                        onExpandMonth = { viewModel.clearSelection() }
                    )

                    // Task card
                    SelectedDatePanel(
                        tasks = lookup.tasksOn(currentDate),
                        selectedDate = currentDate,
                        onToggleComplete = { task ->
                            viewModel.toggleTaskComplete(task.id, !task.isCompleted)
                        },
                        onTaskClick = {},
                        detailListName = { task -> viewModel.listNameFor(task.listId) },
                        onUpdateTask = { updated -> viewModel.updateTask(updated) },
                        onAbandon = { task ->
                            viewModel.setAbandoned(task.id, true)
                        },
                        onUnabandon = { task ->
                            viewModel.setAbandoned(task.id, false)
                        },
                        onDelete = { task -> viewModel.deleteTask(task.id) },
                        onSkipOccurrence = { task, date ->
                            viewModel.skipTaskOccurrence(task.id, date)
                        },
                        tags = viewModel.tags.collectAsState().value,
                        onCreateTag = { viewModel.createTag(it) },
                        onDragTaskUpdate = { drag -> activeDrag = drag },
                        onDragTaskRelease = { task ->
                            val drop = activeDrag?.currentOffset
                            if (drop != null) {
                                val targetDate = dateBounds.entries
                                    .firstOrNull { (_, r) -> r.contains(drop) }
                                    ?.key
                                if (targetDate != null) {
                                    if (task.repeatRule.isNullOrBlank()) {
                                        viewModel.moveTaskToDate(task.id, targetDate)
                                    } else {
                                        // Recurring — pause for the scope dialog
                                        // so the user chooses between moving a
                                        // single occurrence vs the whole series.
                                        pendingRecurringMove = Triple(task, currentDate, targetDate)
                                    }
                                    // Follow the task to the drop date so the
                                    // user sees where it landed, especially
                                    // after a multi-week edge-scroll drag.
                                    if (targetDate != currentDate) {
                                        viewModel.selectDate(targetDate)
                                    }
                                }
                            }
                            activeDrag = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }

                // Floating drag preview — center on the finger regardless
                // of title width (previous fixed -80/-24 hack drifted left
                // for short titles and right for long ones). currentOffset
                // is in root coords; subtract the Box's own root position
                // so the offset is applied in this Box's frame.
                pendingRecurringMove?.let { (task, source, target) ->
                    com.bettertick.ui.screens.tasks.components.RecurringScopeDialog(
                        title = "반복 할일 편집",
                        body = "반복 작업의 시간을 수정하고 있습니다. 수정 범위를 확인해주세요.",
                        onDismiss = { pendingRecurringMove = null },
                        onChoice = { scope ->
                            when (scope) {
                                com.bettertick.ui.screens.tasks.components.RecurringScope.ThisOccurrence ->
                                    viewModel.moveTaskOccurrence(task.id, source, target)
                                com.bettertick.ui.screens.tasks.components.RecurringScope.AllIncomplete ->
                                    viewModel.moveTaskToDate(task.id, target)
                            }
                            pendingRecurringMove = null
                        }
                    )
                }

                activeDrag?.let { drag ->
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { previewSize = it.size }
                            .offset {
                                IntOffset(
                                    (drag.currentOffset.x - outerBoxRootPos.x).roundToInt() - previewSize.width / 2,
                                    (drag.currentOffset.y - outerBoxRootPos.y).roundToInt() - previewSize.height / 2
                                )
                            }
                            .background(
                                DarkCard.copy(alpha = 0.95f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Orange, RoundedCornerShape(8.dp))
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
          }
        }
    }
}

/**
 * Week strip that reports each date cell's global bounds so the parent can
 * hit-test an in-flight drag. Shows a thin highlight on the cell currently
 * under the drag pointer.
 */
@Composable
private fun DragAwareWeekStrip(
    week: List<java.time.LocalDate>,
    today: java.time.LocalDate,
    selectedDate: java.time.LocalDate,
    lookup: TaskDateLookup,
    activeDrag: ActiveDrag?,
    hoveredDate: java.time.LocalDate?,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDateBounds: (java.time.LocalDate, Rect) -> Unit,
    onExpandMonth: () -> Unit
) {
    val selectedWeek = week.take(7)
    val selectedWeekList = remember(selectedWeek) { listOf(selectedWeek) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragEnd = { total = 0f },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        total += dragAmount
                        if (total > 60f) {
                            total = 0f
                            onExpandMonth()
                        }
                    }
                )
            }
    ) {
        // Inner wrapper holds the original 8dp side padding so the date
        // cells keep their visual margin; the outer Box spans edge-to-edge
        // so the drag edge zones can sit in the gutters.
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            WeekRow(
                week = selectedWeek,
                weekIdx = 0,
                allWeeks = selectedWeekList,
                today = today,
                selectedDate = selectedDate,
                lookup = lookup,
                onDateSelected = onDateSelected
            )

        // Invisible overlay rows with onGloballyPositioned, so the parent
        // can hit-test the floating drag preview against each date cell.
        // Also paints a translucent circle over the date currently under
        // the drag pointer (only one cell at a time, not the whole week).
        Row(modifier = Modifier.fillMaxWidth()) {
            selectedWeek.forEach { d ->
                val isHovered = activeDrag != null && hoveredDate == d
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            onDateBounds(
                                d,
                                Rect(
                                    pos.x,
                                    pos.y,
                                    pos.x + coords.size.width,
                                    pos.y + coords.size.height
                                )
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isHovered) {
                        // Same 32dp circle as DayCell uses for selection,
                        // rendered semi-transparent so the date number
                        // beneath stays readable.
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Orange.copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }
        } // close inner padded Box

        // Left/right week-navigation edge zones — visible only while a
        // drag is in flight. Hold a drag inside either box to advance the
        // week (see the LaunchedEffect in CalendarScreen). Sized 36dp wide
        // / 84dp tall to match WeekRow's height and the edgeZonePx check.
        if (activeDrag != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                    .background(Orange.copy(alpha = 0.18f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(Orange.copy(alpha = 0.18f))
            )
        }
    }
}
