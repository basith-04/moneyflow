package com.moneyflow.android

import com.moneyflow.android.domain.AccountData
import com.moneyflow.android.domain.Category
import com.moneyflow.android.domain.Dates
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.EntryDraft
import com.moneyflow.android.domain.EntryField
import com.moneyflow.android.domain.Kind
import com.moneyflow.android.domain.LedgerFilter
import com.moneyflow.android.domain.LedgerSort
import com.moneyflow.android.domain.Period
import com.moneyflow.android.domain.dashboard
import com.moneyflow.android.domain.filterLedger
import com.moneyflow.android.domain.money
import com.moneyflow.android.domain.parseAmount
import com.moneyflow.android.domain.pickerDate
import com.moneyflow.android.domain.pickerMillis
import com.moneyflow.android.domain.validateCategory
import com.moneyflow.android.domain.validateEntry
import com.moneyflow.android.domain.validatedWrite
import java.math.BigDecimal
import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceTest {
    private val day = LocalDate.of(2024, 2, 29)
    private val food = Category(5, "Groceries")
    private val base = Entry(Kind.Expense, 1, BigDecimal("0.10"), day, food.id, food.name, "Market", "upi", "Lunch")

    @Test fun dashboardSumsExactlyAndAppliesInclusiveRange() {
        val expenses = listOf(
            base,
            base.copy(id = 2, amount = BigDecimal("0.20"), date = day.minusDays(1), method = null),
            base.copy(id = 3, amount = BigDecimal("29.99"), date = day.minusMonths(1)),
        )
        val income = base.copy(kind = Kind.Income, amount = BigDecimal("1.01"))
        val data = AccountData(expenses, listOf(income), listOf(food), emptyList())
        val summary = dashboard(data, Dates(day.minusDays(1), day), day)

        assertEquals(BigDecimal("0.30"), summary.expenses)
        assertEquals(BigDecimal("1.01"), summary.income)
        assertEquals(BigDecimal("0.71"), summary.net)
        assertEquals(BigDecimal("0.30"), summary.categories.single().amount)
        assertEquals(listOf("Unspecified", "UPI"), summary.payments.map { it.label })
        assertEquals(listOf(BigDecimal("29.99"), BigDecimal("0.30")), summary.trend.takeLast(2).map { it.amount })
        assertEquals(3, summary.recent.size)
    }

    @Test fun sortsAndFiltersByCategoryMethodDatesAmountAndNote() {
        val options = listOf(
            base,
            base.copy(id = 2, amount = BigDecimal("9.00")),
            base.copy(id = 3, amount = BigDecimal("2.00"), method = "cash"),
            base.copy(id = 4, amount = BigDecimal("8.00"), date = day.plusDays(1)),
        )
        val filters = LedgerFilter(Dates(day, day), food.id, "upi", BigDecimal("0.10"), BigDecimal("9.00"))
        val result = filterLedger(options, filters, "  LUNCH ", LedgerSort.Highest)
        assertEquals(listOf(2, 1), result.map { it.id })
        assertTrue(filterLedger(options, filters, "nothing matches", LedgerSort.Newest).isEmpty())
        assertEquals(listOf(4, 3, 2, 1), filterLedger(options, LedgerFilter(), "groceries", LedgerSort.Newest).map { it.id })
    }

    @Test fun formatsIndianCurrencyWithoutFloatingPoint() {
        assertEquals("₹0.00", money(BigDecimal.ZERO))
        assertEquals("₹999.99", money(BigDecimal("999.99")))
        assertEquals("₹1,000.00", money(BigDecimal("1000")))
        assertEquals("−₹12,34,567.85", money(BigDecimal("-1234567.845")))
        assertEquals("₹9,99,99,99,999.99", money(BigDecimal("9999999999.99")))
    }

    @Test fun preservesCalendarDatesAcrossTimezonesAndLeapYears() {
        val original = TimeZone.getDefault()
        try {
            listOf("Asia/Kolkata", "Pacific/Pago_Pago", "Pacific/Kiritimati").forEach {
                TimeZone.setDefault(TimeZone.getTimeZone(it))
                assertEquals(day, pickerDate(pickerMillis(day)))
            }
        } finally {
            TimeZone.setDefault(original)
        }
        assertEquals(Dates(LocalDate.of(2024, 2, 1), day), Period.LastMonth.dates(day.plusDays(1)))
        assertTrue(day in Dates(day, day))
        assertFalse(day.minusDays(1) in Dates(day, day))
    }

    @Test fun validatesNewTransactionsAndAcceptsLegacyZeroOnEdit() {
        val draft = EntryDraft(Kind.Expense, day, amount = "0", title = "  ", categoryId = 7)
        assertEquals(setOf(EntryField.Amount, EntryField.Title, EntryField.Category, EntryField.Method), validateEntry(draft, listOf(food)).keys)

        val legacy = draft.copy(id = 4, title = " Old expense ", categoryId = food.id)
        assertTrue(validateEntry(legacy, listOf(food)).isEmpty())
        assertEquals("Old expense", legacy.validatedWrite().title)
        assertNull(legacy.validatedWrite().note)

        val income = draft.copy(kind = Kind.Income, amount = "1,05", categoryId = food.id, method = "Bank Account")
        assertTrue(validateEntry(income, listOf(food)).isEmpty())
        assertEquals(BigDecimal("1.05"), income.validatedWrite().amount)
        assertEquals(setOf(EntryField.Method), validateEntry(income.copy(method = "UPI"), listOf(food)).keys)
    }

    @Test fun validatesDatabaseLimitsAndCategoryNames() {
        assertNull(parseAmount("1.009"))
        assertNull(parseAmount("10000000000"))
        assertNull(parseAmount("1e2"))
        assertNull(parseAmount("-1"))
        assertEquals(BigDecimal("0.05"), parseAmount(".05"))
        assertEquals(BigDecimal("123.45"), parseAmount("१२३٫४५"))
        assertEquals(BigDecimal("9999999999.99"), parseAmount("9999999999.99"))
        assertEquals("That category already exists.", validateCategory(" Groceries ", listOf(food)))
        assertEquals("Use at most 120 characters.", validateCategory("x".repeat(121), listOf(food)))
        assertNull(validateCategory("Transport", listOf(food)))
    }
}
