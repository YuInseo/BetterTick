package com.bettertick.widget.focus

import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bettertick.MainActivity
import com.bettertick.data.model.FocusSession
import com.bettertick.widget.WidgetServiceLocator
import com.bettertick.widget.theme.WidgetColors
import com.bettertick.widget.theme.WidgetColorValues
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/**
 * Stacked-bar widget showing this week's focus minutes by category, plus
 * Today/Week totals. Tapping the widget opens the app.
 */
class FocusDistributionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.i("FocusDistWidget", "provideGlance start id=$id")
        val cached = WidgetServiceLocator.focusWeekCache.value
        val sessions = if (cached.isNotEmpty()) cached else
            WidgetServiceLocator.safeLoad(emptyList()) {
                WidgetServiceLocator.focusRepository.observeThisWeekSessions().first()
            }

        provideContent { Content(sessions) }
    }

    @androidx.compose.runtime.Composable
    private fun Content(sessions: List<FocusSession>) {
        val today = LocalDate.now()
        val weekStart = today.with(java.time.DayOfWeek.SUNDAY)
            .let { sun -> if (sun.isAfter(today)) sun.minusWeeks(1) else sun }
        val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
        val perDay = weekDates.map { d -> bucketsForDate(sessions, d) }
        val todaySeconds = bucketsForDate(sessions, today).values.sum()
        val weekSeconds = perDay.sumOf { it.values.sum() }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1C1C1E)))
                .cornerRadius(20.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                TotalsRow(todaySeconds, weekSeconds)
                Spacer(GlanceModifier.height(8.dp))
                ChartRow(weekDates, perDay, today)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TotalsRow(todaySeconds: Long, weekSeconds: Long) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Today",
                    style = TextStyle(
                        color = WidgetColors.textTertiary,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = formatDuration(todaySeconds),
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Week",
                    style = TextStyle(
                        color = WidgetColors.textTertiary,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = formatDuration(weekSeconds),
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ChartRow(
        weekDates: List<LocalDate>,
        perDay: List<Map<String, Long>>,
        today: LocalDate
    ) {
        // Cap the bar height to the largest day's seconds (so empty weeks
        // still draw at min height and busy weeks fill the chart area).
        val maxSeconds = max(perDay.maxOfOrNull { it.values.sum() } ?: 1L, 60L)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            weekDates.forEachIndexed { index, date ->
                DayBar(
                    label = dayLabel(index),
                    isToday = date == today,
                    buckets = perDay[index],
                    maxSeconds = maxSeconds,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DayBar(
        label: String,
        isToday: Boolean,
        buckets: Map<String, Long>,
        maxSeconds: Long,
        modifier: GlanceModifier
    ) {
        val total = buckets.values.sum()
        // Visual scale: 0..1 of available bar area.
        val scale = if (maxSeconds > 0) total.toFloat() / maxSeconds.toFloat() else 0f
        val barFractionDp = (scale * 100).toInt()
        Column(
            modifier = modifier.padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer at top forces bars to bottom-align — Glance lacks a
            // direct gravity option for column children mid-axis, so we do
            // it by giving the bar a fixed-ish height and the spacer the
            // remaining flex weight.
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                StackedBar(
                    buckets = buckets,
                    fractionPct = barFractionDp
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    color = if (isToday) WidgetColors.textPrimary else WidgetColors.textTertiary,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun StackedBar(buckets: Map<String, Long>, fractionPct: Int) {
        // fractionPct is 0..100 — translates to a height of 0..80dp so each
        // bar respects the chart area on a 4x2 widget.
        val totalDp = (fractionPct.coerceIn(0, 100) * 80) / 100
        val totalSeconds = buckets.values.sum().coerceAtLeast(1L)
        // Sort largest segment to the bottom for visual stability.
        val ordered = buckets.entries.sortedByDescending { it.value }
        Column(
            modifier = GlanceModifier
                .width(14.dp)
                .height(max(totalDp, 2).dp)
                .background(ColorProvider(Color(0xFF2A2A2C)))
                .cornerRadius(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ordered.forEach { (colorHex, seconds) ->
                val segDp = ((seconds.toDouble() / totalSeconds) * totalDp).toInt()
                if (segDp > 0) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(segDp.dp)
                            .background(ColorProvider(parseColor(colorHex)))
                    ) {}
                }
            }
        }
    }

    /** Group seconds by activityColor (acts as our category key). */
    private fun bucketsForDate(
        sessions: List<FocusSession>,
        date: LocalDate
    ): Map<String, Long> {
        val zone = ZoneId.systemDefault()
        return sessions
            .filter { s ->
                val started = s.startedAt.toDate().toInstant().atZone(zone).toLocalDate()
                started == date && s.durationSeconds > 0
            }
            .groupBy { it.activityColor.ifBlank { "#FF8C00" } }
            .mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
    }

    private fun parseColor(hex: String): Color =
        runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(WidgetColorValues.accent)

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return when {
            hours <= 0 && mins <= 0 -> "0m"
            hours <= 0 -> "${mins}m"
            mins <= 0 -> "${hours}h"
            else -> "${hours}h${mins}m"
        }
    }

    private fun dayLabel(index: Int): String = when (index) {
        0 -> "S"; 1 -> "M"; 2 -> "T"; 3 -> "W"
        4 -> "T"; 5 -> "F"; 6 -> "S"
        else -> ""
    }
}
