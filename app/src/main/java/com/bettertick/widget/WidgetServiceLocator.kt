package com.bettertick.widget

import android.content.Context
import android.content.SharedPreferences
import com.bettertick.data.model.FocusSession
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.repository.FocusRepository
import com.bettertick.data.repository.HabitRepository
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * ServiceLocator for widgets — bypasses Hilt (not available in AppWidgetProvider).
 * Initialized from BetterTickApplication after Hilt injection completes.
 */
object WidgetServiceLocator {
    private var _taskRepository: TaskRepository? = null
    private var _habitRepository: HabitRepository? = null
    private var _focusRepository: FocusRepository? = null
    private var _listRepository: ListRepository? = null

    // Hot caches populated by BetterTickApplication so `provideGlance` can
    // read the latest data synchronously. Cold `observeX().first()` calls from
    // provideGlance raced against the 1500ms safeLoad timeout on freshly-added
    // tasks — by the time the new cache listener spun up, the render had
    // already fallen back to emptyList.
    private val _tasksCache = MutableStateFlow<List<Task>>(emptyList())
    val tasksCache: StateFlow<List<Task>> = _tasksCache.asStateFlow()

    private val _listsCache = MutableStateFlow<List<TaskList>>(emptyList())
    val listsCache: StateFlow<List<TaskList>> = _listsCache.asStateFlow()

    // Focus sessions for the current week — feeds FocusDistributionWidget.
    private val _focusWeekCache = MutableStateFlow<List<FocusSession>>(emptyList())
    val focusWeekCache: StateFlow<List<FocusSession>> = _focusWeekCache.asStateFlow()

    fun publishTasks(tasks: List<Task>) { _tasksCache.value = tasks }
    fun publishLists(lists: List<TaskList>) { _listsCache.value = lists }
    fun publishFocusWeek(sessions: List<FocusSession>) { _focusWeekCache.value = sessions }

    // Selected date for the ReminderWidget (persisted so it survives process
    // death / widget host rebinds). Defaults to today when unset or stale.
    private const val PREFS_NAME = "bettertick_widget_prefs"
    private const val KEY_SELECTED_DATE = "reminder_selected_date"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun selectedDate(context: Context): LocalDate {
        val stored = prefs(context).getString(KEY_SELECTED_DATE, null)
        val parsed = runCatching { stored?.let(LocalDate::parse) }.getOrNull()
        val today = LocalDate.now()
        // Auto-revert stale picks (e.g. picked yesterday and never touched
        // again) so the widget always defaults to today on cold renders.
        return if (parsed == null || parsed.isBefore(today)) today else parsed
    }

    fun setSelectedDate(context: Context, date: LocalDate) {
        prefs(context).edit().putString(KEY_SELECTED_DATE, date.toString()).apply()
    }

    // Whether the in-widget overflow menu (새로 고침 / 설정) is currently
    // shown. Kept in prefs so a widget host rebind doesn't lose the toggle.
    private const val KEY_MENU_OPEN = "reminder_menu_open"

    fun isMenuOpen(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MENU_OPEN, false)

    fun setMenuOpen(context: Context, open: Boolean) {
        prefs(context).edit().putBoolean(KEY_MENU_OPEN, open).apply()
    }

    // ── Widget settings ────────────────────────────────────────────────────
    // Fields here mirror the settings screen. All have sensible defaults so
    // the widget renders correctly before the user ever opens settings.
    private const val KEY_THEME = "widget_theme"              // "black" | "gray" | "white"
    private const val KEY_FONT_SIZE = "widget_font_size"      // "small" | "normal" | "large"
    private const val KEY_OPACITY = "widget_opacity"          // 0..100
    private const val KEY_FILTER = "widget_filter"            // legacy: "all" | list id
    private const val KEY_FILTER_LISTS = "widget_filter_lists" // list ids ("" = 기본함)
    private const val KEY_FILTER_TAGS = "widget_filter_tags"
    private const val KEY_MIN_PRIORITY = "widget_min_priority" // 0 = 전체
    private const val KEY_EXCLUDE = "widget_filter_exclude"   // true = 선택 항목 숨기기
    private const val KEY_SHOW_COMPLETED = "widget_show_completed"
    private const val KEY_SHOW_DETAIL = "widget_show_detail"
    private const val KEY_SHOW_OVERDUE = "widget_show_overdue"
    private const val KEY_SHOW_WEEK = "widget_show_week"
    private const val KEY_SORT = "widget_sort"                // "time" | "priority" | "title"

    data class ReminderSettings(
        val theme: String = "black",
        val fontSize: String = "normal",
        val opacity: Int = 100,
        /** 필터 보기 범위: empty sets = 전체. "" in [lists] means 기본함. */
        val lists: Set<String> = emptySet(),
        val tags: Set<String> = emptySet(),
        val minPriority: Int = 0,
        /** false: show only the checked lists/tags. true: hide them. */
        val exclude: Boolean = false,
        val showCompleted: Boolean = true,
        val showDetail: Boolean = true,
        val showOverdue: Boolean = false,
        val showWeek: Boolean = true,
        val sort: String = "time"
    ) {
        val isFiltered: Boolean get() = lists.isNotEmpty() || tags.isNotEmpty() || minPriority > 0

        /** Whether [task] falls inside 필터 보기 범위. */
        fun accepts(task: Task): Boolean {
            if (task.priority < minPriority) return false
            return if (exclude) {
                task.listId !in lists && task.tagIds.none { it in tags }
            } else {
                (lists.isEmpty() || task.listId in lists) &&
                    (tags.isEmpty() || task.tagIds.any { it in tags })
            }
        }
    }

    fun reminderSettings(context: Context): ReminderSettings {
        val p = prefs(context)
        val legacy = p.getString(KEY_FILTER, "all") ?: "all"
        val lists = p.getStringSet(KEY_FILTER_LISTS, null)?.toSet()
            ?: if (legacy != "all") setOf(legacy) else emptySet()
        return ReminderSettings(
            theme = p.getString(KEY_THEME, "black") ?: "black",
            fontSize = p.getString(KEY_FONT_SIZE, "normal") ?: "normal",
            opacity = p.getInt(KEY_OPACITY, 100),
            lists = lists,
            tags = p.getStringSet(KEY_FILTER_TAGS, emptySet()).orEmpty().toSet(),
            minPriority = p.getInt(KEY_MIN_PRIORITY, 0),
            exclude = p.getBoolean(KEY_EXCLUDE, false),
            showCompleted = p.getBoolean(KEY_SHOW_COMPLETED, true),
            showDetail = p.getBoolean(KEY_SHOW_DETAIL, true),
            showOverdue = p.getBoolean(KEY_SHOW_OVERDUE, false),
            showWeek = p.getBoolean(KEY_SHOW_WEEK, true),
            sort = p.getString(KEY_SORT, "time") ?: "time"
        )
    }

    fun saveReminderSettings(context: Context, s: ReminderSettings) {
        prefs(context).edit()
            .putString(KEY_THEME, s.theme)
            .putString(KEY_FONT_SIZE, s.fontSize)
            .putInt(KEY_OPACITY, s.opacity)
            .remove(KEY_FILTER)
            .putStringSet(KEY_FILTER_LISTS, s.lists)
            .putStringSet(KEY_FILTER_TAGS, s.tags)
            .putInt(KEY_MIN_PRIORITY, s.minPriority)
            .putBoolean(KEY_EXCLUDE, s.exclude)
            .putBoolean(KEY_SHOW_COMPLETED, s.showCompleted)
            .putBoolean(KEY_SHOW_DETAIL, s.showDetail)
            .putBoolean(KEY_SHOW_OVERDUE, s.showOverdue)
            .putBoolean(KEY_SHOW_WEEK, s.showWeek)
            .putString(KEY_SORT, s.sort)
            // commit(): the widget re-reads these right after the activity
            // closes, so the write must land before the refresh starts.
            .commit()
    }

    val taskRepository: TaskRepository
        get() = _taskRepository ?: throw IllegalStateException("WidgetServiceLocator not initialized")

    val habitRepository: HabitRepository
        get() = _habitRepository ?: throw IllegalStateException("WidgetServiceLocator not initialized")

    val focusRepository: FocusRepository
        get() = _focusRepository ?: throw IllegalStateException("WidgetServiceLocator not initialized")

    val listRepository: ListRepository
        get() = _listRepository ?: throw IllegalStateException("WidgetServiceLocator not initialized")

    val isInitialized: Boolean
        get() = _taskRepository != null

    /** Whether both DI init AND Firebase auth are ready for data access. */
    val isReady: Boolean
        get() = isInitialized && runCatching {
            FirebaseAuth.getInstance().currentUser != null
        }.getOrDefault(false)

    fun init(
        taskRepository: TaskRepository,
        habitRepository: HabitRepository,
        focusRepository: FocusRepository,
        listRepository: ListRepository
    ) {
        _taskRepository = taskRepository
        _habitRepository = habitRepository
        _focusRepository = focusRepository
        _listRepository = listRepository
    }

    /**
     * Safely load widget data with a hard timeout + graceful fallback.
     * Ensures `provideGlance` never hangs when Firestore cache is cold or
     * user is not authenticated.
     */
    suspend fun <T> safeLoad(default: T, timeoutMs: Long = 3000L, block: suspend () -> T): T {
        if (!isReady) return default
        return runCatching {
            withTimeoutOrNull(timeoutMs) { block() } ?: default
        }.getOrDefault(default)
    }
}
