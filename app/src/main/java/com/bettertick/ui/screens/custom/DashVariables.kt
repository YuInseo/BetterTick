package com.bettertick.ui.screens.custom

import com.bettertick.data.model.DiaryEntry
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskList
import com.bettertick.data.model.completedOn
import com.bettertick.data.model.isShiftTask
import com.bettertick.data.model.isWorkOn
import com.bettertick.data.model.occursOn
import com.bettertick.data.model.shiftTitleOn
import com.bettertick.ui.screens.tasks.HabitTodayItem
import com.bettertick.ui.screens.tasks.MoneyPrefs
import com.bettertick.ui.screens.tasks.SalaryPrefs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** How a variable's value reads: drives default formatting and the target of a gauge. */
enum class VarKind { NUMBER, PERCENT, MONEY, TEXT }

/** A value a block can be wired to ("변수"). [key] is what `{key}` stands for in templates. */
data class DashVar(val key: String, val label: String, val group: String, val kind: VarKind)

/** A variable's current value: a number when it has one, and always a display text. */
data class DashValue(val number: Double?, val text: String)

/** Everything the variables read. */
class DashContext(
    val tasks: List<Task>,
    val lists: List<TaskList>,
    val habits: List<HabitTodayItem>,
    val diary: DiaryEntry?,
    val today: LocalDate = LocalDate.now()
)

object DashVars {
    private val fixed = listOf(
        DashVar("today.left", "오늘 남은 할 일", "할 일", VarKind.NUMBER),
        DashVar("today.done", "오늘 완료한 할 일", "할 일", VarKind.NUMBER),
        DashVar("today.total", "오늘 할 일 전체", "할 일", VarKind.NUMBER),
        DashVar("today.rate", "오늘 달성률", "할 일", VarKind.PERCENT),
        DashVar("tomorrow.total", "내일 할 일", "할 일", VarKind.NUMBER),
        DashVar("week.done", "이번 주 완료", "할 일", VarKind.NUMBER),
        DashVar("week.total", "이번 주 할 일 전체", "할 일", VarKind.NUMBER),
        DashVar("week.rate", "이번 주 달성률", "할 일", VarKind.PERCENT),
        DashVar("overdue", "밀린 할 일", "할 일", VarKind.NUMBER),
        DashVar("next.task", "다음 일정", "할 일", VarKind.TEXT),
        DashVar("shift.today", "오늘 근무", "근무", VarKind.TEXT),
        DashVar("shift.tomorrow", "내일 근무", "근무", VarKind.TEXT),
        DashVar("shift.next", "다음 근무일", "근무", VarKind.TEXT),
        DashVar("shift.days_to_next", "다음 근무까지 (일)", "근무", VarKind.NUMBER),
        DashVar("shift.week_work", "이번 주 근무일 수", "근무", VarKind.NUMBER),
        DashVar("shift.month_work", "이번 달 근무일 수", "근무", VarKind.NUMBER),
        DashVar("habit.done", "오늘 체크한 습관", "습관", VarKind.NUMBER),
        DashVar("habit.total", "오늘 할 습관", "습관", VarKind.NUMBER),
        DashVar("habit.rate", "오늘 습관 달성률", "습관", VarKind.PERCENT),
        DashVar("money.income", "이번 달 수입", "가계부", VarKind.MONEY),
        DashVar("money.expense", "이번 달 지출", "가계부", VarKind.MONEY),
        DashVar("money.balance", "이번 달 남은 돈", "가계부", VarKind.MONEY),
        DashVar("money.salary", "이번 달 월급", "가계부", VarKind.MONEY),
        DashVar("money.today_expense", "오늘 지출", "가계부", VarKind.MONEY),
        DashVar("money.daily_budget", "남은 날 하루 예산", "가계부", VarKind.MONEY),
        DashVar("diary.today", "오늘 일기 썼는지", "기록", VarKind.TEXT),
        DashVar("date.today", "오늘 날짜", "날짜", VarKind.TEXT),
        DashVar("date.weekday", "오늘 요일", "날짜", VarKind.TEXT),
        DashVar("date.month_left", "이번 달 남은 날", "날짜", VarKind.NUMBER),
        DashVar("date.year_rate", "올해 지난 비율", "날짜", VarKind.PERCENT),
        DashVar("time.now", "지금 시각 (실행한 때)", "날짜", VarKind.TEXT)
    )

    /** Every variable, with one "남은 할 일" per list. */
    fun all(lists: List<TaskList>): List<DashVar> =
        fixed + lists.map { DashVar("list.left:${it.id}", "${it.name} 남은 할 일", "목록", VarKind.NUMBER) }

    fun find(key: String, lists: List<TaskList>): DashVar? = all(lists).firstOrNull { it.key == key }

    private fun Task.plain() = !isShiftTask() && parentId.isBlank() && !isAbandoned

    private fun shiftOn(ctx: DashContext, day: LocalDate): Task? =
        ctx.tasks.firstOrNull { it.isShiftTask() && it.occursOn(day) }

    private fun pct(done: Int, total: Int) = if (total == 0) 0.0 else done * 100.0 / total

    private fun num(n: Number) = DashValue(n.toDouble(), formatNumber(n.toDouble(), VarKind.NUMBER))
    private fun percent(p: Double) = DashValue(p, formatNumber(p, VarKind.PERCENT))
    private fun money(m: Long) = DashValue(m.toDouble(), formatNumber(m.toDouble(), VarKind.MONEY))
    private fun text(s: String) = DashValue(null, s)

    fun formatNumber(v: Double, kind: VarKind): String = when (kind) {
        VarKind.PERCENT -> "${Math.round(v)}%"
        VarKind.MONEY -> "%,d원".format(Math.round(v))
        else -> if (v == Math.floor(v)) "%,d".format(v.toLong()) else "%.1f".format(v)
    }

    fun value(key: String, ctx: DashContext): DashValue {
        val today = ctx.today
        val month = YearMonth.from(today)
        fun dayCount(day: LocalDate, doneOnly: Boolean): Int =
            ctx.tasks.count { it.plain() && it.occursOn(day) && (!doneOnly || it.completedOn(day)) }
        return when (key) {
            "today.left" -> num(dayCount(today, false) - dayCount(today, true))
            "today.done" -> num(dayCount(today, true))
            "today.total" -> num(dayCount(today, false))
            "today.rate" -> percent(pct(dayCount(today, true), dayCount(today, false)))
            "tomorrow.total" -> num(dayCount(today.plusDays(1), false))
            "week.done", "week.total", "week.rate" -> {
                val monday = today.with(DayOfWeek.MONDAY)
                val days = (0L until 7L).map { monday.plusDays(it) }
                val done = days.sumOf { dayCount(it, true) }
                val total = days.sumOf { dayCount(it, false) }
                when (key) { "week.done" -> num(done); "week.total" -> num(total); else -> percent(pct(done, total)) }
            }
            "overdue" -> num(ctx.tasks.count { t ->
                t.plain() && !t.isCompleted && t.repeatRule.isNullOrBlank() &&
                    (t.dueDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()?.isBefore(today) == true)
            })
            "next.task" -> {
                val now = java.time.LocalDateTime.now()
                val next = (0L..14L).asSequence().flatMap { d ->
                    val day = today.plusDays(d)
                    ctx.tasks.asSequence().filter { it.plain() && it.occursOn(day) && !it.completedOn(day) }.mapNotNull { t ->
                        val time = t.dueDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: return@mapNotNull null
                        val at = day.atTime(time)
                        if (time == LocalTime.MIDNIGHT || at.isBefore(now)) null else at to t
                    }.sortedBy { it.first }
                }.firstOrNull()
                if (next == null) text("없음")
                else text("${if (next.first.toLocalDate() == today) "" else "${next.first.monthValue}/${next.first.dayOfMonth} "}" +
                    "%02d:%02d ".format(next.first.hour, next.first.minute) + next.second.title)
            }
            "shift.today" -> text(shiftOn(ctx, today)?.shiftTitleOn(today) ?: "없음")
            "shift.tomorrow" -> {
                val d = today.plusDays(1)
                text(shiftOn(ctx, d)?.shiftTitleOn(d) ?: "없음")
            }
            "shift.next", "shift.days_to_next" -> {
                val d = (1L..60L).map { today.plusDays(it) }
                    .firstOrNull { day -> ctx.tasks.any { it.isWorkOn(day) && it.occursOn(day) } }
                if (key == "shift.next") {
                    if (d == null) text("없음") else {
                        val s = shiftOn(ctx, d)
                        text("${d.monthValue}/${d.dayOfMonth}(${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)}) ${s?.shiftTitleOn(d).orEmpty()}".trim())
                    }
                } else if (d == null) text("-") else num(java.time.temporal.ChronoUnit.DAYS.between(today, d))
            }
            "shift.week_work" -> {
                val monday = today.with(DayOfWeek.MONDAY)
                num((0L until 7L).count { i -> val day = monday.plusDays(i); ctx.tasks.any { it.isWorkOn(day) && it.occursOn(day) } })
            }
            "shift.month_work" -> num((1..month.lengthOfMonth()).count { i ->
                val day = month.atDay(i); ctx.tasks.any { it.isWorkOn(day) && it.occursOn(day) }
            })
            "habit.done" -> num(ctx.habits.count { it.done })
            "habit.total" -> num(ctx.habits.size)
            "habit.rate" -> percent(pct(ctx.habits.count { it.done }, ctx.habits.size))
            "money.income" -> money(MoneyPrefs.book.incomeIn(month) + SalaryPrefs.total(month))
            "money.expense" -> money(MoneyPrefs.book.expenseIn(month))
            "money.balance" -> money(MoneyPrefs.book.incomeIn(month) + SalaryPrefs.total(month) - MoneyPrefs.book.expenseIn(month))
            "money.salary" -> money(SalaryPrefs.total(month))
            "money.today_expense" -> money(MoneyPrefs.book.linesIn(month).filter { it.date == today && it.signed < 0 }.sumOf { -it.signed })
            "money.daily_budget" -> {
                val left = MoneyPrefs.book.incomeIn(month) + SalaryPrefs.total(month) - MoneyPrefs.book.expenseIn(month)
                val days = month.lengthOfMonth() - today.dayOfMonth + 1
                money((left / days.coerceAtLeast(1)).coerceAtLeast(0))
            }
            "diary.today" -> text(if (ctx.diary?.content?.isNotBlank() == true) "작성함" else "아직 안 씀")
            "date.today" -> text("${today.monthValue}월 ${today.dayOfMonth}일")
            "date.weekday" -> text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN))
            "date.month_left" -> num(month.lengthOfMonth() - today.dayOfMonth)
            "date.year_rate" -> percent((today.dayOfYear - 1) * 100.0 / today.lengthOfYear())
            "time.now" -> LocalTime.now().let { text("%02d:%02d".format(it.hour, it.minute)) }
            else -> if (key.startsWith("list.left:")) {
                val id = key.removePrefix("list.left:")
                num(ctx.tasks.count { it.plain() && !it.isCompleted && it.listId == id })
            } else text("?")
        }
    }

    /** "{today.left}개 남음" → "3개 남음". Unknown keys stay as written. */
    fun fill(template: String, ctx: DashContext): String =
        Regex("\\{([a-z_.]+(?::[A-Za-z0-9_-]+)?)}").replace(template) { m ->
            val key = m.groupValues[1]
            val v = value(key, ctx)
            if (v.text == "?") m.value else v.text
        }
}
