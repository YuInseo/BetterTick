package com.bettertick.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.Tag
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.buildShiftTasks
import com.bettertick.data.model.detectShiftPattern
import com.bettertick.data.model.isShiftRotationOf
import com.bettertick.data.model.matchedPrefixLength
import com.bettertick.data.model.normalizeShiftLabel
import com.bettertick.data.model.occursOn
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.TagRepository
import com.bettertick.data.repository.TaskRepository
import com.bettertick.util.DateUtils.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = tagRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun createTag(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return ""
        tags.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            ?.let { return it.id }
        return tagRepository.addTag(Tag(name = trimmed))
    }

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    /** Months rendered in the scrollable list, centered on today. */
    val monthsList: List<YearMonth> = run {
        val center = YearMonth.now()
        (-MONTHS_RANGE..MONTHS_RANGE).map { center.plusMonths(it.toLong()) }
    }

    /** Years covered by [monthsList], used by the YEAR view's infinite scroll. */
    val yearsList: List<Int> = run {
        val first = monthsList.first().year
        val last = monthsList.last().year
        (first..last).toList()
    }

    /**
     * Tasks across the full visible range (±MONTHS_RANGE months around today),
     * grouped by date. Loaded once; continuously updated from Firestore snapshot.
     */
    val tasksByDate: StateFlow<Map<LocalDate, List<Task>>> = run {
        val startDate = monthsList.first().atDay(1)
        val endDate = monthsList.last().atEndOfMonth()
        taskRepository.observeTasksForDateRange(startDate, endDate).map { tasks ->
            // Partition into one-shot vs recurring. One-shots group by due
            // date (cheap). Recurring tasks get expanded into every visible
            // date they apply to so calendar dots + the per-day list include
            // them — previously repeatRule was silently ignored here.
            val result = mutableMapOf<LocalDate, MutableList<Task>>()
            val recurring = mutableListOf<Task>()
            tasks.forEach { task ->
                if (task.repeatRule.isNullOrBlank()) {
                    val due = task.dueDate?.toLocalDate() ?: return@forEach
                    result.getOrPut(due) { mutableListOf() }.add(task)
                } else {
                    recurring += task
                }
            }
            if (recurring.isNotEmpty()) {
                var day = startDate
                while (!day.isAfter(endDate)) {
                    recurring.forEach { task ->
                        if (task.occursOn(day)) {
                            result.getOrPut(day) { mutableListOf() }.add(task)
                        }
                    }
                    day = day.plusDays(1)
                }
            }
            // Sort repeating tasks first within each day — visual grouping
            // that matches the per-day panel's section ordering.
            result.mapValues { (_, list) ->
                list.sortedWith(
                    compareByDescending<Task> { !it.repeatRule.isNullOrBlank() }
                        .thenBy { it.dueDate?.toDate()?.time ?: Long.MAX_VALUE }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    /** Tapping the currently selected date clears the selection (closes the panel). */
    fun selectDate(date: LocalDate) {
        if (_selectedDate.value == date) {
            _selectedDate.value = null
        } else {
            _selectedDate.value = date
            if (YearMonth.from(date) != _selectedMonth.value) {
                _selectedMonth.value = YearMonth.from(date)
            }
        }
    }

    /** Explicit clear — used by the pull-down gesture on the week strip to
     *  expand back to the full-month view. */
    fun clearSelection() {
        _selectedDate.value = null
    }

    /** Flat list of all tasks in the visible range — used by WEEK timeline
     *  which does its own per-day partitioning (including recurring expansion). */
    val allTasks: StateFlow<List<Task>> = run {
        val startDate = monthsList.first().atDay(1)
        val endDate = monthsList.last().atEndOfMonth()
        taskRepository.observeTasksForDateRange(startDate, endDate)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    /** Called by the scrollable calendar when the visible month changes. */
    fun onVisibleMonthChanged(month: YearMonth) {
        if (_selectedMonth.value != month) {
            _selectedMonth.value = month
        }
    }

    /**
     * Called by the year view when the dominant visible year changes. Updates
     * [selectedMonth] in-place so the header reflects the scroll, keeping the
     * month part so that switching back to MONTH view lands on the same month
     * within the scrolled-to year.
     */
    fun onVisibleYearChanged(year: Int) {
        val current = _selectedMonth.value
        if (current.year != year) {
            _selectedMonth.value = YearMonth.of(year, current.monthValue)
        }
    }

    fun toggleTaskComplete(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleComplete(taskId, isCompleted)
        }
    }

    fun setAbandoned(taskId: String, isAbandoned: Boolean) {
        viewModelScope.launch {
            taskRepository.setAbandoned(taskId, isAbandoned)
        }
    }

    fun moveTaskToDate(taskId: String, date: LocalDate) {
        viewModelScope.launch {
            val ts = com.google.firebase.Timestamp(
                java.util.Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            )
            taskRepository.setDueDate(taskId, ts)
        }
    }

    /** Move a single occurrence of a recurring task, leaving the series intact. */
    fun moveTaskOccurrence(taskId: String, sourceDate: LocalDate, newDate: LocalDate) {
        viewModelScope.launch {
            taskRepository.moveOccurrence(taskId, sourceDate, newDate)
        }
    }

    /** Skip a single occurrence of a recurring task. */
    fun skipTaskOccurrence(taskId: String, date: LocalDate) {
        viewModelScope.launch {
            taskRepository.skipOccurrence(taskId, date)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    /**
     * Persist a task edit — notes, notion URL, title, etc. — from the detail
     * sheet. Fire-and-forget: Firestore's offline cache accepts the write
     * immediately so the UI doesn't need to wait.
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun createTask(task: Task) {
        viewModelScope.launch {
            taskRepository.addTask(task)
        }
    }

    /** 선택 날짜 주변에 입력된 근무(주/야/비/휴 등)에서 반복 패턴을 찾는다. */
    fun detectShiftRotation(anchor: LocalDate) = detectShiftPattern(tasksByDate.value, anchor)

    /**
     * 근무 패턴을 [start]부터 [pattern] 주기로 자동 반복한다.
     * 이미 입력된 앞부분([run] 중 패턴과 일치하는 날들)은 그대로 두고 다음 날부터
     * 패턴 칸마다 N일 반복 할일을 만든다. 같은 라벨로 예전에 만든 근무 반복이
     * 있으면 새 반복 시작 전날에서 끊어 겹치지 않게 한다.
     */
    fun applyShiftRotation(
        start: LocalDate,
        pattern: List<String>,
        run: List<String>,
        end: LocalDate?
    ) {
        val cleaned = pattern.map { it.trim() }
        val labels = cleaned.filter { it.isNotBlank() }.toSet()
        if (labels.isEmpty()) return
        val byDate = tasksByDate.value
        val from = start.plusDays(matchedPrefixLength(run, cleaned).toLong())

        // 같은 라벨의 기존 할일을 템플릿으로 — 정확히 같은 제목 우선, 시작일에 가까운 것 우선.
        val oneShots = byDate.entries
            .sortedBy { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(start, it.key)) }
            .flatMap { it.value }
            .filter { it.repeatRule.isNullOrBlank() }
        val templates = labels.mapNotNull { label ->
            (oneShots.firstOrNull { it.title.trim() == label }
                ?: oneShots.firstOrNull { normalizeShiftLabel(it.title) == label })
                ?.let { label to it }
        }.toMap()

        val newTasks = buildShiftTasks(start, cleaned, from, end, templates, byDate)
        viewModelScope.launch {
            allTasks.value.filter { it.isShiftRotationOf(labels) }.forEach { old ->
                val oldStart = old.dueDate?.toLocalDate() ?: return@forEach
                if (!oldStart.isBefore(from)) {
                    taskRepository.deleteTask(old.id)
                } else {
                    val cut = from.minusDays(1)
                    val oldEnd = old.repeatEnd?.takeIf { it.startsWith("DATE:") }
                        ?.let { runCatching { LocalDate.parse(it.removePrefix("DATE:")) }.getOrNull() }
                    if (oldEnd == null || oldEnd.isAfter(cut)) {
                        taskRepository.updateTask(old.copy(repeatEnd = "DATE:$cut"))
                    }
                }
            }
            newTasks.forEach { taskRepository.addTask(it) }
        }
    }

    /** Lists used to resolve the display name in the detail sheet header. */
    val lists: StateFlow<List<TaskList>> =
        listRepository.observeLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Looks up the display name for a task's list, falling back to 기본함. */
    fun listNameFor(listId: String): String {
        if (listId.isBlank()) return "기본함"
        return lists.value.firstOrNull { it.id == listId }?.name ?: "기본함"
    }

    companion object {
        // ± this many months around today are rendered in the scrollable
        // calendars. Also bounds the Firestore task range query, so don't
        // push this absurdly high — 60 covers a full decade either side.
        private const val MONTHS_RANGE = 60
    }
}
