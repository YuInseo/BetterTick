package com.bettertick.ui.screens.tasks

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyBookTest {
    private val nov = YearMonth.of(2026, 11)
    private val book = MoneyBook(
        entries = listOf(
            MoneyEntry(id = "a", date = LocalDate.parse("2026-11-03"), amount = 12_000, income = false, category = "식비", memo = "점심"),
            MoneyEntry(id = "b", date = LocalDate.parse("2026-11-10"), amount = 50_000, income = true, category = "부수입"),
            MoneyEntry(id = "c", date = LocalDate.parse("2026-10-30"), amount = 9_000, income = false, category = "식비")
        ),
        recurring = listOf(
            RecurringMoney(id = "allowance", name = "용돈", amount = 300_000, income = true, category = "용돈", day = 1),
            RecurringMoney(id = "rent", name = "월세", amount = 500_000, income = false, category = "주거", day = 31, to = YearMonth.of(2026, 11))
        )
    )

    @Test fun monthHasItsEntriesAndRecurringItems() {
        val lines = book.linesIn(nov)
        assertEquals(listOf("rent", "b", "a", "allowance"), lines.map { it.entry?.id ?: it.recurring!!.id })
        assertEquals(LocalDate.parse("2026-11-30"), lines.first().date)
        assertEquals(350_000L, book.incomeIn(nov))
        assertEquals(512_000L, book.expenseIn(nov))
    }

    @Test fun recurringStopsAfterItsLastMonth() {
        val dec = YearMonth.of(2026, 12)
        assertEquals(0L, book.expenseIn(dec))
        assertEquals(300_000L, book.incomeIn(dec))
    }

    @Test fun expensesGroupByCategoryBiggestFirst() {
        assertEquals(listOf("주거" to 500_000L, "식비" to 12_000L), book.expenseByCategory(nov))
    }
}
