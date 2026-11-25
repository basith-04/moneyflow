package com.moneyflow.android.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Kind(val label: String, val singular: String, val methodLabel: String, val methods: List<String>) {
    Expense("Expenses", "expense", "Payment method", listOf("Credit Card", "upi", "cash")),
    Income("Income", "income", "Credited to", listOf("Bank Account", "Cash"));

    val categoryType: String? get() = if (this == Income) "income" else null
}

fun methodName(value: String?): String = when (value) {
    "upi" -> "UPI"
    "cash" -> "Cash"
    null, "" -> "Unspecified"
    else -> value
}

data class Category(val id: Int, val name: String)

data class Entry(
    val kind: Kind,
    val id: Int,
    val amount: BigDecimal,
    val date: LocalDate,
    val categoryId: Int,
    val category: String,
    val title: String? = null,
    val method: String? = null,
    val note: String? = null,
) {
    val key: String get() = "${kind.name}/$id"
    val name: String get() = title ?: category
    val signedAmount: String get() = (if (kind == Kind.Income) "+" else "−") + money(amount)
}

data class AccountData(
    val expenses: List<Entry>,
    val incomes: List<Entry>,
    val expenseCategories: List<Category>,
    val incomeCategories: List<Category>,
) {
    fun entries(kind: Kind) = if (kind == Kind.Expense) expenses else incomes
    fun categories(kind: Kind) = if (kind == Kind.Expense) expenseCategories else incomeCategories
}

data class Dates(val from: LocalDate? = null, val to: LocalDate? = null) {
    init {
        require(from == null || to == null || from <= to)
    }

    operator fun contains(date: LocalDate): Boolean =
        (from == null || date >= from) && (to == null || date <= to)

    fun label(): String = when {
        from != null && to != null -> "${displayDate(from)} – ${displayDate(to)}"
        from != null -> "From ${displayDate(from)}"
        to != null -> "Through ${displayDate(to)}"
        else -> "All dates"
    }
}

enum class Period(val label: String) {
    ThisMonth("This month"), LastMonth("Last month"), ThisYear("This year"), All("All dates");

    fun dates(today: LocalDate): Dates {
        val month = YearMonth.from(today)
        return when (this) {
            ThisMonth -> Dates(month.atDay(1), month.atEndOfMonth())
            LastMonth -> month.minusMonths(1).let { Dates(it.atDay(1), it.atEndOfMonth()) }
            ThisYear -> Dates(today.withDayOfYear(1), LocalDate.of(today.year, 12, 31))
            All -> Dates()
        }
    }
}

fun pickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun pickerDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("en-IN"))
fun displayDate(date: LocalDate): String = date.format(dayFormat)

fun money(amount: BigDecimal): String {
    val (integer, fraction) = amount.abs().setScale(2, RoundingMode.HALF_UP).toPlainString().split('.')
    val leading = integer.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
    val grouped = if (leading.isEmpty()) integer else "$leading,${integer.takeLast(3)}"
    val sign = if (amount.signum() < 0) "−" else ""
    return "${sign}₹$grouped.$fraction"
}

enum class LedgerSort(val label: String) {
    Newest("Newest first"), Oldest("Oldest first"), Highest("Highest amount"), Lowest("Lowest amount");

    val byDate get() = this == Newest || this == Oldest
}

private val newest = compareByDescending<Entry> { it.date }.thenByDescending { it.id }.thenBy { it.kind }

data class LedgerFilter(
    val dates: Dates = Dates(),
    val categoryId: Int? = null,
    val method: String? = null,
    val minimum: BigDecimal? = null,
    val maximum: BigDecimal? = null,
) {
    fun includes(entry: Entry): Boolean = entry.date in dates &&
        (categoryId == null || entry.categoryId == categoryId) &&
        (method == null || method == entry.method) &&
        (minimum == null || entry.amount >= minimum) &&
        (maximum == null || entry.amount <= maximum)
}

fun filterLedger(
    entries: List<Entry>,
    filter: LedgerFilter,
    query: String,
    sort: LedgerSort,
): List<Entry> {
    val term = query.trim()
    val comparator = when (sort) {
        LedgerSort.Newest -> newest
        LedgerSort.Oldest -> newest.reversed()
        LedgerSort.Highest -> compareByDescending<Entry> { it.amount }.then(newest)
        LedgerSort.Lowest -> compareBy<Entry> { it.amount }.then(newest)
    }
    return entries.asSequence()
        .filter(filter::includes)
        .filter { entry ->
            term.isEmpty() || listOf(entry.name, entry.category, entry.note.orEmpty(), methodName(entry.method))
                .any { it.contains(term, ignoreCase = true) }
        }
        .sortedWith(comparator)
        .toList()
}

data class Total(val label: String, val amount: BigDecimal)

data class Dashboard(
    val income: BigDecimal,
    val expenses: BigDecimal,
    val categories: List<Total>,
    val payments: List<Total>,
    val trend: List<Total>,
    val recent: List<Entry>,
) {
    val net: BigDecimal get() = income - expenses
}

fun dashboard(data: AccountData, dates: Dates, today: LocalDate): Dashboard {
    val expenses = data.expenses.filter { it.date in dates }
    val incomes = data.incomes.filter { it.date in dates }
    val lastMonth = YearMonth.from(minOf(dates.to ?: today, today))
    val monthFormat = DateTimeFormatter.ofPattern("MMM uuuu", Locale.forLanguageTag("en-IN"))
    val byMonth = data.expenses.groupBy { YearMonth.from(it.date) }
    val trend = if ((0L..5L).any { lastMonth.minusMonths(it) in byMonth }) {
        (5L downTo 0L).map { index ->
            val month = lastMonth.minusMonths(index)
            Total(month.format(monthFormat), byMonth[month].orEmpty().sumOf(Entry::amount))
        }
    } else {
        emptyList()
    }
    return Dashboard(
        income = incomes.sumOf(Entry::amount),
        expenses = expenses.sumOf(Entry::amount),
        categories = expenses.totals(Entry::category),
        payments = expenses.totals { methodName(it.method) },
        trend = trend,
        recent = (expenses + incomes).sortedWith(newest).take(6),
    )
}

private fun List<Entry>.totals(key: (Entry) -> String): List<Total> =
    groupBy(key).map { (name, items) -> Total(name, items.sumOf(Entry::amount)) }
        .sortedWith(compareByDescending<Total> { it.amount }.thenBy { it.label })
