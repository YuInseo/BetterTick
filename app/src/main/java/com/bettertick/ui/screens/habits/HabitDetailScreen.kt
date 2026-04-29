package com.bettertick.ui.screens.habits

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.data.model.Habit
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@Composable
fun HabitDetailScreen(
    habit: Habit,
    completedDates: Set<String>,
    displayedMonth: YearMonth,
    logRecords: Map<String, com.bettertick.data.model.HabitLog> = emptyMap(),
    onDismiss: () -> Unit,
    onToggle: (habitId: String, date: LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onSetDateRange: (start: String, end: String) -> Unit,
    onSaveLog: (habitId: String, date: LocalDate, isCompleted: Boolean, mood: Int, note: String) -> Unit = { _, _, _, _, _ -> },
    onEdit: () -> Unit = {},
    onArchive: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var dateDialogDate by remember { mutableStateOf<LocalDate?>(null) }

    // Keep loaded range synced to displayed month
    LaunchedEffect(displayedMonth) {
        onSetDateRange(
            displayedMonth.atDay(1).format(dateFormatter),
            displayedMonth.atEndOfMonth().format(dateFormatter)
        )
    }

    if (showMore) {
        HabitYearlyView(
            habit = habit,
            completedDates = completedDates,
            onBack = { showMore = false },
            onShare = { showMore = false; showShare = true },
            onSetDateRange = onSetDateRange
        )
        return
    }
    if (showShare) {
        HabitShareView(
            habit = habit,
            completedDates = completedDates,
            onBack = { showShare = false }
        )
        return
    }

    val habitColor = runCatching { Color(android.graphics.Color.parseColor(habit.color)) }
        .getOrDefault(Color(0xFF9B59B6))
    val today = LocalDate.now()

    // Stats
    val totalCompletions = completedDates.size
    val sortedDates = remember(completedDates) {
        completedDates.mapNotNull { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }.sorted()
    }
    val longestStreak = remember(sortedDates) { computeLongestStreak(sortedDates) }
    val currentStreak = remember(sortedDates, today) { computeCurrentStreak(sortedDates, today) }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ===== HERO =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight - 48.dp)
                .background(habitColor)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "메뉴", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            MenuRow(icon = Icons.Default.Edit, label = "편집") {
                                showMenu = false; onEdit()
                            }
                            MenuRow(icon = Icons.Default.TrackChanges, label = "포커스 시작") {
                                showMenu = false
                            }
                            MenuRow(icon = Icons.Default.Share, label = "공유") {
                                showMenu = false; showShare = true
                            }
                            MenuRow(icon = Icons.Default.Archive, label = "기록 보관소") {
                                showMenu = false; onArchive(); onDismiss()
                            }
                            MenuRow(icon = Icons.Default.Delete, label = "삭제") {
                                showMenu = false; showDeleteConfirm = true
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1.2f))

                // Random illustration seeded by habit id
                HabitIllustration(
                    seed = habit.id.hashCode(),
                    accent = habitColor,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = habit.name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (habit.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = habit.description,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Up-arrow hint — placed above the stats so it's in the user's
                // thumb reach rather than buried at the bottom.
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(36.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Stats pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeroStat(value = totalCompletions.toString(), label = "총 체크인 수")
                    HeroStat(value = longestStreak.toString(), label = "최장 연속 기록")
                    HeroStat(value = currentStreak.toString(), label = "연속")
                }

                Spacer(Modifier.height(20.dp))
            }
        }

        // ===== DETAILS =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Monthly calendar — tap opens the rich log dialog.
            MonthCalendarCard(
                yearMonth = displayedMonth,
                completedDates = completedDates,
                today = today,
                onPrev = { onMonthChange(displayedMonth.minusMonths(1)) },
                onNext = { onMonthChange(displayedMonth.plusMonths(1)) },
                onToggle = { date -> if (!date.isAfter(today)) dateDialogDate = date }
            )

            // Stats cards (2×2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("출근 기록 통계", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier.clickable { showMore = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("더 보기", color = TextSecondary, fontSize = 14.sp)
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                val monthlyCount = completedDates.count {
                    runCatching {
                        val d = LocalDate.parse(it, dateFormatter)
                        YearMonth.from(d) == displayedMonth
                    }.getOrDefault(false)
                }
                val daysInMonth = displayedMonth.lengthOfMonth()
                val pct = if (daysInMonth == 0) 0 else (monthlyCount * 100) / daysInMonth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = Color(0xFF4CAF50),
                        title = "월간 출석체크",
                        value = "${monthlyCount}일"
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CalendarToday,
                        iconTint = Color(0xFF4CAF50),
                        title = "총 체크인 수",
                        value = "${totalCompletions}일"
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Percent,
                        iconTint = Color(0xFFE08030),
                        title = "월별 체크인 비율",
                        value = "${pct}%"
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Timeline,
                        iconTint = Color(0xFF4A90E2),
                        title = "연속",
                        value = "${currentStreak}일"
                    )
                }
            }

            // Habit log section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Text(
                    "${displayedMonth.monthValue}월에서 습관 로그",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "이번 달에 공유할 체크인 생각이 아직 없음",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    dateDialogDate?.let { date ->
        val key = "${habit.id}|${date.format(dateFormatter)}"
        val existing = logRecords[key]
        HabitLogDialog(
            date = date,
            initialCompleted = existing?.isCompleted ?: completedDates.contains(date.format(dateFormatter)),
            initialMood = existing?.mood ?: -1,
            initialNote = existing?.note ?: "",
            onDismiss = { dateDialogDate = null },
            onSave = { completed, mood, note ->
                onSaveLog(habit.id, date, completed, mood, note)
                dateDialogDate = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("습관 삭제", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("'${habit.name}'을(를) 삭제하시겠습니까? 삭제된 데이터는 복구할 수 없습니다.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                    onDismiss()
                }) { Text("삭제", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        onClick = onClick
    )
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(iconTint.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(title, color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}

@Composable
private fun MonthCalendarCard(
    yearMonth: YearMonth,
    completedDates: Set<String>,
    today: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggle: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${yearMonth.monthValue}월",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNext) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
        }
        Spacer(Modifier.height(4.dp))
        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { dow ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(dow, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // Grid
        val firstDay = yearMonth.atDay(1)
        val startOffset = firstDay.dayOfWeek.value % 7  // Sunday=0
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIdx = row * 7 + col
                    val day = cellIdx - startOffset + 1
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val date = yearMonth.atDay(day)
                            val isCompleted = completedDates.contains(date.format(dateFormatter))
                            val isToday = date == today
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        when {
                                            isCompleted -> Modifier.background(Color(0xFF4A90E2))
                                            isToday -> Modifier.border(1.5.dp, Color(0xFF4A90E2), CircleShape)
                                            else -> Modifier
                                        }
                                    )
                                    .clickable { onToggle(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.toString(),
                                    color = when {
                                        isCompleted -> Color.White
                                        isToday -> Color(0xFF4A90E2)
                                        date.isAfter(today) -> TextSecondary
                                        else -> MaterialTheme.colorScheme.onBackground
                                    },
                                    fontWeight = if (isToday || isCompleted) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Yearly heatmap view (= 더 보기)
// =====================================================================
@Composable
private fun HabitYearlyView(
    habit: Habit,
    completedDates: Set<String>,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSetDateRange: (String, String) -> Unit
) {
    var year by remember { mutableIntStateOf(LocalDate.now().year) }

    LaunchedEffect(year) {
        onSetDateRange("$year-01-01", "$year-12-31")
    }

    val yearDates = remember(completedDates, year) {
        completedDates.mapNotNull { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }
            .filter { it.year == year }
    }
    val yearCount = yearDates.size
    val totalDaysInYear = if (java.time.Year.of(year).isLeap) 366 else 365
    val pct = (yearCount * 100) / totalDaysInYear
    val bestStreak = computeLongestStreak(yearDates.sorted())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.weight(1f))
            Text("More", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "공유", tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                // Year nav
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = Color(0xFF4A90E2))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("$year", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { year++ }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF4A90E2))
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Habit header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HabitIconView(
                        iconKey = habit.icon,
                        colorHex = habit.color,
                        circleSize = 32.dp,
                        iconSize = 18.dp,
                        fallbackText = habit.name
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(habit.name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(12.dp))

                // Month labels row
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("1월", "4월", "7월", "10월").forEach { label ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            Text(label, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Heatmap grid: 53 weeks × 7 rows
                YearHeatmap(year = year, completedDates = completedDates, habitColor = habit.color)

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BigStat(value = yearCount.toString(), label = "Yearly check-ins", unit = "")
                    BigStat(value = bestStreak.toString(), label = "Best streak", unit = "")
                    BigStat(value = pct.toString(), label = "Check-in rate", unit = "%")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotBlank()) {
                Spacer(Modifier.width(2.dp))
                Text(unit, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun YearHeatmap(year: Int, completedDates: Set<String>, habitColor: String) {
    val baseColor = runCatching { Color(android.graphics.Color.parseColor(habitColor)) }
        .getOrDefault(Color(0xFF4A90E2))
    val emptyColor = Color(0xFF2A2A2A)

    val jan1 = LocalDate.of(year, 1, 1)
    val startDow = jan1.dayOfWeek.value % 7  // Sun=0
    val daysInYear = if (java.time.Year.of(year).isLeap) 366 else 365
    val totalCells = startDow + daysInYear
    val weeks = (totalCells + 6) / 7

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val cellWidth = maxWidth / weeks
        Column {
            for (dow in 0 until 7) {
                Row {
                    for (w in 0 until weeks) {
                        val cellIdx = w * 7 + dow
                        val dayOfYear = cellIdx - startDow + 1
                        val filled = if (dayOfYear in 1..daysInYear) {
                            val date = jan1.plusDays((dayOfYear - 1).toLong())
                            completedDates.contains(date.format(dateFormatter))
                        } else false
                        Box(
                            modifier = Modifier
                                .size(cellWidth)
                                .padding(0.3.dp)
                                .background(if (filled) baseColor else emptyColor)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// Share preview (= 공유)
// =====================================================================
@Composable
private fun HabitShareView(
    habit: Habit,
    completedDates: Set<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val habitColor = runCatching { Color(android.graphics.Color.parseColor(habit.color)) }
        .getOrDefault(Color(0xFF6B6ADE))
    val today = LocalDate.now()
    val streakDays = computeCurrentStreak(
        completedDates.mapNotNull { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }.sorted(),
        today
    )
    val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy. M. d."))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.weight(1f))
            Text("공유", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.weight(1f))

        // Share card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(habitColor)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HabitIconView(
                    iconKey = habit.icon,
                    colorHex = habit.color,
                    circleSize = 30.dp,
                    iconSize = 18.dp,
                    fallbackText = habit.name
                )
                Spacer(Modifier.width(8.dp))
                Text(habit.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TickTick", color = Color.White, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("나는 고집해 왔다", color = Color.White, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("을 위해 ", color = Color.White, fontSize = 18.sp)
                Text(streakDays.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(" 일", color = Color.White, fontSize = 18.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.4f)))
                Spacer(Modifier.width(8.dp))
                Text(todayStr, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.4f)))
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("BetterTick", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("자기관리적인 삶으로의 초대", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Share button row (native Intent)
        Button(
            onClick = {
                val text = "나는 '${habit.name}'을 위해 ${streakDays}일째 고집해 왔다. (${todayStr})"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "공유"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("공유하기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

// =====================================================================
// Stats helpers
// =====================================================================
private fun computeLongestStreak(sortedDates: List<LocalDate>): Int {
    if (sortedDates.isEmpty()) return 0
    var best = 1
    var cur = 1
    for (i in 1 until sortedDates.size) {
        cur = if (sortedDates[i] == sortedDates[i - 1].plusDays(1)) cur + 1 else 1
        if (cur > best) best = cur
    }
    return best
}

private fun computeCurrentStreak(sortedDates: List<LocalDate>, today: LocalDate): Int {
    if (sortedDates.isEmpty()) return 0
    val dateSet = sortedDates.toHashSet()
    var streak = 0
    var cursor = if (dateSet.contains(today)) today else today.minusDays(1)
    while (dateSet.contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

// =====================================================================
// Random illustration — 5 variants drawn via Canvas, seeded by habit id
// so the same habit always gets the same picture.
// =====================================================================
@Composable
fun HabitIllustration(seed: Int, accent: Color, modifier: Modifier = Modifier) {
    val variant = (seed.toLong() and Int.MAX_VALUE.toLong()).toInt() % 5
    androidx.compose.foundation.Canvas(modifier = modifier) {
        when (variant) {
            0 -> drawBlocks(this, accent)
            1 -> drawSpheres(this, accent)
            2 -> drawStack(this, accent)
            3 -> drawStars(this, accent)
            else -> drawDiamonds(this, accent)
        }
    }
}

private fun drawBlocks(scope: androidx.compose.ui.graphics.drawscope.DrawScope, accent: Color) = with(scope) {
    val s = size.minDimension
    val cx = size.width / 2
    val cy = size.height / 2 + s * 0.05f
    val u = s * 0.22f
    // three colored blocks with subtle shadow
    drawCircle(color = Color.Black.copy(alpha = 0.18f), radius = s * 0.38f, center = androidx.compose.ui.geometry.Offset(cx, cy + s * 0.3f))
    drawRoundRect(
        color = Color(0xFFE85050),
        topLeft = androidx.compose.ui.geometry.Offset(cx - u * 1.2f, cy - u * 0.2f),
        size = androidx.compose.ui.geometry.Size(u * 1.5f, u * 1.2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.15f, u * 0.15f)
    )
    drawRoundRect(
        color = Color(0xFFF5C542),
        topLeft = androidx.compose.ui.geometry.Offset(cx - u * 0.9f, cy - u * 1.3f),
        size = androidx.compose.ui.geometry.Size(u * 1.8f, u * 1.4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.15f, u * 0.15f)
    )
    drawRoundRect(
        color = Color(0xFF4CD267),
        topLeft = androidx.compose.ui.geometry.Offset(cx + u * 0.3f, cy - u * 0.8f),
        size = androidx.compose.ui.geometry.Size(u * 1.4f, u * 1.6f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.15f, u * 0.15f)
    )
    // studs (little circles on top of each block)
    val studR = u * 0.12f
    listOf(
        androidx.compose.ui.geometry.Offset(cx - u * 0.55f, cy - u * 1.2f),
        androidx.compose.ui.geometry.Offset(cx - u * 0.15f, cy - u * 1.2f),
        androidx.compose.ui.geometry.Offset(cx + u * 0.25f, cy - u * 1.2f),
    ).forEach { drawCircle(Color.White.copy(alpha = 0.6f), studR, it) }
}

private fun drawSpheres(scope: androidx.compose.ui.graphics.drawscope.DrawScope, accent: Color) = with(scope) {
    val s = size.minDimension
    val cx = size.width / 2
    val cy = size.height / 2
    val r = s * 0.14f
    val palette = listOf(Color(0xFFE85050), Color(0xFFF5C542), Color(0xFF4A90E2), Color(0xFF4CD267))
    palette.forEachIndexed { i, c ->
        val angle = i * (Math.PI * 2 / palette.size)
        val px = (cx + Math.cos(angle) * r * 2.0).toFloat()
        val py = (cy + Math.sin(angle) * r * 2.0).toFloat()
        drawCircle(c, radius = r, center = androidx.compose.ui.geometry.Offset(px, py))
        drawCircle(Color.White.copy(alpha = 0.35f), radius = r * 0.35f,
            center = androidx.compose.ui.geometry.Offset(px - r * 0.3f, py - r * 0.3f))
    }
    drawCircle(accent, radius = r * 1.3f, center = androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.35f), radius = r * 0.4f,
        center = androidx.compose.ui.geometry.Offset(cx - r * 0.35f, cy - r * 0.35f))
}

private fun drawStack(scope: androidx.compose.ui.graphics.drawscope.DrawScope, accent: Color) = with(scope) {
    val s = size.minDimension
    val cx = size.width / 2
    val bottomY = size.height / 2 + s * 0.3f
    val widths = listOf(s * 0.7f, s * 0.55f, s * 0.4f)
    val colors = listOf(Color(0xFF4A90E2), Color(0xFFF5A623), Color(0xFFE85050))
    val h = s * 0.14f
    widths.forEachIndexed { i, w ->
        drawRoundRect(
            color = colors[i],
            topLeft = androidx.compose.ui.geometry.Offset(cx - w / 2, bottomY - (i + 1) * h * 1.1f),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.4f, h * 0.4f)
        )
    }
    drawCircle(Color(0xFFFFD166), radius = s * 0.07f,
        center = androidx.compose.ui.geometry.Offset(cx, bottomY - 3 * h * 1.1f - s * 0.08f))
}

private fun drawStars(scope: androidx.compose.ui.graphics.drawscope.DrawScope, accent: Color) = with(scope) {
    val s = size.minDimension
    val cx = size.width / 2
    val cy = size.height / 2
    val palette = listOf(Color(0xFFE85050), Color(0xFFF5C542), Color(0xFF4CD267), Color(0xFF4A90E2), Color(0xFFB06AE0))
    palette.forEachIndexed { i, c ->
        val angle = i * (Math.PI * 2 / palette.size) - Math.PI / 2
        val d = s * 0.22f
        val px = (cx + Math.cos(angle) * d).toFloat()
        val py = (cy + Math.sin(angle) * d).toFloat()
        drawCircle(c, radius = s * 0.09f, center = androidx.compose.ui.geometry.Offset(px, py))
    }
    drawCircle(accent, radius = s * 0.12f, center = androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun drawDiamonds(scope: androidx.compose.ui.graphics.drawscope.DrawScope, accent: Color) = with(scope) {
    val s = size.minDimension
    val cx = size.width / 2
    val cy = size.height / 2
    val colors = listOf(Color(0xFFE85050), Color(0xFFF5C542), Color(0xFF4CD267), Color(0xFF4A90E2))
    colors.forEachIndexed { i, c ->
        val offsetX = (i - 1.5f) * s * 0.18f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx + offsetX, cy - s * 0.15f)
            lineTo(cx + offsetX + s * 0.09f, cy)
            lineTo(cx + offsetX, cy + s * 0.15f)
            lineTo(cx + offsetX - s * 0.09f, cy)
            close()
        }
        drawPath(path, c)
    }
}

// =====================================================================
// Log dialog — shown when user taps a date (or swipes left on a habit row)
// =====================================================================
@Composable
fun HabitLogDialog(
    date: LocalDate,
    initialCompleted: Boolean,
    initialMood: Int,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (isCompleted: Boolean, mood: Int, note: String) -> Unit,
    habitName: String? = null,
    habitIcon: String? = null,
    habitColor: String? = null
) {
    var completed by remember { mutableStateOf(initialCompleted) }
    var mood by remember { mutableStateOf(initialMood) }
    var note by remember { mutableStateOf(initialNote) }
    val headerFmt = DateTimeFormatter.ofPattern("E, M월 d", Locale.KOREAN)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (habitName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    HabitIconView(
                        iconKey = habitIcon.orEmpty(),
                        colorHex = habitColor ?: "#9B59B6",
                        circleSize = 28.dp,
                        iconSize = 16.dp,
                        fallbackText = habitName
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(habitName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                date.format(headerFmt),
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            // Status radios
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { completed = true }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(
                    selected = completed,
                    onClick = { completed = true },
                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Color(0xFF4A90E2))
                )
                Text("달성됨", color = MaterialTheme.colorScheme.onBackground)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { completed = false }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(
                    selected = !completed,
                    onClick = { completed = false },
                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Color(0xFF4A90E2))
                )
                Text("달성되지 않음", color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(Modifier.height(8.dp))

            // Mood emojis
            val emojis = listOf("😭", "☹️", "😐", "🙂", "😄")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                emojis.forEachIndexed { idx, emoji ->
                    val isSelected = mood == idx
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.Transparent else Color(0xFF3A3A3A))
                            .clickable { mood = if (mood == idx) -1 else idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            emoji,
                            fontSize = if (isSelected) 28.sp else 22.sp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Note field
            androidx.compose.material3.OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("무슨 생각하고 있어요?", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = Color(0xFF3A3A3A),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소", color = Color(0xFF4A90E2))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { onSave(completed, mood, note) }) {
                    Text("저장", color = Color(0xFF4A90E2))
                }
            }
        }
    }
}
