package com.bettertick.data.model

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * 교대 근무(주/야/비/휴 등) 패턴 자동 반복.
 *
 * 사용자가 달력에 며칠치 근무를 직접 입력해 두면, 그 연속 구간을 읽어
 * 최소 주기를 찾아내고(예: 주→야간→비→휴 = 4일), 패턴의 칸마다
 * "N일마다" 반복 할일(`CUSTOM|ByDueDate|N|Day||`)을 하나씩 만든다.
 * 기존 반복 엔진([occursOn])을 그대로 쓰므로 달력·위젯·건너뛰기가 모두
 * 추가 작업 없이 동작한다.
 */
data class ShiftDetection(
    /** 패턴 첫 칸이 놓이는 날짜 (연속 입력 구간의 첫 날 또는 선택 날짜). */
    val start: LocalDate,
    /** start부터 연속으로 입력돼 있던 근무 라벨 (정규화됨). */
    val run: List<String>,
    /** run에서 찾은 최소 반복 단위. run이 비어 있으면 빈 리스트. */
    val pattern: List<String>
)

/** "주 (병가)" → "주". 끝에 붙은 괄호 메모를 떼어 같은 근무로 취급한다. */
fun normalizeShiftLabel(title: String): String =
    title.trim()
        .replace(Regex("""\s*[(（][^()（）]*[)）]\s*$"""), "")
        .trim()

private const val MAX_RUN_DAYS = 62

/** 그 날의 근무 라벨: 반복이 아닌 첫 할일의 정규화된 제목. */
private fun labelOn(tasksByDate: Map<LocalDate, List<Task>>, day: LocalDate): String? =
    tasksByDate[day].orEmpty()
        .firstOrNull { it.repeatRule.isNullOrBlank() && !it.isAbandoned && it.title.isNotBlank() }
        ?.let { normalizeShiftLabel(it.title) }
        ?.takeIf { it.isNotEmpty() }

/** [seq]를 그대로 재현하는 가장 짧은 반복 단위. */
fun minimalPeriod(seq: List<String>): List<String> {
    for (p in 1..seq.size) {
        if (seq.indices.all { seq[it] == seq[it % p] }) return seq.take(p)
    }
    return seq
}

/**
 * [anchor]가 포함된 "매일 무언가 입력된" 연속 구간을 찾아 패턴을 추출한다.
 * anchor에 아무것도 없으면 빈 패턴과 함께 start = anchor를 돌려준다.
 */
fun detectShiftPattern(
    tasksByDate: Map<LocalDate, List<Task>>,
    anchor: LocalDate
): ShiftDetection {
    if (labelOn(tasksByDate, anchor) == null) {
        return ShiftDetection(anchor, emptyList(), emptyList())
    }
    var start = anchor
    var back = 0
    while (back < MAX_RUN_DAYS && labelOn(tasksByDate, start.minusDays(1)) != null) {
        start = start.minusDays(1)
        back++
    }
    val run = mutableListOf<String>()
    var d = start
    while (run.size < MAX_RUN_DAYS) {
        val label = labelOn(tasksByDate, d) ?: break
        run += label
        d = d.plusDays(1)
    }
    return ShiftDetection(start, run, minimalPeriod(run))
}

/** run 중 [pattern]과 일치하는 앞부분 길이 — 이 날들은 이미 입력돼 있으므로 건너뛴다. */
fun matchedPrefixLength(run: List<String>, pattern: List<String>): Int {
    if (pattern.isEmpty()) return 0
    var n = 0
    while (n < run.size && run[n] == pattern[n % pattern.size]) n++
    return n
}

/** 패턴 칸 하나의 첫 발생일: start + index 를 기준으로 from 이상이 되도록 주기만큼 민다. */
private fun firstOccurrence(start: LocalDate, index: Int, period: Int, from: LocalDate): LocalDate {
    var d = start.plusDays(index.toLong())
    if (d.isBefore(from)) {
        val gap = ChronoUnit.DAYS.between(d, from)
        val cycles = (gap + period - 1) / period
        d = d.plusDays(cycles * period)
    }
    return d
}

/** [start]에서 시작한 [pattern]으로 [day]에 들어갈 라벨 (미리보기용). */
fun shiftLabelOn(start: LocalDate, pattern: List<String>, day: LocalDate): String? {
    if (pattern.isEmpty() || day.isBefore(start)) return null
    val idx = (ChronoUnit.DAYS.between(start, day) % pattern.size).toInt()
    return pattern[idx].takeIf { it.isNotBlank() }
}

/**
 * 패턴 칸마다 반복 할일을 만든다. 빈 라벨 칸은 "아무것도 없는 날"로 취급해 건너뛴다.
 *
 * @param from 이 날짜부터 생성 (이미 입력된 구간 다음 날)
 * @param end 반복 종료일 (null = 끝없이)
 * @param templates 같은 라벨의 기존 할일 — 목록/태그/시간/길이 등을 복사
 * @param tasksByDate 이미 근무가 직접 입력된 날은 예외(exceptions)로 넣어 겹치지 않게 함
 */
fun buildShiftTasks(
    start: LocalDate,
    pattern: List<String>,
    from: LocalDate,
    end: LocalDate?,
    templates: Map<String, Task>,
    tasksByDate: Map<LocalDate, List<Task>>
): List<Task> {
    val period = pattern.size
    if (period == 0) return emptyList()
    val labels = pattern.filter { it.isNotBlank() }.toSet()
    // 이미 직접 근무를 적어 둔 날 — 해당 날짜의 반복 발생은 건너뛴다.
    val manualDays = tasksByDate.filter { (day, tasks) ->
        !day.isBefore(from) && tasks.any {
            it.repeatRule.isNullOrBlank() && normalizeShiftLabel(it.title) in labels
        }
    }.keys
    val rule = "CUSTOM|ByDueDate|$period|Day||"
    val zone = ZoneId.systemDefault()
    val now = Timestamp.now()

    return pattern.mapIndexedNotNull { index, label ->
        if (label.isBlank()) return@mapIndexedNotNull null
        val first = firstOccurrence(start, index, period, from)
        if (end != null && first.isAfter(end)) return@mapIndexedNotNull null

        val template = templates[label]
        val time = template?.dueDate?.toDate()?.toInstant()?.atZone(zone)?.toLocalTime()
            ?: LocalTime.MIDNIGHT
        val exceptions = manualDays
            .filter { d ->
                !d.isBefore(first) && (end == null || !d.isAfter(end)) &&
                    ChronoUnit.DAYS.between(first, d) % period == 0L
            }
            .sorted()
            .map { it.toString() }

        (template ?: Task()).copy(
            id = "",
            title = label,
            dueDate = Timestamp(Date.from(first.atTime(time).atZone(zone).toInstant())),
            isCompleted = false,
            completedAt = null,
            isAbandoned = false,
            abandonedAt = null,
            repeatRule = rule,
            repeatEnd = end?.let { "DATE:$it" },
            exceptions = exceptions,
            attachments = emptyList(),
            createdAt = now,
            updatedAt = now
        )
    }
}

/** 이전에 만든 근무 반복(같은 라벨, N일 주기)인지 — 다시 적용할 때 겹치지 않도록 정리 대상. */
fun Task.isShiftRotationOf(labels: Set<String>): Boolean {
    val rule = repeatRule ?: return false
    if (!rule.startsWith("CUSTOM|")) return false
    val parts = rule.split("|")
    return parts.getOrNull(3)?.equals("Day", ignoreCase = true) == true &&
        normalizeShiftLabel(title) in labels
}
