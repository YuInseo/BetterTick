package com.bettertick.ui.screens.habits

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.Habit
import com.bettertick.ui.components.AppActionButton
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val HabitBlue = Color(0xFF4A90E2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()
    val archivedHabits by viewModel.archivedHabits.collectAsState()
    val weekDates by viewModel.weekDates.collectAsState()
    val weekStart by viewModel.weekStart.collectAsState()
    val weekLogs by viewModel.weekLogs.collectAsState()
    val detailLogs by viewModel.detailLogs.collectAsState()
    val detailLogRecords by viewModel.detailLogRecords.collectAsState()
    val weekLogRecords by viewModel.weekLogRecords.collectAsState()

    var showGallery by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var detailHabit by remember { mutableStateOf<Habit?>(null) }
    var showList by remember { mutableStateOf(false) }
    var detailMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var logDialogHabit by remember { mutableStateOf<Habit?>(null) }

    when {
        detailHabit != null -> {
            val hid = detailHabit!!.id
            val completed = detailLogs[hid] ?: emptySet()
            HabitDetailScreen(
                habit = detailHabit!!,
                completedDates = completed,
                displayedMonth = detailMonth,
                logRecords = detailLogRecords,
                onDismiss = { detailHabit = null },
                onToggle = { habitId, date -> viewModel.toggleHabit(habitId, date) },
                onMonthChange = { detailMonth = it },
                onSetDateRange = viewModel::setDetailDateRange,
                onSaveLog = { hid2, d, done, mood, note ->
                    viewModel.saveHabitLog(hid2, d, done, mood, note)
                },
                onArchive = { viewModel.archiveHabit(hid) },
                onDelete = { viewModel.deleteHabit(hid) }
            )
            return
        }
        showList -> {
            HabitsListScreen(
                activeHabits = habits,
                archivedHabits = archivedHabits,
                weekLogs = weekLogs,
                onBack = { showList = false },
                onHabitClick = { detailHabit = it },
                onArchive = { viewModel.archiveHabit(it.id) }
            )
            return
        }
        showSettings -> {
            HabitSettingsScreen(onBack = { showSettings = false })
            return
        }
        showGallery -> {
            HabitGalleryScreen(
                onBack = { showGallery = false },
                onAddHabit = { draft ->
                    viewModel.addHabitFromDraft(draft)
                    showGallery = false
                }
            )
            return
        }
    }

    // Last 7 days ending today for the week strip
    val today = LocalDate.now()
    val last7Days = remember(today) { (6L downTo 0L).map { today.minusDays(it) } }

    // Expand state per group
    val expandedGroups = remember { mutableStateMapOf("기타" to true) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "습관",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = TextSecondary)
                    }
                    IconButton(onClick = { showList = true }) {
                        Icon(Icons.Outlined.List, contentDescription = null, tint = TextSecondary)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            AppActionButton(
                icon = Icons.Default.Add,
                contentDescription = "습관 추가",
                onClick = { showGallery = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Last-7-days strip with blue today highlight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                last7Days.forEach { date ->
                    val isToday = date == today
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            color = if (isToday) HabitBlue else TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .then(if (isToday) Modifier.background(HabitBlue) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 16.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (habits.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "습관을 추가해 보세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "+ 버튼으로 첫 습관을 만들어보세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // "기타" group with expand/collapse
                    val expanded = expandedGroups["기타"] ?: true
                    item(key = "group-기타") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedGroups["기타"] = !expanded }
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                                    else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "기타",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                habits.size.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    if (expanded) {
                        items(habits, key = { it.id }) { habit ->
                            val count = weekLogs[habit.id]?.size ?: 0
                            val todayStr = today.toString()
                            val doneToday = weekLogs[habit.id]?.contains(todayStr) == true
                            HabitSummaryRow(
                                habit = habit,
                                weekCount = count,
                                isCompletedToday = doneToday,
                                onClick = { detailHabit = habit },
                                onSwipeRight = { viewModel.toggleHabit(habit.id, today) },
                                onSwipeLeft = { logDialogHabit = habit }
                            )
                        }
                    }
                }
            }
        }
    }

    logDialogHabit?.let { h ->
        val todayStr = today.toString()
        val existing = weekLogRecords["${h.id}|$todayStr"]
        HabitLogDialog(
            date = today,
            initialCompleted = existing?.isCompleted ?: false,
            initialMood = existing?.mood ?: -1,
            initialNote = existing?.note ?: "",
            habitName = h.name,
            habitIcon = h.icon,
            habitColor = h.color,
            onDismiss = { logDialogHabit = null },
            onSave = { done, mood, note ->
                viewModel.saveHabitLog(h.id, today, done, mood, note)
                logDialogHabit = null
            }
        )
    }
}

@Composable
private fun HabitSummaryRow(
    habit: Habit,
    weekCount: Int,
    isCompletedToday: Boolean,
    onClick: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }
    val maxReveal = with(density) { 140.dp.toPx() }

    // Synchronous float state — reads in onDragEnd always see the latest
    // drag delta (the previous Animatable + scope.launch pattern had a
    // race that made the swipe visually work but never fire the toggle).
    val target = remember(habit.id) { mutableFloatStateOf(0f) }
    var dragging by remember(habit.id) { mutableStateOf(false) }

    val displayOffset by animateFloatAsState(
        targetValue = target.floatValue,
        animationSpec = if (dragging) snap() else tween(durationMillis = 220),
        label = "habit-swipe-${habit.id}"
    )

    val swipeRightColor = if (isCompletedToday) Color(0xFFE57373) else Color(0xFF4A90E2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Right-swipe background: blue pill w/ white check circle (switch-style).
        // Visible only while row is pulled rightward.
        if (displayOffset > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(swipeRightColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 6.dp, top = 6.dp, bottom = 6.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompletedToday) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    tint = swipeRightColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Left-swipe background: edit hint on the right side.
        if (displayOffset < 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4A90E2))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(displayOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .pointerInput(habit.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            val x = target.floatValue
                            Log.d("HabitSwipe", "onDragEnd: habit=${habit.id} offset=$x threshold=$threshold")
                            when {
                                x > threshold -> {
                                    Log.d("HabitSwipe", "  -> firing onSwipeRight (toggle)")
                                    onSwipeRight()
                                }
                                x < -threshold -> {
                                    Log.d("HabitSwipe", "  -> firing onSwipeLeft (edit dialog)")
                                    onSwipeLeft()
                                }
                            }
                            dragging = false
                            target.floatValue = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            target.floatValue = 0f
                        },
                        onHorizontalDrag = { change, delta ->
                            change.consume()
                            target.floatValue = (target.floatValue + delta)
                                .coerceIn(-maxReveal, maxReveal)
                        }
                    )
                }
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HabitIconView(
                iconKey = habit.icon,
                colorHex = habit.color,
                circleSize = 40.dp,
                iconSize = 22.dp,
                fallbackText = habit.name
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = weekCount.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "총 일수",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}
