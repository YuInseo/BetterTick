package com.bettertick.ui.screens.tasks.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertick.data.model.ShiftDetection
import com.bettertick.data.model.ShiftHours
import com.bettertick.data.model.Task
import com.bettertick.data.model.buildShiftTasks
import com.bettertick.data.model.detectShiftPattern
import com.bettertick.data.model.isShiftRotationOf
import com.bettertick.data.model.matchedPrefixLength
import com.bettertick.data.model.normalizeShiftLabel
import com.bettertick.data.repository.TaskRepository
import com.bettertick.util.DateUtils.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

/**
 * 블록 설정(반복 > 근무 패턴 반복)에서 쓰는 근무 패턴 자동 반복.
 * 어느 화면의 할일 상세에서 열어도 동작하도록 화면 ViewModel과 분리했다.
 */
@HiltViewModel
class ShiftRotationViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    // detect 시점의 스냅샷 — apply가 같은 데이터를 기준으로 계산하도록 보관.
    private var allTasks: List<Task> = emptyList()
    private var oneShotsByDate: Map<LocalDate, List<Task>> = emptyMap()

    /** [anchor] 주변에 입력된 근무(주/야/비/휴 등)에서 반복 패턴을 찾는다. */
    suspend fun detect(anchor: LocalDate): ShiftDetection {
        allTasks = taskRepository.observeAllTasks().first()
        oneShotsByDate = allTasks
            .filter { it.repeatRule.isNullOrBlank() && it.dueDate != null }
            .sortedBy { it.dueDate?.toDate()?.time ?: Long.MAX_VALUE }
            .groupBy { it.dueDate!!.toLocalDate() }
        return detectShiftPattern(oneShotsByDate, anchor)
    }

    /**
     * 근무 패턴을 [start]부터 [pattern] 주기로 자동 반복한다.
     * 이미 입력된 앞부분([run] 중 패턴과 일치하는 날들)은 그대로 두고 다음 날부터
     * 패턴 칸마다 N일 반복 할일을 만든다. 같은 라벨로 예전에 만든 근무 반복이
     * 있으면 새 반복 시작 전날에서 끊어 겹치지 않게 한다.
     */
    fun apply(
        start: LocalDate,
        pattern: List<String>,
        run: List<String>,
        end: LocalDate?,
        hours: Map<String, ShiftHours> = emptyMap()
    ) {
        val cleaned = pattern.map { it.trim() }
        val labels = cleaned.filter { it.isNotBlank() }.toSet()
        if (labels.isEmpty()) return
        val byDate = oneShotsByDate
        val from = start.plusDays(matchedPrefixLength(run, cleaned).toLong())

        // 같은 라벨의 기존 할일을 템플릿으로 — 정확히 같은 제목 우선, 시작일에 가까운 것 우선.
        val oneShots = byDate.entries
            .sortedBy { abs(ChronoUnit.DAYS.between(start, it.key)) }
            .flatMap { it.value }
        val templates = labels.mapNotNull { label ->
            (oneShots.firstOrNull { it.title.trim() == label }
                ?: oneShots.firstOrNull { normalizeShiftLabel(it.title) == label })
                ?.let { label to it }
        }.toMap()

        val newTasks = buildShiftTasks(start, cleaned, from, end, templates, byDate, hours)
        val previous = allTasks.filter { it.isShiftRotationOf(labels) }
        viewModelScope.launch {
            previous.forEach { old ->
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
}
