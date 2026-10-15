package com.bettertick.ui.screens.tasks

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * TickTick-style "스마트 인식" for the quick-add title. Recognized tokens are
 * removed from the title and turned into task fields:
 *
 *  - 날짜: 오늘 / 내일 / 모레 / 글피, (이번 주|다음 주) 월요일…, 월요일,
 *          M월 D일, M/D, D일
 *  - 시간: 오전/오후/아침/저녁/밤 H시 (M분 | 반), H시, HH:MM
 *  - 반복: 매일, 평일마다/매주 평일, 매주 (X요일), 매월|매달 (D일), 매년
 *  - 태그: #태그
 *  - 우선순위: !높음 !중간 !낮음 !없음, !3 !2 !1 !0, !!! / !!
 *  - 목록: ^목록이름
 */
object QuickAddParser {

    data class Result(
        val title: String,
        val date: LocalDate? = null,
        val time: LocalTime? = null,
        val repeatRule: String? = null,
        val tagNames: List<String> = emptyList(),
        val priority: Int? = null,
        val listName: String? = null,
        /** Human-readable tokens that were recognized (for the hint row). */
        val recognized: List<String> = emptyList()
    )

    private val dowNames = mapOf(
        "월" to DayOfWeek.MONDAY, "화" to DayOfWeek.TUESDAY, "수" to DayOfWeek.WEDNESDAY,
        "목" to DayOfWeek.THURSDAY, "금" to DayOfWeek.FRIDAY, "토" to DayOfWeek.SATURDAY,
        "일" to DayOfWeek.SUNDAY
    )

    fun parse(input: String, today: LocalDate = LocalDate.now()): Result {
        var text = " $input "
        val recognized = mutableListOf<String>()
        fun take(regex: Regex): MatchResult? {
            val m = regex.find(text) ?: return null
            text = text.removeRange(m.range).let { it.substring(0, m.range.first) + " " + it.substring(m.range.first) }
            recognized += m.value.trim()
            return m
        }

        // 태그 #name (stop at whitespace)
        val tags = mutableListOf<String>()
        while (true) {
            val m = take(Regex("""(?<=\s)#([^\s#!^]+)""")) ?: break
            tags += m.groupValues[1]
        }

        // 우선순위
        var priority: Int? = null
        take(Regex("""(?<=\s)!(높음|중간|낮음|없음|[0-3]|high|medium|low|none)(?=\s)""", RegexOption.IGNORE_CASE))?.let {
            priority = when (it.groupValues[1].lowercase()) {
                "높음", "3", "high" -> 3
                "중간", "2", "medium" -> 2
                "낮음", "1", "low" -> 1
                else -> 0
            }
        } ?: take(Regex("""(?<=\s)(!!!|!!)(?=\s)"""))?.let {
            priority = if (it.value.trim() == "!!!") 3 else 2
        }

        // 목록 ^name
        val listName = take(Regex("""(?<=\s)\^([^\s#!^]+)"""))?.groupValues?.get(1)

        // 반복
        var repeat: String? = null
        var repeatAnchor: LocalDate? = null
        take(Regex("""(?<=\s)(평일마다|매주\s*평일|주중마다)(?=\s)"""))?.let { repeat = "WEEKDAYS" }
            ?: take(Regex("""(?<=\s)매일(?=\s)"""))?.let { repeat = "DAILY" }
            ?: take(Regex("""(?<=\s)매주\s*([월화수목금토일])요일(?=\s)"""))?.let {
                val dow = dowNames.getValue(it.groupValues[1])
                repeat = "WEEKLY:${dow.name}"
                repeatAnchor = today.with(TemporalAdjusters.nextOrSame(dow))
            }
            ?: take(Regex("""(?<=\s)매주(?=\s)"""))?.let { repeat = "WEEKLY:${today.dayOfWeek.name}" }
            ?: take(Regex("""(?<=\s)(매월|매달)\s*(\d{1,2})일(?=\s)"""))?.let {
                val d = it.groupValues[2].toInt().coerceIn(1, 31)
                repeat = "MONTHLY:$d"
                val candidate = today.withDayOfMonth(d.coerceAtMost(today.lengthOfMonth()))
                repeatAnchor = if (candidate.isBefore(today)) candidate.plusMonths(1) else candidate
            }
            ?: take(Regex("""(?<=\s)(매월|매달)(?=\s)"""))?.let { repeat = "MONTHLY:${today.dayOfMonth}" }
            ?: take(Regex("""(?<=\s)매년(?=\s)"""))?.let { repeat = "YEARLY:${today.monthValue}:${today.dayOfMonth}" }

        // 날짜
        var date: LocalDate? = null
        take(Regex("""(?<=\s)(오늘|금일)(?:까지|에|부터)?(?=\s)"""))?.let { date = today }
            ?: take(Regex("""(?<=\s)(내일|명일)(?:까지|에|부터)?(?=\s)"""))?.let { date = today.plusDays(1) }
            ?: take(Regex("""(?<=\s)모레(?:까지|에|부터)?(?=\s)"""))?.let { date = today.plusDays(2) }
            ?: take(Regex("""(?<=\s)글피(?:까지|에|부터)?(?=\s)"""))?.let { date = today.plusDays(3) }
            ?: take(Regex("""(?<=\s)(다음\s*주|담주|차주)\s*([월화수목금토일])요일(?:까지|에|부터)?(?=\s)"""))?.let {
                val dow = dowNames.getValue(it.groupValues[2])
                val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                date = nextMonday.with(TemporalAdjusters.nextOrSame(dow))
            }
            ?: take(Regex("""(?<=\s)(다음\s*주|담주|차주)(?=\s)"""))?.let {
                date = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            }
            ?: take(Regex("""(?<=\s)(이번\s*주\s*)?([월화수목금토일])요일(?:까지|에|부터)?(?=\s)"""))?.let {
                date = today.with(TemporalAdjusters.nextOrSame(dowNames.getValue(it.groupValues[2])))
            }
            ?: take(Regex("""(?<=\s)(\d{1,2})월\s*(\d{1,2})일(?:까지|에|부터)?(?=\s)"""))?.let {
                date = safeDate(today, it.groupValues[1].toInt(), it.groupValues[2].toInt())
            }
            ?: take(Regex("""(?<=\s)(\d{1,2})/(\d{1,2})(?=\s)"""))?.let {
                date = safeDate(today, it.groupValues[1].toInt(), it.groupValues[2].toInt())
            }
            ?: take(Regex("""(?<=\s)(\d{1,2})일(?=\s)"""))?.let {
                val d = it.groupValues[1].toInt()
                if (d in 1..31) {
                    val thisMonth = runCatching { today.withDayOfMonth(d) }.getOrNull()
                    date = if (thisMonth != null && !thisMonth.isBefore(today)) thisMonth
                        else runCatching { today.plusMonths(1).withDayOfMonth(d) }.getOrNull()
                }
            }

        // 시간
        var time: LocalTime? = null
        take(Regex("""(?<=\s)(오전|오후|아침|저녁|밤|새벽|낮)?\s*(\d{1,2})시\s*(?:(\d{1,2})분|(반))?(?=\s)"""))?.let { m ->
            var h = m.groupValues[2].toInt()
            val min = when {
                m.groupValues[4].isNotEmpty() -> 30
                m.groupValues[3].isNotEmpty() -> m.groupValues[3].toInt()
                else -> 0
            }
            when (m.groupValues[1]) {
                "오후", "저녁", "밤" -> if (h < 12) h += 12
                "낮" -> if (h < 6) h += 12
                "오전", "아침", "새벽" -> if (h == 12) h = 0
                else -> if (h in 1..6) h += 12 // "3시" reads as afternoon, like TickTick
            }
            if (h in 0..23 && min in 0..59) time = LocalTime.of(h, min)
        } ?: take(Regex("""(?<=\s)(\d{1,2}):(\d{2})(?=\s)"""))?.let { m ->
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) time = LocalTime.of(h, min)
        }

        if (date == null && repeatAnchor != null) date = repeatAnchor
        if (date == null && (time != null || repeat != null)) date = today

        val title = text.replace(Regex("""\s+"""), " ").trim()
        return Result(
            title = title.ifEmpty { input.trim() },
            date = date,
            time = time,
            repeatRule = repeat,
            tagNames = tags,
            priority = priority,
            listName = listName,
            recognized = recognized
        )
    }

    private fun safeDate(today: LocalDate, month: Int, day: Int): LocalDate? {
        val d = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
        return if (d.isBefore(today.minusDays(1))) runCatching { d.plusYears(1) }.getOrNull() else d
    }
}
