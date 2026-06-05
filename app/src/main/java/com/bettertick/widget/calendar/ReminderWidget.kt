package com.bettertick.widget.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityWithIntent
import com.bettertick.overlay.QuickOverlayReceiver
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bettertick.MainActivity
import com.bettertick.QuickAddActivity
import com.bettertick.R
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.widget.WidgetServiceLocator
import com.bettertick.widget.theme.WidgetColors
import com.bettertick.widget.theme.WidgetColorValues
import com.bettertick.data.model.occursOn
import com.bettertick.widget.util.WidgetDateUtils
import com.bettertick.widget.util.WidgetDateUtils.localDueDate
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Samsung Reminder-style widget: week strip with event dots + today's task list.
 * Each task row shows a checkbox, title, time info, and list name.
 */
class ReminderWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.i("ReminderWidget", "provideGlance start id=$id")
        // Read from the hot cache kept in sync by BetterTickApplication. If
        // the cache is still empty (e.g. widget rendered before auth/Firestore
        // warmed up), fall back to a one-shot cold read so the first paint
        // after a cold boot still shows real data.
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
            // Re-read everything settings-related on each composition.
            // Glance's update() does NOT re-invoke provideGlance on Glance
            // 1.1 — only the composition re-runs. Keeping the read inside
            // the composable means every SETTINGS_VERSION_KEY bump picks
            // up the fresh SharedPreferences.
            val ctxInside = LocalContext.current
            val settings = WidgetServiceLocator.reminderSettings(ctxInside)
            val state = currentState<Preferences>()
            val today = WidgetDateUtils.today()
            val selectedDate = state[SELECTED_DATE_KEY]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: today
            val menuOpen = state[MENU_OPEN_KEY] ?: false
            Log.i(
                "ReminderWidget",
                "render selectedDate=$selectedDate menuOpen=$menuOpen " +
                    "opacity=${settings.opacity} showCompleted=${settings.showCompleted} showDetail=${settings.showDetail}"
            )

            val weekDates = WidgetDateUtils.weekDates(selectedDate)
            // `occursOn` expands each task's repeat rule so the dot/filter
            // reflects every date the task applies to — not just the due
            // date. Daily/weekly/monthly/yearly/weekdays all handled.
            val datesWithEvents = weekDates.associateWith { d ->
                tasks.any {
                    (settings.showCompleted || (!it.isCompleted && !it.isAbandoned)) && it.occursOn(d)
                }
            }
            val dayTasks = tasks
                .filter {
                    it.occursOn(selectedDate) &&
                        (settings.showCompleted || (!it.isCompleted && !it.isAbandoned))
                }
                // Repeating first, then by due time — matches the in-app
                // calendar panel's ordering so widget and app stay in sync.
                .sortedWith(
                    compareByDescending<Task> { !it.repeatRule.isNullOrBlank() }
                        .thenBy { it.dueDate?.toDate()?.time ?: Long.MAX_VALUE }
                )

            ReminderContent(today, selectedDate, weekDates, datesWithEvents, dayTasks, listById, settings, menuOpen)
        }
    }

    companion object {
        // Glance state keys, shared with WidgetActionDispatcher. These live
        // in Glance's own per-widget Preferences datastore — Glance's change
        // detection tracks them and triggers re-composition on write.
        val SELECTED_DATE_KEY = stringPreferencesKey("selected_date")
        val MENU_OPEN_KEY = booleanPreferencesKey("menu_open")
    }

    @androidx.compose.runtime.Composable
    private fun ReminderContent(
        today: LocalDate,
        selectedDate: LocalDate,
        weekDates: List<LocalDate>,
        datesWithEvents: Map<LocalDate, Boolean>,
        tasks: List<Task>,
        listById: Map<String, TaskList>,
        settings: WidgetServiceLocator.ReminderSettings,
        menuOpen: Boolean
    ) {
        val baseBg = Color(0xFF1C1C1E).copy(alpha = (settings.opacity / 100f).coerceIn(0f, 1f))
        val context = LocalContext.current
        // Outer Box stacks the menu overlay on top of the main Column.
        // NOTE: do not set `contentAlignment` on this Box — Glance applies
        // that alignment to every child including the fillMaxSize content
        // Column, which can cause the inner Row width to shrink and drop
        // the last columns of the week strip.
        // Outer clickable opens the app for any tap that isn't claimed by a
        // child clickable (header buttons, date cells, task rows, etc.).
        // In Glance, child clickables override the parent's, so the inner
        // controls keep their dedicated actions.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(baseBg))
                .cornerRadius(20.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp)
            ) {
                // Horizontal padding is applied per-section so the LazyColumn
                // below can reach the widget's right edge — otherwise its
                // ListView scrollbar ends up ~12dp inside the widget chrome.
                Column(modifier = GlanceModifier.padding(horizontal = 12.dp)) {
                    HeaderRow(today, selectedDate)
                    Spacer(GlanceModifier.height(8.dp))
                    DayOfWeekRow()
                    Spacer(GlanceModifier.height(4.dp))
                    DatesRow(today, selectedDate, weekDates)
                    Spacer(GlanceModifier.height(4.dp))
                    DotsRow(weekDates, datesWithEvents)
                    Spacer(GlanceModifier.height(8.dp))
                }

                if (tasks.isEmpty()) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 12.dp)
                            .clickable(actionStartActivity<MainActivity>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "할일이 없어요",
                            style = TextStyle(
                                color = WidgetColors.textSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                            Column(
                                modifier = GlanceModifier
                                    .padding(horizontal = 12.dp)
                                    .clickable(actionStartActivity<MainActivity>())
                            ) {
                                TaskRow(task, listById, settings.showDetail)
                                Spacer(GlanceModifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // Overflow menu overlay — shown only when the user taps ⋮.
            // Wrapped in its own fillMaxSize Box with TopEnd alignment so
            // the menu positions itself without affecting the main Column's
            // available width (see Box alignment caveat above).
            if (menuOpen) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Column(
                        modifier = GlanceModifier
                            .padding(top = 44.dp, end = 10.dp)
                            .background(ColorProvider(Color(0xFF2C2C2E)))
                            .cornerRadius(14.dp)
                    ) {
                        MenuItem(
                            label = "새로 고침",
                            intent = dispatcherIntent(
                                context,
                                WidgetActionDispatcher.ACTION_REFRESH
                            )
                        )
                        MenuItem(
                            label = "설정",
                            intent = dispatcherIntent(
                                context,
                                WidgetActionDispatcher.ACTION_OPEN_SETTINGS
                            )
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MenuItem(label: String, intent: Intent) {
        Box(
            modifier = GlanceModifier
                .clickable(actionStartActivityWithIntent(intent))
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = WidgetColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun HeaderRow(today: LocalDate, selectedDate: LocalDate) {
        val context = LocalContext.current
        val prevWeek = selectedDate.minusWeeks(1)
        val nextWeek = selectedDate.plusWeeks(1)
        val addIntent = Intent(QuickOverlayReceiver.ACTION_QUICK_ADD).apply {
            setClass(context, QuickOverlayReceiver::class.java)
        }
        val menuToggleIntent = dispatcherIntent(
            context,
            WidgetActionDispatcher.ACTION_TOGGLE_MENU
        )
        // Tapping the badge always jumps back to today — that's its whole
        // purpose on Samsung's Reminder widget.
        val jumpToTodayIntent = selectDateIntent(context, today)

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left flex region — AppBadge left-aligned. Equal weight with the
            // right flex region keeps the `‹ 4월 ›` group truly centered
            // despite the right side (+/⋮) being wider than the badge.
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                AppBadge(today, jumpToTodayIntent)
            }

            // Center group — `‹ 4월 ›` with clickable arrows.
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(actionStartActivityWithIntent(selectDateIntent(context, prevWeek))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                text = "${selectedDate.monthValue}월",
                style = TextStyle(
                    color = WidgetColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(actionStartActivityWithIntent(selectDateIntent(context, nextWeek))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Right flex region — + and ⋮ right-aligned.
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .clickable(actionSendBroadcast(addIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(
                                color = WidgetColors.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .clickable(actionStartActivityWithIntent(menuToggleIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⋮",
                            style = TextStyle(
                                color = WidgetColors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun AppBadge(today: LocalDate, onTapIntent: Intent) {
        // Outlined rounded-square badge that always shows TODAY's day-of-
        // month (unrelated to the selected date). Tapping it jumps the
        // week strip back to today — Samsung Reminder widget parity.
        Box(
            modifier = GlanceModifier
                .size(24.dp)
                .background(ColorProvider(WidgetColorValues.textPrimary.copy(alpha = 0.15f)))
                .cornerRadius(6.dp)
                .clickable(actionStartActivityWithIntent(onTapIntent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = today.dayOfMonth.toString(),
                style = TextStyle(
                    color = WidgetColors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
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
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DatesRow(today: LocalDate, selectedDate: LocalDate, weekDates: List<LocalDate>) {
        val context = LocalContext.current
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            weekDates.forEach { date ->
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(32.dp)
                        .clickable(actionStartActivityWithIntent(selectDateIntent(context, date))),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        date == selectedDate -> {
                            // Filled blue circle for the chosen date
                            Box(
                                modifier = GlanceModifier
                                    .size(28.dp)
                                    .background(ColorProvider(WidgetColorValues.blue))
                                    .cornerRadius(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = TextStyle(
                                        color = WidgetColors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        date == today -> {
                            // Today (but not selected) — blue text, no background
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = TextStyle(
                                    color = ColorProvider(WidgetColorValues.blue),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        else -> {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = TextStyle(
                                    color = WidgetColors.textPrimary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DotsRow(
        weekDates: List<LocalDate>,
        datesWithEvents: Map<LocalDate, Boolean>
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            weekDates.forEach { date ->
                Box(
                    modifier = GlanceModifier.defaultWeight().height(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (datesWithEvents[date] == true) {
                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .background(ColorProvider(WidgetColorValues.blue))
                                .cornerRadius(3.dp)
                        ) {}
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TaskRow(
        task: Task,
        listById: Map<String, TaskList>,
        showDetail: Boolean
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Outlined square checkbox (ring effect via nested Box)
            Box(
                modifier = GlanceModifier
                    .size(20.dp)
                    .background(ColorProvider(WidgetColorValues.textTertiary.copy(alpha = 0.8f)))
                    .cornerRadius(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(16.dp)
                        .background(ColorProvider(Color(0xFF1C1C1E)))
                        .cornerRadius(4.dp)
                ) {}
            }
            Spacer(GlanceModifier.width(10.dp))

            // Title + optional time subtitle
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        color = WidgetColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                if (showDetail) {
                    val subtitle = buildTaskSubtitle(task)
                    if (subtitle != null) {
                        Spacer(GlanceModifier.height(2.dp))
                        val isOverdue = task.isOverdueNow()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = subtitle,
                                style = TextStyle(
                                    color = ColorProvider(
                                        if (isOverdue) WidgetColorValues.red
                                        else WidgetColorValues.accent
                                    ),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                            // Alarm icon — shown when the task has a specific
                            // time-of-day (dueDate != midnight).
                            if (task.hasAlarm()) {
                                Spacer(GlanceModifier.width(6.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_alarm),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(12.dp),
                                    colorFilter = ColorFilter.tint(WidgetColors.textTertiary)
                                )
                            }
                            // Repeat icon — shown when repeatRule is set.
                            if (!task.repeatRule.isNullOrBlank()) {
                                Spacer(GlanceModifier.width(4.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_repeat),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(12.dp),
                                    colorFilter = ColorFilter.tint(WidgetColors.textTertiary)
                                )
                            }
                        }
                    }
                }
            }

            if (showDetail) {
                Spacer(GlanceModifier.width(8.dp))
                val listName = listById[task.listId]?.name ?: "기본함"
                Text(
                    text = listName,
                    style = TextStyle(
                        color = WidgetColors.textTertiary,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }

    /**
     * Intent targeting [WidgetActionDispatcher] for a date cell. Action +
     * payload live inside the data URI path — Glance's PendingIntent
     * pipeline proved unreliable at forwarding extras, but the URI always
     * survives. PendingIntent identity is (component, action, data), so
     * each per-date URI gets its own pending intent.
     */
    private fun selectDateIntent(context: Context, date: LocalDate): Intent =
        Intent(context, WidgetActionDispatcher::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            data = Uri.parse("bettertick://widget/selectdate/$date")
        }

    /**
     * Intent targeting [WidgetActionDispatcher] for a named action other
     * than date selection. Data URI is keyed on the action name so each has
     * its own PendingIntent.
     */
    private fun dispatcherIntent(context: Context, action: String): Intent =
        Intent(context, WidgetActionDispatcher::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            data = Uri.parse("bettertick://widget/action/$action")
        }

    /**
     * True when the task has a specific time-of-day (i.e. not just a
     * midnight all-day marker). The alarm icon mirrors this.
     */
    private fun Task.hasAlarm(): Boolean {
        val dueDate = this.dueDate ?: return false
        val dt = dueDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        return dt.hour != 0 || dt.minute != 0
    }

    /**
     * True when the task's due date is strictly in the past and it is
     * neither completed nor abandoned — matches the app's red-subtitle rule.
     * Recurring tasks are excluded: their stored due date is just the series
     * start, so showing red there misrepresents an actively-recurring task.
     */
    private fun Task.isOverdueNow(): Boolean {
        if (isCompleted || isAbandoned) return false
        if (!repeatRule.isNullOrBlank()) return false
        val due = dueDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
            ?: return false
        return due.isBefore(LocalDate.now())
    }

    /**
     * Build subtitle like "4월 20일", "4월 20일, 오후 4:00", or
     * "4월 20일, 오후 4:00 - 오후 5:00". Emoji suffixes dropped — the
     * alarm/repeat icons are rendered separately by TaskRow.
     */
    private fun buildTaskSubtitle(task: Task): String? {
        val dueDate = task.dueDate ?: return null
        val startDate = dueDate.toDate()
        val startDt = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        val dateLabel = "${startDt.monthValue}월 ${startDt.dayOfMonth}일"
        if (startDt.hour == 0 && startDt.minute == 0) return dateLabel

        val timeFormatter = SimpleDateFormat("a h:mm", Locale.KOREAN)
        val startLabel = timeFormatter.format(startDate)
        val dur = task.durationMinutes
        if (dur <= 0) return "$dateLabel, $startLabel"

        val endDate = java.util.Date(startDate.time + dur * 60_000L)
        val endLabel = timeFormatter.format(endDate)
        return "$dateLabel, $startLabel - $endLabel"
    }
}
