package com.bettertick.ui.screens.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertick.data.model.FocusSession
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkCard
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AccentBlue = Color(0xFF4D8EFF)
private val CardBg = DarkCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusStatsScreen(
    onBack: () -> Unit,
    viewModel: FocusStatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "포커스 통계",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            item { TopMetricsGrid(stats) }
            item { FocusRecordCard(stats) }
            item { DetailsCard(stats) }
            item { RankCard(stats) }
            item { TrendCard(stats) }
            item { TimelineCard(stats) }
            item { PeakHourCard(stats) }
            item { YearlyGridCard(stats) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/* ---------- Top 2x2 metric grid ---------- */

@Composable
private fun TopMetricsGrid(stats: FocusStats) {
    val todayCount = stats.todaySessionCount()
    val todaySec = stats.todaySeconds()
    val yCount = stats.yesterdaySessionCount()
    val ySec = stats.yesterdaySeconds()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "오늘의 포모",
                subtitle = "어제 $yCount",
                value = todayCount.toString(),
                valueSuffix = null
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "오늘의 포커스(h)",
                subtitle = "어제 ${formatHm(ySec)}",
                value = "${todaySec / 3600}h ${(todaySec % 3600) / 60}m",
                valueSuffix = null,
                valueIsDuration = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "총 포모",
                subtitle = null,
                value = stats.totalSessionCount().toString(),
                valueSuffix = null
            )
            val total = stats.totalSeconds()
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "총 집중 기간",
                subtitle = null,
                value = "${total / 3600}h ${(total % 3600) / 60}m",
                valueSuffix = null,
                valueIsDuration = true
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    subtitle: String?,
    value: String,
    valueSuffix: String?,
    valueIsDuration: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
        if (subtitle != null) {
            Text(subtitle, color = TextTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = value,
            color = AccentBlue,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ---------- 집중 기록 ---------- */

@Composable
private fun FocusRecordCard(stats: FocusStats) {
    val recent = remember(stats) {
        stats.sessionsInWindow(StatsWindow.Year).take(5)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text("집중 기록", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (recent.isEmpty()) {
            Text("기록이 없습니다", color = TextTertiary, fontSize = 13.sp)
        } else {
            recent.forEachIndexed { idx, s ->
                SessionRow(s, stats.colorFor(s.activityName))
                if (idx < recent.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SessionRow(s: FocusSession, colorHex: String) {
    val dot = parseColorSafe(colorHex)
    val zone = java.time.ZoneId.systemDefault()
    val started = s.startedAt.toDate().toInstant().atZone(zone)
    val ended = s.endedAt?.toDate()?.toInstant()?.atZone(zone)
    val dateLabel = started.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
    val startLabel = started.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    val endLabel = ended?.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)) ?: "진행 중"
    val mins = s.durationSeconds / 60
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(dateLabel, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("$startLabel - $endLabel", color = TextTertiary, fontSize = 12.sp)
            }
            Text(s.activityName.ifBlank { "활동" }, color = TextSecondary, fontSize = 13.sp)
        }
        Text("${mins}m", color = TextSecondary, fontSize = 12.sp)
    }
}

/* ---------- 세부사항 (donut) ---------- */

@Composable
private fun DetailsCard(stats: FocusStats) {
    var window by remember { mutableStateOf(StatsWindow.Day) }
    val breakdown = remember(stats, window) { stats.categoryBreakdown(window) }
    val total = breakdown.sumOf { it.second }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("세부사항", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(windowLabel(window), color = AccentBlue, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        WindowTabs(selected = window, onSelect = { window = it })
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            if (total <= 0L) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = size.minDimension / 2 - 8f,
                            style = Stroke(width = 16f)
                        )
                    }
                    Text("데이터 없음", color = TextTertiary, fontSize = 13.sp)
                }
            } else {
                Donut(
                    segments = breakdown.map { (name, sec) -> parseColorSafe(stats.colorFor(name)) to sec },
                    centerText = formatHm(total)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("집중 순위", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("리스트", color = TextTertiary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (breakdown.isEmpty()) {
            Text("없음", color = TextTertiary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Donut(segments: List<Pair<Color, Long>>, centerText: String) {
    val total = segments.sumOf { it.second }.coerceAtLeast(1L)
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val stroke = 24f
            val diameter = size.minDimension - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            var start = -90f
            segments.forEach { (color, sec) ->
                val sweep = (sec.toFloat() / total) * 360f
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke)
                )
                start += sweep
            }
        }
        Text(centerText, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/* ---------- 집중 순위 ---------- */

@Composable
private fun RankCard(stats: FocusStats) {
    val rank = remember(stats) { stats.categoryBreakdown(StatsWindow.Year) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text("집중 순위", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (rank.isEmpty()) {
            Text("없음", color = TextTertiary, fontSize = 13.sp)
        } else {
            rank.take(8).forEach { (name, sec) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseColorSafe(stats.colorFor(name)))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(name, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(formatHm(sec), color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

/* ---------- 트렌드 ---------- */

@Composable
private fun TrendCard(stats: FocusStats) {
    var scope by remember { mutableStateOf(TrendScope.Week) }
    val values = remember(stats, scope) {
        when (scope) {
            TrendScope.Week -> stats.weeklySecondsByDay()
            TrendScope.Month -> monthlySecondsByDay(stats)
            TrendScope.Year -> yearlySecondsByMonth(stats)
        }
    }
    val labels = when (scope) {
        TrendScope.Week -> listOf("일", "월", "화", "수", "목", "금", "토")
        TrendScope.Month -> (1..values.size).map { it.toString() }
        TrendScope.Year -> (1..12).map { "${it}월" }
    }
    val avg = if (values.isNotEmpty()) values.average().toLong() else 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("트렌드", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                when (scope) {
                    TrendScope.Week -> "이번주"
                    TrendScope.Month -> "이번달"
                    TrendScope.Year -> "올해"
                },
                color = AccentBlue, fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TrendTab("주", scope == TrendScope.Week) { scope = TrendScope.Week }
            TrendTab("월", scope == TrendScope.Month) { scope = TrendScope.Month }
            TrendTab("년", scope == TrendScope.Year) { scope = TrendScope.Year }
        }
        Spacer(Modifier.height(16.dp))
        BarChart(values = values, labels = labels, valueToLabel = { formatHm(it) })
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (scope) {
                    TrendScope.Week -> "매일 평균"
                    TrendScope.Month -> "매일 평균"
                    TrendScope.Year -> "월 평균"
                },
                color = TextSecondary, fontSize = 13.sp
            )
            Spacer(Modifier.weight(1f))
            Text(formatHm(avg), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun TrendTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF1F2B45) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) AccentBlue else TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun BarChart(values: List<Long>, labels: List<String>, valueToLabel: (Long) -> String) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            values.forEach { v ->
                val frac = v.toFloat() / max.toFloat()
                val hDp = (frac * 110).toInt().coerceAtLeast(if (v > 0) 4 else 2)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(hDp.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (v > 0) AccentBlue else Color(0xFF3A3A3A))
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEach { l ->
                Text(
                    text = l,
                    color = TextTertiary,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/* ---------- 타임라인 ---------- */

@Composable
private fun TimelineCard(stats: FocusStats) {
    val today = LocalDate.now()
    val (weekStart, _) = stats.weekBounds(today)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("타임라인", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("이번주", color = AccentBlue, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Column(modifier = Modifier.width(40.dp)) {
                listOf("00:00", "06:00", "12:00", "18:00").forEach { t ->
                    Text(t, color = TextTertiary, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                }
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (0..6).forEach { i ->
                    val date = weekStart.plusDays(i.toLong())
                    val sessionsForDay = stats.sessionsOnDate(date)
                    DayColumn(
                        sessions = sessionsForDay,
                        stats = stats,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp)) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { l ->
                Text(l, color = TextTertiary, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun DayColumn(sessions: List<FocusSession>, stats: FocusStats, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2A2A2C))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val zone = java.time.ZoneId.systemDefault()
            val dayH = size.height
            sessions.forEach { s ->
                val zdt = s.startedAt.toDate().toInstant().atZone(zone)
                val startSec = zdt.toLocalTime().toSecondOfDay()
                val durSec = s.durationSeconds.coerceAtMost(86400L - startSec)
                val top = (startSec / 86400f) * dayH
                val h = (durSec / 86400f) * dayH
                drawRect(
                    color = parseColorSafe(stats.colorFor(s.activityName)),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, h.coerceAtLeast(2f))
                )
            }
        }
    }
}

/* ---------- 최고 집중 시간 ---------- */

@Composable
private fun PeakHourCard(stats: FocusStats) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val hourly = remember(stats, month) {
        stats.peakHourSecondsForMonth(month.atDay(1))
    }
    val maxVal = hourly.maxOrNull() ?: 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("최고 집중 시간", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = AccentBlue)
            }
            Text("${month.monthValue}월", color = AccentBlue, fontSize = 13.sp)
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = AccentBlue)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = 24
                val gap = 2f
                val barWidth = (size.width - gap * (n - 1)) / n
                val maxHeight = size.height - 16f
                val scale = if (maxVal > 0) maxHeight / maxVal.toFloat() else 0f
                for (i in 0 until n) {
                    val v = hourly[i]
                    val h = (v * scale).coerceAtLeast(if (v > 0) 4f else 2f)
                    drawRect(
                        color = if (v > 0) AccentBlue else Color(0xFF3A3A3A),
                        topLeft = Offset(i * (barWidth + gap), size.height - h),
                        size = Size(barWidth, h)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00").forEach { l ->
                Text(l, color = TextTertiary, fontSize = 10.sp)
            }
        }
    }
}

/* ---------- 연간 그리드 ---------- */

@Composable
private fun YearlyGridCard(stats: FocusStats) {
    var year by remember { mutableStateOf(LocalDate.now().year) }
    val byDate = remember(stats, year) { stats.yearlySecondsByDate(year) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("연간 그리드", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { year-- }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = AccentBlue)
            }
            Text("$year", color = AccentBlue, fontSize = 13.sp)
            IconButton(onClick = { year++ }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = AccentBlue)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("1월", "4월", "7월", "10월").forEach {
                Text(it, color = TextTertiary, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rows = 7
                val firstDay = LocalDate.of(year, 1, 1)
                val leadOffset = firstDay.dayOfWeek.value % 7 // Sun=0
                val daysInYear = if (firstDay.isLeapYear) 366 else 365
                val cells = leadOffset + daysInYear
                val cols = (cells + rows - 1) / rows
                val gap = 2f
                val cellW = (size.width - gap * (cols - 1)) / cols
                val cellH = (size.height - gap * (rows - 1)) / rows
                for (i in 0 until daysInYear) {
                    val date = firstDay.plusDays(i.toLong())
                    val idx = leadOffset + i
                    val col = idx / rows
                    val row = idx % rows
                    val sec = byDate[date] ?: 0L
                    val color = heatColor(sec)
                    drawRect(
                        color = color,
                        topLeft = Offset(col * (cellW + gap), row * (cellH + gap)),
                        size = Size(cellW, cellH)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendSwatch(heatColor(0L), "0m")
            LegendSwatch(heatColor(1800L), "0-1h")
            LegendSwatch(heatColor(7200L), "1h-3h")
            LegendSwatch(heatColor(14400L), "3h-5h")
            LegendSwatch(heatColor(20000L), ">5h")
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextTertiary, fontSize = 10.sp)
    }
}

private fun heatColor(sec: Long): Color = when {
    sec <= 0 -> Color(0xFF2A2A2C)
    sec < 3600 -> Color(0xFFC7D6F7)
    sec < 3 * 3600 -> Color(0xFF7FA3E8)
    sec < 5 * 3600 -> Color(0xFF4D7FD9)
    else -> Color(0xFF1F5ACC)
}

/* ---------- shared ---------- */

@Composable
private fun WindowTabs(selected: StatsWindow, onSelect: (StatsWindow) -> Unit) {
    val items = listOf(StatsWindow.Day to "날", StatsWindow.Week to "주", StatsWindow.Month to "월", StatsWindow.Year to "맞춤 설정")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (w, label) ->
            TrendTab(label = label, selected = w == selected) { onSelect(w) }
        }
    }
}

private fun windowLabel(w: StatsWindow): String = when (w) {
    StatsWindow.Day -> "오늘"
    StatsWindow.Week -> "이번주"
    StatsWindow.Month -> "이번달"
    StatsWindow.Year -> "올해"
}

private fun formatHm(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h <= 0 && m <= 0 -> "0m"
        h <= 0 -> "${m}m"
        m <= 0 -> "${h}h"
        else -> "${h}h${m}m"
    }
}

private fun parseColorSafe(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Orange)

private enum class TrendScope { Week, Month, Year }

private fun monthlySecondsByDay(stats: FocusStats): List<Long> {
    val today = LocalDate.now()
    val ym = YearMonth.of(today.year, today.month)
    val days = ym.lengthOfMonth()
    val byDate = stats.sessionsInWindow(StatsWindow.Month).groupBy {
        it.startedAt.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }.mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
    return (1..days).map { d -> byDate[LocalDate.of(today.year, today.month, d)] ?: 0L }
}

private fun yearlySecondsByMonth(stats: FocusStats): List<Long> {
    val year = LocalDate.now().year
    val buckets = LongArray(12)
    stats.sessionsInWindow(StatsWindow.Year).forEach { s ->
        val zdt = s.startedAt.toDate().toInstant().atZone(java.time.ZoneId.systemDefault())
        if (zdt.year == year) buckets[zdt.monthValue - 1] += s.durationSeconds
    }
    return buckets.toList()
}
