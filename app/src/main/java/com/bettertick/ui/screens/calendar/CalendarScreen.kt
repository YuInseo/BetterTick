package com.bettertick.ui.screens.calendar

import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextTertiary
import com.bettertick.ui.theme.ThemeState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.Task
import com.bettertick.ui.screens.tasks.components.TaskDetailSheet
import com.bettertick.ui.screens.calendar.components.ActiveDrag
import com.bettertick.ui.screens.calendar.components.ListCalendarView
import com.bettertick.ui.screens.calendar.components.ScrollableMonthCalendar
import com.bettertick.ui.screens.calendar.components.SelectedDatePanel
import com.bettertick.ui.screens.calendar.components.TaskDateLookup
import com.bettertick.ui.screens.calendar.components.WeekRow
import com.bettertick.ui.screens.calendar.components.WeekTimelineView
import com.bettertick.ui.screens.calendar.components.YearView
import com.bettertick.ui.screens.calendar.components.weekContaining
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary
import com.google.firebase.Timestamp
import java.time.LocalTime
import java.time.ZoneId
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

private fun CalendarViewMode.zoomIn(): CalendarViewMode = when (this) {
    CalendarViewMode.YEAR -> CalendarViewMode.MONTH
    CalendarViewMode.MONTH -> CalendarViewMode.WEEK
    CalendarViewMode.WEEK -> CalendarViewMode.THREE_DAY
    CalendarViewMode.THREE_DAY -> CalendarViewMode.DAY
    else -> this
}

private fun CalendarViewMode.zoomOut(): CalendarViewMode = when (this) {
    CalendarViewMode.DAY -> CalendarViewMode.THREE_DAY
    CalendarViewMode.THREE_DAY -> CalendarViewMode.WEEK
    CalendarViewMode.WEEK -> CalendarViewMode.MONTH
    CalendarViewMode.MONTH -> CalendarViewMode.YEAR
    else -> this
}

private val timelineViewModes = setOf(
    CalendarViewMode.WEEK,
    CalendarViewMode.THREE_DAY,
    CalendarViewMode.DAY
)

private data class CreatePreset(
    val date: java.time.LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onSelectedDateChanged: (java.time.LocalDate?) -> Unit = {},
    onOpenViewOptions: () -> Unit = {}
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    androidx.compose.runtime.LaunchedEffect(selectedDate) {
        onSelectedDateChanged(selectedDate)
    }
    val tasksByDate by viewModel.tasksByDate.collectAsState()
    val calLists by viewModel.lists.collectAsState()
    val calTags by viewModel.tags.collectAsState()
    val focusSessions by viewModel.focusSessions.collectAsState()
    val undatedTasks by viewModel.undatedTasks.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // ── 옵션 보기 / 필터 보기 범위 applied to everything the calendar draws ──
    val todayDate = java.time.LocalDate.now()
    val filterKey = listOf<Any>(
        CalendarPrefs.showCompleted, CalendarPrefs.hiddenListIds, CalendarPrefs.tagFilter
    )
    val repeatCycles = CalendarPrefs.showRepeatCycles
    val checkItems = CalendarPrefs.showCheckItems
    // The "current cycle" of each recurring task: its first occurrence from
    // today on (or its last one when the series has ended). Used when
    // 반복 주기 표시 is off so a repeat shows up once instead of every day.
    val currentCycle: Map<String, java.time.LocalDate> = remember(tasksByDate) {
        val next = mutableMapOf<String, java.time.LocalDate>()
        val last = mutableMapOf<String, java.time.LocalDate>()
        tasksByDate.keys.sorted().forEach { d ->
            tasksByDate[d].orEmpty().forEach { t ->
                if (!t.repeatRule.isNullOrBlank()) {
                    if (!d.isBefore(todayDate) && t.id !in next) next[t.id] = d
                    last[t.id] = d
                }
            }
        }
        last.forEach { (id, d) -> next.putIfAbsent(id, d) }
        next
    }
    val occurrenceFilter: (Task, java.time.LocalDate) -> Boolean = remember(currentCycle, repeatCycles) {
        { t, d ->
            repeatCycles || t.repeatRule.isNullOrBlank() ||
                (currentCycle[parentTaskId(t.id)]?.let { it == d } ?: true)
        }
    }
    val filteredByDate = remember(tasksByDate, filterKey, occurrenceFilter) {
        tasksByDate.mapValues { (d, l) -> l.filter { CalendarPrefs.passesFilter(it) && occurrenceFilter(it, d) } }
    }
    // Panel lists keep real tasks only; grid chips may add check items.
    val lookup = remember(filteredByDate) { TaskDateLookup(filteredByDate) }
    val displayLookup = remember(filteredByDate, checkItems) {
        if (!checkItems) lookup
        else TaskDateLookup(filteredByDate.mapValues { (_, l) ->
            l.flatMap { listOf(it) + it.checkItemEntries() }.filter { CalendarPrefs.passesFilter(it) }
        })
    }
    val taskColor: (Task) -> androidx.compose.ui.graphics.Color = remember(calLists, calTags) {
        { t -> CalendarPrefs.colorFor(t, calLists, calTags) }
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showArrange by remember { mutableStateOf(false) }
    var arrangeTask by remember { mutableStateOf<Task?>(null) }
    androidx.compose.runtime.DisposableEffect(showArrange) {
        CalendarPrefs.arrangePanelOpen = showArrange
        onDispose { CalendarPrefs.arrangePanelOpen = false }
    }
    // Shared vertical position of the week/3-day/day timeline while paging.
    val timelineScroll = remember { IntArray(1) { -1 } }

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var hourHeight by remember { mutableStateOf(52.dp) }
    val minHourHeight = 32.dp
    val maxHourHeight = 140.dp

    var createPreset by remember { mutableStateOf<CreatePreset?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    val tags by viewModel.tags.collectAsState()

    val monthFormatter = DateTimeFormatter.ofPattern("M월", Locale.KOREAN)
    val initialMonth = remember { selectedMonth }

    /** Date taps: assign the picked task while 할 일 배정하기 is active. */
    val onDatePick: (java.time.LocalDate) -> Unit = { d ->
        val t = arrangeTask
        if (t != null) {
            viewModel.moveTaskToDate(t.id, d)
            arrangeTask = null
            android.widget.Toast.makeText(
                context, "'${t.title}'을(를) ${d.monthValue}월 ${d.dayOfMonth}일에 배정했습니다",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else viewModel.selectDate(d)
    }
    val onTimelineDatePick: (java.time.LocalDate) -> Unit = { d ->
        if (arrangeTask != null) onDatePick(d) else viewModel.setSelectedDate(d)
    }
    val openTask: (Task) -> Unit = { t ->
        val parentId = parentTaskId(t.id)
        selectedTask = if (parentId == t.id) t
            else viewModel.allTasks.value.firstOrNull { it.id == parentId } ?: t
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalTaskColor provides taskColor) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(viewMode) {
                var acc = 1f
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom == 1f) return@detectTransformGestures
                    if (viewMode in timelineViewModes) {
                        hourHeight = (hourHeight.value * zoom)
                            .coerceIn(minHourHeight.value, maxHourHeight.value).dp
                    }
                    acc *= zoom
                    val next = when {
                        acc > 1.45f -> viewMode.zoomIn()
                        acc < 0.6f -> viewMode.zoomOut()
                        else -> null
                    }
                    if (next != null && next != viewMode) {
                        viewMode = next
                        acc = 1f
                        if (next !in timelineViewModes) hourHeight = 52.dp
                    }
                }
            }
    ) {
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
                    // Timeline / list views follow the shown date, not the
                    // month the month-grid last scrolled to.
                    val titleMonth = if (viewMode == CalendarViewMode.MONTH) selectedMonth
                        else java.time.YearMonth.from(selectedDate ?: java.time.LocalDate.now())
                    Text(
                        text = "${titleMonth.year}년 ${titleMonth.format(monthFormatter)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
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
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

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
                            onClick = { showMoreMenu = false; showFilterSheet = true },
                            leadingIcon = { Icon(Icons.Outlined.FilterList, null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("옵션 보기") },
                            onClick = { showMoreMenu = false; onOpenViewOptions() },
                            leadingIcon = { Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("할 일 배정하기") },
                            onClick = { showMoreMenu = false; showArrange = true; arrangeTask = null },
                            leadingIcon = { Icon(Icons.Outlined.TaskAlt, null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("공유") },
                            onClick = {
                                showMoreMenu = false
                                val anchor = selectedDate ?: java.time.LocalDate.now()
                                val (title, dates) = when (viewMode) {
                                    CalendarViewMode.WEEK -> "${anchor.year}년 ${anchor.monthValue}월 주간 일정" to weekContaining(anchor)
                                    CalendarViewMode.THREE_DAY -> "3일 일정" to listOf(anchor.minusDays(1), anchor, anchor.plusDays(1))
                                    CalendarViewMode.DAY -> "${anchor.monthValue}월 ${anchor.dayOfMonth}일 일정" to listOf(anchor)
                                    CalendarViewMode.LIST -> "다가오는 일정" to (0L until 30L).map { java.time.LocalDate.now().plusDays(it) }
                                    else -> "${selectedMonth.year}년 ${selectedMonth.monthValue}월 일정" to
                                        (1..selectedMonth.lengthOfMonth()).map { selectedMonth.atDay(it) }
                                }
                                val shareTasks = viewModel.allTasks.value.filter { CalendarPrefs.passesFilter(it) }
                                shareCalendarText(context, buildCalendarShareText(title, dates, shareTasks))
                            },
                            leadingIcon = { Icon(Icons.Outlined.Share, null, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        if (viewMode == CalendarViewMode.MONTH) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                com.bettertick.ui.theme.AppPrefs.weekdayLabels().forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        val date = selectedDate
        val weekTasks by viewModel.allTasks.collectAsState()

        when (viewMode) {
            CalendarViewMode.WEEK, CalendarViewMode.THREE_DAY, CalendarViewMode.DAY -> {
                val anchor = date ?: java.time.LocalDate.now()
                val timelineTasks = remember(weekTasks, filterKey, checkItems) {
                    val base = weekTasks.filter { CalendarPrefs.passesFilter(it) }
                    if (!checkItems) base
                    else base.flatMap { listOf(it) + it.checkItemEntries() }.filter { CalendarPrefs.passesFilter(it) }
                }
                val secondaryZone = CalendarPrefs.secondaryZones.firstOrNull()
                    ?.takeIf { CalendarPrefs.secondaryTzEnabled }
                val shownFocus = if (CalendarPrefs.showFocusRecords) focusSessions else emptyList()
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (viewMode == CalendarViewMode.DAY) {
                        DayViewNavStrip(
                            week = weekContaining(anchor),
                            today = java.time.LocalDate.now(),
                            selectedDate = anchor,
                            onDateSelected = onTimelineDatePick
                        )
                    }
                    // Swipe left / right to page by a week, 3 days or a day.
                    TimelinePager(
                        mode = viewMode,
                        selected = anchor,
                        onSettle = { viewModel.setSelectedDate(it) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { dates ->
                        WeekTimelineView(
                            weekDates = dates,
                            allTasks = timelineTasks,
                            today = java.time.LocalDate.now(),
                            selectedDate = anchor,
                            onDateSelected = onTimelineDatePick,
                            showDayHeader = viewMode != CalendarViewMode.DAY,
                            hourHeight = hourHeight,
                            onCreateTask = { d, t, dur -> createPreset = CreatePreset(d, t, dur) },
                            onTaskClick = openTask,
                            focusSessions = shownFocus,
                            secondaryZoneId = secondaryZone,
                            initialScrollPx = timelineScroll[0].takeIf { it >= 0 },
                            onScrollChanged = { timelineScroll[0] = it },
                            occurrenceFilter = occurrenceFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            CalendarViewMode.LIST -> {
                ListCalendarView(
                    allTasks = weekTasks.filter { CalendarPrefs.passesFilter(it) },
                    today = java.time.LocalDate.now(),
                    selectedDate = date,
                    onDateSelected = onDatePick,
                    occurrenceFilter = occurrenceFilter,
                    onTaskClick = openTask,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            CalendarViewMode.YEAR -> {
                YearView(
                    years = viewModel.yearsList,
                    initialYear = initialMonth.year,
                    lookup = lookup,
                    onMonthSelected = { ym ->
                        viewModel.onVisibleMonthChanged(ym)
                        viewMode = CalendarViewMode.MONTH
                    },
                    onVisibleYearChanged = { viewModel.onVisibleYearChanged(it) },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            CalendarViewMode.MONTH -> Crossfade(
                targetState = date,
                animationSpec = tween(durationMillis = 280),
                label = "calendar_month",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { currentDate ->
                if (currentDate == null) {
                    ScrollableMonthCalendar(
                        months = viewModel.monthsList,
                        selectedDate = null,
                        lookup = displayLookup,
                        initialMonth = initialMonth,
                        onDateSelected = onDatePick,
                        onVisibleMonthChanged = { viewModel.onVisibleMonthChanged(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val today = remember { java.time.LocalDate.now() }
                    var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
                    val dateBounds = remember { mutableStateMapOf<java.time.LocalDate, Rect>() }
                    var pendingRecurringMove by remember {
                        mutableStateOf<Triple<com.bettertick.data.model.Task, java.time.LocalDate, java.time.LocalDate>?>(null)
                    }
                    var displayedAnchor by remember { mutableStateOf(currentDate) }
                    androidx.compose.runtime.LaunchedEffect(currentDate) { displayedAnchor = currentDate }
                    androidx.compose.runtime.LaunchedEffect(activeDrag) {
                        if (activeDrag == null) displayedAnchor = currentDate
                    }
                    val selectedWeek = remember(displayedAnchor) { weekContaining(displayedAnchor) }
                    val hoveredDate: java.time.LocalDate? = activeDrag?.let { drag ->
                        dateBounds.entries.firstOrNull { (_, r) -> r.contains(drag.currentOffset) }?.key
                    }
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                    val edgeZonePx = with(density) { 20.dp.toPx() }
                    val inLeftEdge = activeDrag?.let { it.currentOffset.x < edgeZonePx } == true
                    val inRightEdge = activeDrag?.let { it.currentOffset.x > screenWidthPx - edgeZonePx } == true
                    androidx.compose.runtime.LaunchedEffect(inLeftEdge, inRightEdge, displayedAnchor) {
                        if (!inLeftEdge && !inRightEdge) return@LaunchedEffect
                        kotlinx.coroutines.delay(400)
                        while (inLeftEdge || inRightEdge) {
                            val shift = if (inLeftEdge) -7L else 7L
                            displayedAnchor = displayedAnchor.plusDays(shift)
                            kotlinx.coroutines.delay(500)
                        }
                    }
                    var outerBoxRootPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    var previewSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { outerBoxRootPos = it.positionInRoot() }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            DragAwareWeekStrip(
                                week = selectedWeek,
                                today = today,
                                selectedDate = currentDate,
                                lookup = displayLookup,
                                activeDrag = activeDrag,
                                hoveredDate = hoveredDate,
                                onDateSelected = onDatePick,
                                onDateBounds = { d, r -> dateBounds[d] = r },
                                onExpandMonth = { viewModel.clearSelection() }
                            )
                            SelectedDatePanel(
                                tasks = lookup.tasksOn(currentDate),
                                selectedDate = currentDate,
                                onToggleComplete = { task ->
                                    viewModel.toggleTaskComplete(task.id, !task.isCompleted)
                                },
                                onTaskClick = {},
                                detailListName = { task -> viewModel.listNameFor(task.listId) },
                                onUpdateTask = { updated -> viewModel.updateTask(updated) },
                                onAbandon = { task -> viewModel.setAbandoned(task.id, true) },
                                onUnabandon = { task -> viewModel.setAbandoned(task.id, false) },
                                onDelete = { task -> viewModel.deleteTask(task.id) },
                                onSkipOccurrence = { task, d -> viewModel.skipTaskOccurrence(task.id, d) },
                                tags = viewModel.tags.collectAsState().value,
                                onCreateTag = { viewModel.createTag(it) },
                                onDragTaskUpdate = { drag -> activeDrag = drag },
                                onDragTaskRelease = { task ->
                                    val drop = activeDrag?.currentOffset
                                    if (drop != null) {
                                        val targetDate = dateBounds.entries
                                            .firstOrNull { (_, r) -> r.contains(drop) }?.key
                                        if (targetDate != null) {
                                            if (task.repeatRule.isNullOrBlank()) {
                                                viewModel.moveTaskToDate(task.id, targetDate)
                                            } else {
                                                pendingRecurringMove = Triple(task, currentDate, targetDate)
                                            }
                                            if (targetDate != currentDate) viewModel.selectDate(targetDate)
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
                }
            }
        }

        if (showArrange) {
            ArrangeTasksPanel(
                tasks = undatedTasks.filter { CalendarPrefs.passesFilter(it) },
                lists = calLists,
                selectedTaskId = arrangeTask?.id,
                onSelect = { arrangeTask = it },
                onClose = { showArrange = false; arrangeTask = null }
            )
        }
    }
    }

    if (showFilterSheet) {
        CalendarFilterSheet(
            lists = calLists,
            tags = calTags,
            onDismiss = { showFilterSheet = false }
        )
    }

    selectedTask?.let { task ->
        TaskDetailSheet(
            task = task,
            listName = viewModel.listNameFor(task.listId),
            onDismiss = { selectedTask = null },
            onUpdateTask = { updated -> viewModel.updateTask(updated); selectedTask = null },
            onToggleComplete = { viewModel.toggleTaskComplete(task.id, !task.isCompleted) },
            tags = tags,
            onCreateTag = { name -> viewModel.createTag(name) },
            lists = calLists,
            onDuplicate = { viewModel.duplicateTask(it) },
            onDelete = { viewModel.deleteTask(it.id) }
        )
    }

    // Quick-add bottom sheet — slides up after drag-to-create on the timeline
    createPreset?.let { preset ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var taskTitle by remember(preset) { mutableStateOf("") }
        var taskNote by remember(preset) { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        ModalBottomSheet(
            onDismissRequest = { createPreset = null },
            sheetState = sheetState,
            containerColor = DarkSurface,
            dragHandle = {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 12.dp)
                    .imePadding()
            ) {
                // Title input
                BasicTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (taskTitle.isNotBlank()) {
                            val instant = preset.date.atTime(preset.startTime)
                                .atZone(ZoneId.systemDefault()).toInstant()
                            viewModel.createTask(
                                Task(
                                    title = taskTitle.trim(),
                                    dueDate = Timestamp(instant.epochSecond, 0),
                                    durationMinutes = preset.durationMinutes
                                )
                            )
                        }
                        createPreset = null
                    }),
                    decorationBox = { innerTextField ->
                        if (taskTitle.isEmpty()) {
                            Text(
                                "무엇을 하고 싶으신가요?",
                                fontSize = 20.sp,
                                color = TextTertiary
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(Modifier.height(14.dp))

                // Note input
                BasicTextField(
                    value = taskNote,
                    onValueChange = { taskNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 14.sp, color = TextSecondary),
                    decorationBox = { innerTextField ->
                        if (taskNote.isEmpty()) {
                            Text("설명", fontSize = 14.sp, color = TextTertiary)
                        }
                        innerTextField()
                    }
                )

                Spacer(Modifier.height(20.dp))

                // Action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val today = remember { java.time.LocalDate.now() }
                    val isToday = preset.date == today
                    val dow = listOf("일", "월", "화", "수", "목", "금", "토")
                    val dowLabel = dow[preset.date.dayOfWeek.value % 7]
                    val dateLabel = if (isToday) "오늘"
                        else "${preset.date.monthValue}월 ${preset.date.dayOfMonth}일 ($dowLabel)"

                    // Date chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ThemeState.accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ThemeState.accentColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Today,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = ThemeState.accentColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(dateLabel, fontSize = 12.sp, color = ThemeState.accentColor)
                        }
                    }

                    Spacer(Modifier.width(2.dp))

                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(20.dp), tint = TextSecondary)
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Label, null, modifier = Modifier.size(20.dp), tint = TextSecondary)
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.MoveToInbox, null, modifier = Modifier.size(20.dp), tint = TextSecondary)
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.MoreHoriz, null, modifier = Modifier.size(20.dp), tint = TextSecondary)
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Mic, null, modifier = Modifier.size(20.dp), tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun DayViewNavStrip(
    week: List<java.time.LocalDate>,
    today: java.time.LocalDate,
    selectedDate: java.time.LocalDate,
    onDateSelected: (java.time.LocalDate) -> Unit
) {
    val koreanDow = listOf("일", "월", "화", "수", "목", "금", "토")
    val timeGutter = 52.dp
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(timeGutter))
            week.forEach { d ->
                val dowIdx = d.dayOfWeek.value % 7
                Text(
                    text = koreanDow[dowIdx],
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(timeGutter))
            week.forEach { d ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .clickable { onDateSelected(d) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (d == selectedDate) Modifier.clip(CircleShape).background(ThemeState.accentColor)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = d.dayOfMonth.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                d == selectedDate -> Color.White
                                d == today -> ThemeState.accentColor
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
            }
        }
    }
}

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
                        if (total > 60f) { total = 0f; onExpandMonth() }
                    }
                )
            }
    ) {
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
            Row(modifier = Modifier.fillMaxWidth()) {
                selectedWeek.forEach { d ->
                    val isHovered = activeDrag != null && hoveredDate == d
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                onDateBounds(d, Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height))
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (isHovered) {
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(32.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            )
                        }
                    }
                }
            }
        }

        if (activeDrag != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp).height(84.dp)
                    .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp).height(84.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            )
        }
    }
}

/**
 * Horizontally swipeable timeline: each page is one week (주), three days
 * (3일) or one day (일). Settling on a page moves the selection; selecting a
 * date elsewhere (오늘 button, day strip) pages to it.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TimelinePager(
    mode: CalendarViewMode,
    selected: java.time.LocalDate,
    onSettle: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (List<java.time.LocalDate>) -> Unit
) {
    val center = 5_000
    val step = when (mode) {
        CalendarViewMode.WEEK -> 7L
        CalendarViewMode.THREE_DAY -> 3L
        else -> 1L
    }
    var base by remember(mode) { mutableStateOf(selected) }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = center) { center * 2 }
    fun anchorAt(page: Int) = base.plusDays((page - center) * step)
    fun datesFor(a: java.time.LocalDate): List<java.time.LocalDate> = when (mode) {
        CalendarViewMode.WEEK -> weekContaining(a)
        CalendarViewMode.THREE_DAY -> listOf(a.minusDays(1), a, a.plusDays(1))
        else -> listOf(a)
    }
    val latestSelected by androidx.compose.runtime.rememberUpdatedState(selected)

    // Selection changed from outside the pager → page (or re-center) to it.
    androidx.compose.runtime.LaunchedEffect(selected, mode) {
        val page = pagerState.currentPage
        val cur = anchorAt(page)
        if (selected in datesFor(cur)) {
            // 3일 view keeps the selected day in the middle.
            if (mode == CalendarViewMode.THREE_DAY && selected != cur) {
                base = selected.minusDays((page - center) * step)
            }
            return@LaunchedEffect
        }
        val pages = when (mode) {
            CalendarViewMode.WEEK -> java.time.temporal.ChronoUnit.WEEKS.between(
                weekContaining(cur).first(), weekContaining(selected).first()
            ).toInt()
            else -> (java.time.temporal.ChronoUnit.DAYS.between(cur, selected) / step).toInt()
                .let { if (it == 0) (if (selected.isAfter(cur)) 1 else -1) else it }
        }
        val target = (page + pages).coerceIn(0, center * 2 - 1)
        base = selected.minusDays((target - center) * step)
        if (kotlin.math.abs(pages) <= 2) pagerState.animateScrollToPage(target)
        else pagerState.scrollToPage(target)
    }

    // User swiped to another page → move the selection with it.
    androidx.compose.runtime.LaunchedEffect(pagerState, mode) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }
            .collect { p ->
                val a = anchorAt(p)
                val sel = latestSelected
                val next = if (mode == CalendarViewMode.WEEK) {
                    if (sel in weekContaining(a)) sel else weekContaining(a)[com.bettertick.ui.theme.AppPrefs.dayIndexInWeek(sel.dayOfWeek)]
                } else a
                if (next != sel && sel !in datesFor(a)) onSettle(next)
            }
    }

    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondBoundsPageCount = 1,
        key = { it }
    ) { page ->
        content(datesFor(anchorAt(page)))
    }
}
