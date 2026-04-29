package com.bettertick.ui.screens.calendar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertick.ui.theme.Orange
import com.bettertick.ui.theme.TextSecondary
import com.bettertick.ui.theme.TextTertiary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate
import java.time.YearMonth

// File-scope constants — keeping these out of the per-frame render path
// matters because scroll over ~10 years otherwise allocates thousands of
// identical Color/TextStyle objects that just churn the GC.
private val DayHeaderLabels = listOf("S", "M", "T", "W", "T", "F", "S")
private val TaskCellBg = Orange.copy(alpha = 0.22f)
private val DayNumStyle = TextStyle(fontSize = 9.sp, color = Color.White)
private val DayNumTodayStyle = TextStyle(fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
private val DayHeaderStyle = TextStyle(fontSize = 8.sp, color = TextTertiary)

/**
 * Vertically scrolling year overview. Each LazyColumn item is a whole year
 * (label + 3×4 mini-month grid), so swiping vertically moves through years.
 * Tapping a month switches the calendar into MONTH mode focused on that
 * month.
 *
 * Each mini month is drawn as a single Canvas rather than ~50 nested
 * Box+Text widgets — this was the main fix for scroll lag once the range
 * grew to ±5 years.
 */
@Composable
fun YearView(
    years: List<Int>,
    initialYear: Int,
    lookup: TaskDateLookup,
    onMonthSelected: (YearMonth) -> Unit,
    onVisibleYearChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val initialIndex = remember(years, initialYear) {
        years.indexOf(initialYear).coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Report the year whose block occupies the most visible area — matches
    // how ScrollableMonthCalendar picks its dominant month.
    LaunchedEffect(listState, years) {
        snapshotFlow {
            val info = listState.layoutInfo
            val viewportStart = info.viewportStartOffset
            val viewportEnd = info.viewportEndOffset
            info.visibleItemsInfo
                .maxByOrNull { item ->
                    val top = item.offset
                    val bottom = item.offset + item.size
                    (minOf(bottom, viewportEnd) - maxOf(top, viewportStart))
                        .coerceAtLeast(0)
                }
                ?.index
                ?.let { years.getOrNull(it) }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { onVisibleYearChanged(it) }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberCalendarFlingBehavior(),
        modifier = modifier
    ) {
        items(years, key = { it }) { year ->
            YearBlock(
                year = year,
                today = today,
                lookup = lookup,
                onMonthSelected = onMonthSelected
            )
        }
    }
}

@Composable
private fun YearBlock(
    year: Int,
    today: LocalDate,
    lookup: TaskDateLookup,
    onMonthSelected: (YearMonth) -> Unit
) {
    val months = remember(year) { (1..12).map { YearMonth.of(year, it) } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "${year}년",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 3 cols × 4 rows. LazyVerticalGrid inside a LazyColumn is awkward
        // (nested scroll), so we lay out the grid manually with Rows.
        for (row in 0 until 4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    Box(modifier = Modifier.weight(1f)) {
                        MiniMonth(
                            month = months[idx],
                            today = today,
                            lookup = lookup,
                            onClick = { onMonthSelected(months[idx]) }
                        )
                    }
                }
            }
            if (row < 3) Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Single mini-month rendered in a Canvas. Layout math is remembered against
 * the month identity; text is measured once per unique day number using the
 * TextMeasurer's cache (so the 31 numerals are measured exactly 31 times
 * across the whole year view, not 31 × 12 × N-years-visible).
 */
@Composable
private fun MiniMonth(
    month: YearMonth,
    today: LocalDate,
    lookup: TaskDateLookup,
    onClick: () -> Unit
) {
    val layout = remember(month) { MiniMonthLayout.of(month) }
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "${month.monthValue}월",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )

        // Header row height + `weeks` × 15dp for the day grid. Canvas height
        // is fixed per month so LazyColumn can skip layout work on scroll.
        val gridHeight = (15.dp * (layout.weeks + 1))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
        ) {
            val cellW = size.width / 7f
            val cellH = 15.dp.toPx()
            val boxSize = 15.dp.toPx()
            val radius = 3.dp.toPx()
            val cornerRadius = CornerRadius(radius, radius)

            // Day-of-week header row.
            for (i in 0..6) {
                val result = textMeasurer.measure(DayHeaderLabels[i], DayHeaderStyle)
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(
                        x = i * cellW + (cellW - result.size.width) / 2f,
                        y = (cellH - result.size.height) / 2f
                    )
                )
            }

            // Day numbers.
            for (d in 1..layout.daysInMonth) {
                val idx = layout.startOffset + d - 1
                val col = idx % 7
                val row = idx / 7
                val cellX = col * cellW
                val cellY = cellH + row * cellH

                val date = month.atDay(d)
                val isToday = date == today
                val hasTasks = lookup.hasTasksOn(date)

                if (isToday || hasTasks) {
                    val bgColor: Color = if (isToday) Orange else TaskCellBg
                    drawRoundRect(
                        color = bgColor,
                        topLeft = Offset(
                            x = cellX + (cellW - boxSize) / 2f,
                            y = cellY
                        ),
                        size = Size(boxSize, boxSize),
                        cornerRadius = cornerRadius
                    )
                }

                val style = if (isToday) DayNumTodayStyle else DayNumStyle
                val result = textMeasurer.measure(d.toString(), style)
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(
                        x = cellX + (cellW - result.size.width) / 2f,
                        y = cellY + (cellH - result.size.height) / 2f
                    )
                )
            }
        }
    }
}

/**
 * Per-month grid geometry. Tiny, but pulling it out lets us `remember` the
 * value by month identity rather than recomputing the day-of-week offset
 * arithmetic on every recomposition.
 */
private data class MiniMonthLayout(
    val startOffset: Int, // Sun=0
    val daysInMonth: Int,
    val weeks: Int
) {
    companion object {
        fun of(month: YearMonth): MiniMonthLayout {
            val startOffset = month.atDay(1).dayOfWeek.value % 7
            val daysInMonth = month.lengthOfMonth()
            val weeks = (startOffset + daysInMonth + 6) / 7
            return MiniMonthLayout(startOffset, daysInMonth, weeks)
        }
    }
}
