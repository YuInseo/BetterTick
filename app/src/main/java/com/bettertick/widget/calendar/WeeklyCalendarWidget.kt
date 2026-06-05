package com.bettertick.widget.calendar

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityWithIntent
import com.bettertick.overlay.QuickOverlayReceiver
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import com.bettertick.QuickAddActivity
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.occursOn
import com.bettertick.widget.WidgetServiceLocator
import com.bettertick.widget.theme.WidgetColors
import com.bettertick.widget.theme.WidgetColorValues
import com.bettertick.widget.util.WidgetDateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Weekly calendar widget — Samsung-Calendar-style row of 7 day columns with
 * up to 3 task chips per day, color-coded by list color. Tapping any chip,
 * day, or background opens the main app.
 */
class WeeklyCalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.i("WeeklyCalendarWidget", "provideGlance start id=$id")
        val cachedTasks = WidgetServiceLocator.tasksCache.value
        val tasks = if (cachedTasks.isNotEmpty()) cachedTasks else
            WidgetServiceLocator.safeLoad(emptyList()) {
                WidgetServiceLocator.taskRepository.observeAllTasks().first()
            }
        val cachedLists = WidgetServiceLocator.listsCache.value
        val lists = if (cachedLists.isNotEmpty()) cachedLists else
            WidgetServiceLocator.safeLoad(emptyList<TaskList>()) {
                WidgetServiceLocator.listRepository.observeLists().first()
            }
        val listById = lists.associateBy { it.id }

        provideContent {
            val state = currentState<Preferences>()
            val today = WidgetDateUtils.today()
            val anchorDate = state[ANCHOR_DATE_KEY]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: today
            val weekDates = WidgetDateUtils.weekDates(anchorDate)
            val tasksByDate = weekDates.associateWith { d ->
                tasks
                    .filter { (!it.isCompleted && !it.isAbandoned) && it.occursOn(d) }
                    .take(3)
            }
            CalendarContent(today, anchorDate, weekDates, tasksByDate, listById)
        }
    }

    companion object {
        val ANCHOR_DATE_KEY = stringPreferencesKey("weekly_anchor_date")
    }

    @androidx.compose.runtime.Composable
    private fun CalendarContent(
        today: LocalDate,
        anchorDate: LocalDate,
        weekDates: List<LocalDate>,
        tasksByDate: Map<LocalDate, List<Task>>,
        listById: Map<String, TaskList>
    ) {
        val context = LocalContext.current
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
                    .padding(10.dp)
            ) {
                HeaderRow(anchorDate)
                Spacer(GlanceModifier.height(6.dp))
                DayOfWeekRow()
                Spacer(GlanceModifier.height(2.dp))
                WeekRow(today, weekDates, tasksByDate, listById)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun HeaderRow(anchorDate: LocalDate) {
        val context = LocalContext.current
        val prevWeek = anchorDate.minusWeeks(1)
        val nextWeek = anchorDate.plusWeeks(1)
        val addIntent = Intent(QuickOverlayReceiver.ACTION_QUICK_ADD).apply {
            setClass(context, QuickOverlayReceiver::class.java)
        }
        val monthName = monthShortName(anchorDate.monthValue)

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(28.dp)
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = anchorDate.monthValue.toString(),
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .width(28.dp)
                            .height(28.dp)
                            .clickable(
                                actionStartActivityWithIntent(
                                    setAnchorIntent(context, prevWeek)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹",
                            style = TextStyle(
                                color = WidgetColors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = monthName,
                        style = TextStyle(
                            color = WidgetColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Box(
                        modifier = GlanceModifier
                            .width(28.dp)
                            .height(28.dp)
                            .clickable(
                                actionStartActivityWithIntent(
                                    setAnchorIntent(context, nextWeek)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            style = TextStyle(
                                color = WidgetColors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .width(28.dp)
                        .height(28.dp)
                        .clickable(actionSendBroadcast(addIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = WidgetColors.textPrimary,
                            fontSize = 20.sp
                        )
                    )
                }
                Box(
                    modifier = GlanceModifier
                        .width(20.dp)
                        .height(28.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⋮",
                        style = TextStyle(
                            color = WidgetColors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DayOfWeekRow() {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            for (i in 0..6) {
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = WidgetDateUtils.dayOfWeekKoreanSunFirst(i),
                        style = TextStyle(
                            color = WidgetColors.textTertiary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WeekRow(
        today: LocalDate,
        weekDates: List<LocalDate>,
        tasksByDate: Map<LocalDate, List<Task>>,
        listById: Map<String, TaskList>
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            weekDates.forEach { date ->
                DayCell(
                    date = date,
                    isToday = date == today,
                    tasks = tasksByDate[date].orEmpty(),
                    listById = listById,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DayCell(
        date: LocalDate,
        isToday: Boolean,
        tasks: List<Task>,
        listById: Map<String, TaskList>,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier.padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = GlanceModifier
                    .width(20.dp)
                    .height(20.dp)
                    .let {
                        if (isToday) it
                            .background(ColorProvider(WidgetColorValues.blue))
                            .cornerRadius(10.dp)
                        else it
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
            Spacer(GlanceModifier.height(2.dp))
            tasks.forEach { task ->
                TaskChip(task, listById)
                Spacer(GlanceModifier.height(2.dp))
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TaskChip(task: Task, listById: Map<String, TaskList>) {
        val list = listById[task.listId]
        val color = parseColor(list?.color, fallback = WidgetColorValues.blue)
        val timeLabel = formatTimeRange(task)
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(color))
                .cornerRadius(3.dp)
                .padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            Text(
                text = task.title,
                style = TextStyle(
                    color = WidgetColors.textPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            if (timeLabel != null) {
                Text(
                    text = timeLabel,
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1
                )
            }
        }
    }

    /** Korean "오후4:00-5:00" style range for timed tasks; null for all-day
     *  (midnight dueDate) so the chip stays compact. End period is dropped
     *  when start/end share the same AM/PM to save horizontal space. */
    private fun formatTimeRange(task: Task): String? {
        val start = task.dueDate?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: return null
        if (start == LocalTime.MIDNIGHT) return null
        val dur = task.durationMinutes
        val end = if (dur > 0) start.plusMinutes(dur.toLong()) else start
        val startLabel = koreanClock(start)
        if (dur <= 0) return startLabel
        val samePeriod = (start.hour < 12) == (end.hour < 12)
        val endLabel = if (samePeriod) shortClock(end) else koreanClock(end)
        return "$startLabel-$endLabel"
    }

    private fun koreanClock(t: LocalTime): String {
        val period = if (t.hour < 12) "오전" else "오후"
        val h = ((t.hour + 11) % 12) + 1
        return "$period$h:%02d".format(t.minute)
    }

    private fun shortClock(t: LocalTime): String {
        val h = ((t.hour + 11) % 12) + 1
        return "$h:%02d".format(t.minute)
    }

    private fun parseColor(hex: String?, fallback: Color): Color {
        if (hex.isNullOrBlank()) return fallback
        return runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(fallback)
    }

    private fun monthShortName(month: Int): String {
        return when (month) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
            5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
            9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> ""
        }
    }

    private fun setAnchorIntent(context: Context, date: LocalDate): Intent =
        Intent(context, WeeklyCalendarActionDispatcher::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            data = Uri.parse("bettertick://weeklycalendar/anchor/$date")
        }
}
