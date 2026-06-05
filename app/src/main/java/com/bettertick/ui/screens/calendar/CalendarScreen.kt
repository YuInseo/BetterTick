package com.bettertick.ui.screens.calendar

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
    onSelectedDateChanged: (java.time.LocalDate?) -> Unit = {}
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    androidx.compose.runtime.LaunchedEffect(selectedDate) {
        onSelectedDateChanged(selectedDate)
    }
    val tasksByDate by viewModel.tasksByDate.collectAsState()
    val lookup = remember(tasksByDate) { TaskDateLookup(tasksByDate) }

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
                    Text(
                        text = "${selectedMonth.year}년 ${selectedMonth.format(monthFormatter)}",
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
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.FilterList, null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("옵션 보기") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("할 일 배정하기") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.TaskAlt, null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("공유") },
                            onClick = { showMoreMenu = false },
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
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
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
            CalendarViewMode.WEEK -> {
                val anchor = date ?: java.time.LocalDate.now()
                val week = remember(anchor) { weekContaining(anchor) }
                WeekTimelineView(
                    weekDates = week,
                    allTasks = weekTasks,
                    today = java.time.LocalDate.now(),
                    selectedDate = anchor,
                    onDateSelected = { viewModel.selectDate(it) },
                    hourHeight = hourHeight,
                    onCreateTask = { d, t, dur -> createPreset = CreatePreset(d, t, dur) },
                    onTaskClick = { selectedTask = it },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            CalendarViewMode.THREE_DAY -> {
                val anchor = date ?: java.time.LocalDate.now()
                val threeDays = remember(anchor) {
                    listOf(anchor.minusDays(1), anchor, anchor.plusDays(1))
                }
                WeekTimelineView(
                    weekDates = threeDays,
                    allTasks = weekTasks,
                    today = java.time.LocalDate.now(),
                    selectedDate = anchor,
                    onDateSelected = { viewModel.selectDate(it) },
                    hourHeight = hourHeight,
                    onCreateTask = { d, t, dur -> createPreset = CreatePreset(d, t, dur) },
                    onTaskClick = { selectedTask = it },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            CalendarViewMode.DAY -> {
                val anchor = date ?: java.time.LocalDate.now()
                val week = remember(anchor) { weekContaining(anchor) }
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DayViewNavStrip(
                        week = week,
                        today = java.time.LocalDate.now(),
                        selectedDate = anchor,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                    WeekTimelineView(
                        weekDates = listOf(anchor),
                        allTasks = weekTasks,
                        today = java.time.LocalDate.now(),
                        selectedDate = anchor,
                        onDateSelected = { viewModel.selectDate(it) },
                        showDayHeader = false,
                        hourHeight = hourHeight,
                        onCreateTask = { d, t, dur -> createPreset = CreatePreset(d, t, dur) },
                        onTaskClick = { selectedTask = it },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }

            CalendarViewMode.LIST -> {
                ListCalendarView(
                    allTasks = weekTasks,
                    today = java.time.LocalDate.now(),
                    selectedDate = date,
                    onDateSelected = { viewModel.selectDate(it) },
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
                        lookup = lookup,
                        initialMonth = initialMonth,
                        onDateSelected = { viewModel.selectDate(it) },
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
                                lookup = lookup,
                                activeDrag = activeDrag,
                                hoveredDate = hoveredDate,
                                onDateSelected = { viewModel.selectDate(it) },
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
    }

    selectedTask?.let { task ->
        TaskDetailSheet(
            task = task,
            listName = viewModel.listNameFor(task.listId),
            onDismiss = { selectedTask = null },
            onUpdateTask = { updated -> viewModel.updateTask(updated); selectedTask = null },
            onToggleComplete = { viewModel.toggleTaskComplete(task.id, !task.isCompleted) },
            tags = tags,
            onCreateTag = { name -> viewModel.createTag(name) }
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
            containerColor = Color(0xFF1C1C1E),
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
                                color = Color(0xFF6B6B6B)
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
                    textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF8A8A8E)),
                    decorationBox = { innerTextField ->
                        if (taskNote.isEmpty()) {
                            Text("설명", fontSize = 14.sp, color = Color(0xFF555558))
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
                        color = Color(0xFFFF8C00).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFFF8C00).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Today,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFFFF8C00)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(dateLabel, fontSize = 12.sp, color = Color(0xFFFF8C00))
                        }
                    }

                    Spacer(Modifier.width(2.dp))

                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(20.dp), tint = Color(0xFF8A8A8E))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Label, null, modifier = Modifier.size(20.dp), tint = Color(0xFF8A8A8E))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.MoveToInbox, null, modifier = Modifier.size(20.dp), tint = Color(0xFF8A8A8E))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.MoreHoriz, null, modifier = Modifier.size(20.dp), tint = Color(0xFF8A8A8E))
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Mic, null, modifier = Modifier.size(20.dp), tint = Color(0xFF8A8A8E))
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
                                if (d == selectedDate) Modifier.clip(CircleShape).background(Color(0xFF2F7CF6))
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
                                d == today -> Color(0xFF2F7CF6)
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
