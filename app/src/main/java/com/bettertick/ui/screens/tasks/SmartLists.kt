package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bettertick.data.model.Task
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.occursOn
import com.bettertick.util.DateUtils.toLocalDate
import java.time.LocalDate

/** TickTick smart lists shown at the top of the drawer. */
enum class SmartList(val key: String, val label: String) {
    TODAY("today", "오늘"),
    TOMORROW("tomorrow", "내일"),
    NEXT7("next7", "다음 7일"),
    INBOX("inbox", "기본함"),
    COMPLETED("completed", "완료"),
    WONT_DO("wontdo", "하지 않음")
}

/** 설정 > 스마트 목록 — per-list visibility in the drawer. */
enum class SmartVisibility(val key: String, val label: String) {
    SHOW("show", "표시"),
    AUTO("auto", "비어 있지 않으면 표시"),
    HIDE("hide", "숨기기")
}

object SmartListPrefs {
    private const val PREFS = "smart_list_prefs"
    private val defaults = mapOf(
        SmartList.TODAY to SmartVisibility.SHOW,
        SmartList.TOMORROW to SmartVisibility.AUTO,
        SmartList.NEXT7 to SmartVisibility.SHOW,
        SmartList.INBOX to SmartVisibility.SHOW,
        SmartList.COMPLETED to SmartVisibility.SHOW,
        SmartList.WONT_DO to SmartVisibility.AUTO
    )
    var visibility by mutableStateOf(defaults)
        private set
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        visibility = SmartList.entries.associateWith { s ->
            SmartVisibility.entries.firstOrNull { it.key == p.getString(s.key, null) } ?: defaults.getValue(s)
        }
    }

    fun update(list: SmartList, v: SmartVisibility) {
        visibility = visibility + (list to v)
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(list.key, v.key)?.apply()
    }

    fun isVisible(list: SmartList, count: Int): Boolean = when (visibility[list] ?: SmartVisibility.SHOW) {
        SmartVisibility.SHOW -> true
        SmartVisibility.HIDE -> false
        SmartVisibility.AUTO -> count > 0
    }
}

sealed class TaskFilter {
    data object Today : TaskFilter()
    data object Tomorrow : TaskFilter()
    data object Next7Days : TaskFilter()
    data object Inbox : TaskFilter()
    data object Completed : TaskFilter()
    data object WontDo : TaskFilter()
    data class ByList(val listId: String, val listName: String) : TaskFilter()
    data class ByTag(val tagId: String, val tagName: String) : TaskFilter()

    val title: String
        get() = when (this) {
            Today -> "오늘"
            Tomorrow -> "내일"
            Next7Days -> "다음 7일"
            Inbox -> "기본함"
            Completed -> "완료"
            WontDo -> "하지 않음"
            is ByList -> listName
            is ByTag -> "#$tagName"
        }

    /** Drawer selection key. */
    val key: String
        get() = when (this) {
            Today -> "today"
            Tomorrow -> "tomorrow"
            Next7Days -> "next7"
            Inbox -> "inbox"
            Completed -> "completed"
            WontDo -> "wontdo"
            is ByList -> listId
            is ByTag -> "tag:$tagId"
        }

    /** True for lists whose content is finished tasks. */
    val isArchive: Boolean get() = this == Completed || this == WontDo
}

private fun Task.isOpen() = !isCompleted && !isAbandoned

private fun Task.isOverdue(today: LocalDate): Boolean =
    isOpen() && repeatRule.isNullOrBlank() && dueDate?.toLocalDate()?.isBefore(today) == true &&
        !isShiftTask()

private fun Task.dueOn(day: LocalDate): Boolean =
    if (repeatRule.isNullOrBlank()) dueDate?.toLocalDate() == day else occursOn(day)

private fun Task.finishedOn(day: LocalDate): Boolean =
    (completedAt ?: abandonedAt)?.toLocalDate() == day

/**
 * TickTick smart-list membership. Date lists keep tasks finished on that day
 * so they land in the list's 완료됨 group instead of vanishing.
 */
fun TaskFilter.matches(task: Task, lists: List<TaskList>, today: LocalDate = LocalDate.now()): Boolean = when (this) {
    TaskFilter.Today -> if (task.isOpen()) task.dueOn(today) || task.isOverdue(today)
        else task.finishedOn(today) || task.dueDate?.toLocalDate() == today
    TaskFilter.Tomorrow -> task.dueOn(today.plusDays(1)) && task.isOpen()
    TaskFilter.Next7Days -> if (task.isOpen()) task.isOverdue(today) || (0L..6L).any { task.dueOn(today.plusDays(it)) }
        else task.finishedOn(today)
    TaskFilter.Inbox -> task.listId.isBlank() || lists.none { it.id == task.listId }
    TaskFilter.Completed -> task.isCompleted
    TaskFilter.WontDo -> task.isAbandoned && !task.isCompleted
    is TaskFilter.ByList -> task.listId == listId
    is TaskFilter.ByTag -> tagId in task.tagIds
}

/** Open-task count shown next to a smart list in the drawer. */
fun smartListCount(list: SmartList, tasks: List<Task>, lists: List<TaskList>, today: LocalDate = LocalDate.now()): Int {
    val f = list.toFilter()
    return tasks.count { f.matches(it, lists, today) && (f.isArchive || it.isOpen()) }
}

fun SmartList.toFilter(): TaskFilter = when (this) {
    SmartList.TODAY -> TaskFilter.Today
    SmartList.TOMORROW -> TaskFilter.Tomorrow
    SmartList.NEXT7 -> TaskFilter.Next7Days
    SmartList.INBOX -> TaskFilter.Inbox
    SmartList.COMPLETED -> TaskFilter.Completed
    SmartList.WONT_DO -> TaskFilter.WontDo
}

/** Java-friendly alias used by the nav host. */
fun toFilterOf(list: SmartList): TaskFilter = list.toFilter()
