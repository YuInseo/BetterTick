package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.util.DateUtils.toLocalDate
import java.time.LocalDate

/** 목록 ⋮ > 그룹 및 정렬 > 그룹. */
enum class TaskGroupBy(val key: String, val label: String) {
    DATE("date", "날짜"),
    LIST("list", "목록"),
    PRIORITY("priority", "우선순위"),
    TAG("tag", "태그"),
    NONE("none", "없음")
}

/** 목록 ⋮ > 그룹 및 정렬 > 정렬. */
enum class TaskSortBy(val key: String, val label: String) {
    CUSTOM("custom", "사용자 지정"),
    DATE("date", "날짜"),
    TITLE("title", "제목"),
    PRIORITY("priority", "우선순위"),
    CREATED("created", "생성 시간")
}

/** TickTick list-view options from the task list's ⋮ menu (persisted). */
object TaskListPrefs {
    private const val PREFS = "task_list_prefs"

    var showDetails by mutableStateOf(true)
        private set
    var showCompleted by mutableStateOf(true)
        private set
    var groupBy by mutableStateOf(TaskGroupBy.DATE)
        private set
    var sortBy by mutableStateOf(TaskSortBy.CUSTOM)
        private set

    /** Transient: hides the global + button during multi-select. */
    var selectionActive by mutableStateOf(false)

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        showDetails = p.getBoolean("show_details", true)
        showCompleted = p.getBoolean("show_completed", true)
        groupBy = TaskGroupBy.entries.firstOrNull { it.key == p.getString("group_by", null) } ?: TaskGroupBy.DATE
        sortBy = TaskSortBy.entries.firstOrNull { it.key == p.getString("sort_by", null) } ?: TaskSortBy.CUSTOM
    }

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.apply(block)?.apply()
    }

    fun updateShowDetails(v: Boolean) { showDetails = v; edit { putBoolean("show_details", v) } }
    fun updateShowCompleted(v: Boolean) { showCompleted = v; edit { putBoolean("show_completed", v) } }
    fun updateGroupBy(v: TaskGroupBy) { groupBy = v; edit { putString("group_by", v.key) } }
    fun updateSortBy(v: TaskSortBy) { sortBy = v; edit { putString("sort_by", v.key) } }

    /** Comparator for the chosen 정렬; repeats first within 사용자 지정 like before. */
    fun comparator(): Comparator<Task> = compareByDescending<Task> { it.isPinned }.then(baseComparator())

    private fun baseComparator(): Comparator<Task> = when (sortBy) {
        TaskSortBy.CUSTOM -> compareByDescending<Task> { !it.repeatRule.isNullOrBlank() }.thenBy { it.sortOrder }
        TaskSortBy.DATE -> compareBy<Task> { it.dueDate?.toDate()?.time ?: Long.MAX_VALUE }.thenByDescending { it.priority }
        TaskSortBy.TITLE -> compareBy { it.title.lowercase() }
        TaskSortBy.PRIORITY -> compareByDescending<Task> { it.priority }.thenBy { it.dueDate?.toDate()?.time ?: Long.MAX_VALUE }
        TaskSortBy.CREATED -> compareByDescending { it.createdAt.seconds }
    }
}

/** One collapsible block of the task list. */
data class TaskGroup(
    val key: String,
    val title: String,
    val tasks: List<Task>,
    val isOverdue: Boolean = false,
    val colorHex: String? = null
)

/**
 * Groups open tasks for the non-date groupings (목록 / 우선순위 / 태그 / 없음).
 * Date grouping keeps the screen's configurable 기한 지남/오늘/예정/날짜 없음
 * sections and is built there.
 */
fun groupOpenTasks(
    tasks: List<Task>,
    by: TaskGroupBy,
    lists: List<TaskList>,
    tags: List<Tag>,
    sort: Comparator<Task>
): List<TaskGroup> = when (by) {
    TaskGroupBy.LIST -> {
        val order = listOf("" to "기본함") + lists.map { it.id to it.name }
        order.mapNotNull { (id, name) ->
            val l = tasks.filter { it.listId == id || (id == "" && lists.none { x -> x.id == it.listId }) }
            if (l.isEmpty()) null else TaskGroup("list:$id", name, l.sortedWith(sort),
                colorHex = lists.firstOrNull { it.id == id }?.color)
        }
    }
    TaskGroupBy.PRIORITY -> listOf(3 to "높은 우선순위", 2 to "중간 우선순위", 1 to "낮은 우선순위", 0 to "우선순위 없음")
        .mapNotNull { (lvl, name) ->
            val l = tasks.filter { it.priority == lvl }
            if (l.isEmpty()) null else TaskGroup("prio:$lvl", name, l.sortedWith(sort))
        }
    TaskGroupBy.TAG -> {
        val groups = tags.mapNotNull { t ->
            val l = tasks.filter { t.id in it.tagIds }
            if (l.isEmpty()) null else TaskGroup("tag:${t.id}", "#${t.name}", l.sortedWith(sort), colorHex = t.color)
        }
        val untagged = tasks.filter { t -> t.tagIds.none { id -> tags.any { it.id == id } } }
        groups + listOfNotNull(
            if (untagged.isEmpty()) null else TaskGroup("tag:none", "태그 없음", untagged.sortedWith(sort))
        )
    }
    TaskGroupBy.NONE -> if (tasks.isEmpty()) emptyList() else listOf(TaskGroup("all", "", tasks.sortedWith(sort)))
    TaskGroupBy.DATE -> {
        // Fallback (the screen normally builds date sections itself).
        val today = LocalDate.now()
        val (over, rest) = tasks.partition { t -> t.dueDate?.toLocalDate()?.isBefore(today) == true }
        listOfNotNull(
            if (over.isEmpty()) null else TaskGroup("date:overdue", "기한 지남", over.sortedWith(sort), isOverdue = true),
            if (rest.isEmpty()) null else TaskGroup("date:rest", "할일", rest.sortedWith(sort))
        )
    }
}

/** Plain-text export used by 목록 ⋮ > 공유. */
fun buildTaskListShareText(title: String, groups: List<TaskGroup>): String {
    val sb = StringBuilder(title).append("\n")
    groups.forEach { g ->
        if (g.title.isNotEmpty()) sb.append("\n").append(g.title).append("\n")
        g.tasks.forEach { t -> sb.append(if (t.isCompleted) "☑ " else "☐ ").append(t.title).append("\n") }
    }
    return sb.toString().trimEnd()
}
