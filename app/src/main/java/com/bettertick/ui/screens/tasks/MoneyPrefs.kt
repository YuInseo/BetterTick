package com.bettertick.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

val IncomeCategories = listOf("용돈", "부수입", "상여", "환급", "이자", "선물", "기타")
val ExpenseCategories = listOf("식비", "카페", "교통", "쇼핑", "생활", "통신", "주거", "문화", "의료", "경조사", "용돈", "기타")

/** 수입·지출 한 건. */
data class MoneyEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    /** Always positive; [income] says which way it goes. */
    val amount: Long,
    val income: Boolean,
    val category: String,
    val memo: String = ""
) {
    val signed: Long get() = if (income) amount else -amount
}

/** 매달 고정 수입·지출: 용돈, 월세, 통신비, 구독 … paid on [day] (31 = 말일). */
data class RecurringMoney(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Long,
    val income: Boolean,
    val category: String,
    val day: Int = 1,
    val from: YearMonth? = null,
    val to: YearMonth? = null
) {
    fun activeIn(month: YearMonth): Boolean =
        (from == null || month >= from) && (to == null || month <= to)

    fun dateIn(month: YearMonth): LocalDate = month.atDay(day.coerceIn(1, month.lengthOfMonth()))

    val signed: Long get() = if (income) amount else -amount

    val scheduleLabel: String
        get() {
            fun ym(m: YearMonth) = "${m.year}년 ${m.monthValue}월"
            val base = if (day >= 31) "매달 말일" else "매달 ${day}일"
            return base + when {
                from != null && to != null -> " · ${ym(from)} ~ ${ym(to)}"
                from != null -> " · ${ym(from)}부터"
                to != null -> " · ${ym(to)}까지"
                else -> ""
            }
        }
}

/** A line of a month's 내역: a one-off entry or a recurring item's occurrence. */
data class MoneyLine(
    val date: LocalDate,
    val title: String,
    val category: String,
    val signed: Long,
    val entry: MoneyEntry? = null,
    val recurring: RecurringMoney? = null
)

data class MoneyBook(
    val entries: List<MoneyEntry> = emptyList(),
    val recurring: List<RecurringMoney> = emptyList()
) {
    /** Newest first; recurring items appear on their day of the month. */
    fun linesIn(month: YearMonth): List<MoneyLine> {
        val once = entries.filter { YearMonth.from(it.date) == month }.map {
            MoneyLine(it.date, it.memo.ifBlank { it.category }, it.category, it.signed, entry = it)
        }
        val fixed = recurring.filter { it.activeIn(month) }.map {
            MoneyLine(it.dateIn(month), it.name, it.category, it.signed, recurring = it)
        }
        return (once + fixed).sortedByDescending { it.date }
    }

    fun incomeIn(month: YearMonth): Long = linesIn(month).filter { it.signed > 0 }.sumOf { it.signed }
    fun expenseIn(month: YearMonth): Long = -linesIn(month).filter { it.signed < 0 }.sumOf { it.signed }

    /** 지출 by category, biggest first. */
    fun expenseByCategory(month: YearMonth): List<Pair<String, Long>> =
        linesIn(month).filter { it.signed < 0 }
            .groupBy { it.category }
            .map { (c, l) -> c to -l.sumOf { it.signed } }
            .sortedByDescending { it.second }
}

/** 가계부: 추가 수입·지출, 매달 고정 수입·지출 (용돈 등). Not tied to a workplace. */
object MoneyPrefs {
    private const val PREFS = "money_prefs"

    var book by mutableStateOf(MoneyBook())
        private set

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        book = p.getString("book", null)?.let { decode(it) } ?: MoneyBook()
    }

    private fun update(transform: (MoneyBook) -> MoneyBook) {
        book = transform(book)
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString("book", encode(book))?.apply()
    }

    fun saveEntry(e: MoneyEntry) = update { b ->
        b.copy(entries = if (b.entries.any { it.id == e.id }) b.entries.map { if (it.id == e.id) e else it } else b.entries + e)
    }

    fun deleteEntry(id: String) = update { b -> b.copy(entries = b.entries.filterNot { it.id == id }) }

    fun saveRecurring(r: RecurringMoney) = update { b ->
        b.copy(recurring = if (b.recurring.any { it.id == r.id }) b.recurring.map { if (it.id == r.id) r else it } else b.recurring + r)
    }

    fun deleteRecurring(id: String) = update { b -> b.copy(recurring = b.recurring.filterNot { it.id == id }) }

    private fun encode(b: MoneyBook): String = org.json.JSONObject().apply {
        put("entries", org.json.JSONArray().apply {
            b.entries.forEach { e ->
                put(org.json.JSONObject().apply {
                    put("id", e.id); put("date", e.date.toString()); put("amount", e.amount)
                    put("income", e.income); put("category", e.category); put("memo", e.memo)
                })
            }
        })
        put("recurring", org.json.JSONArray().apply {
            b.recurring.forEach { r ->
                put(org.json.JSONObject().apply {
                    put("id", r.id); put("name", r.name); put("amount", r.amount)
                    put("income", r.income); put("category", r.category); put("day", r.day)
                    r.from?.let { put("from", it.toString()) }
                    r.to?.let { put("to", it.toString()) }
                })
            }
        })
    }.toString()

    private fun decode(raw: String): MoneyBook? = runCatching {
        val o = org.json.JSONObject(raw)
        fun ym(s: String): YearMonth? = s.takeIf { it.isNotBlank() }?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        val ea = o.optJSONArray("entries") ?: org.json.JSONArray()
        val ra = o.optJSONArray("recurring") ?: org.json.JSONArray()
        MoneyBook(
            entries = (0 until ea.length()).mapNotNull { i ->
                val e = ea.optJSONObject(i) ?: return@mapNotNull null
                val date = runCatching { LocalDate.parse(e.optString("date")) }.getOrNull() ?: return@mapNotNull null
                MoneyEntry(
                    id = e.optString("id").ifBlank { UUID.randomUUID().toString() },
                    date = date, amount = e.optLong("amount"), income = e.optBoolean("income"),
                    category = e.optString("category", "기타"), memo = e.optString("memo")
                )
            },
            recurring = (0 until ra.length()).mapNotNull { i ->
                val r = ra.optJSONObject(i) ?: return@mapNotNull null
                RecurringMoney(
                    id = r.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = r.optString("name"), amount = r.optLong("amount"), income = r.optBoolean("income"),
                    category = r.optString("category", "기타"), day = r.optInt("day", 1),
                    from = ym(r.optString("from", "")), to = ym(r.optString("to", ""))
                )
            }
        )
    }.getOrNull()
}
