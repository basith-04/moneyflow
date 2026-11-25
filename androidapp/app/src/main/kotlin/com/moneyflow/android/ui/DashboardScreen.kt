@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moneyflow.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneyflow.android.domain.Dashboard
import com.moneyflow.android.domain.Dates
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.Period
import com.moneyflow.android.domain.Total
import com.moneyflow.android.domain.money
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

@Composable
internal fun DashboardScreen(state: MoneyState, model: MoneyViewModel) {
    val summary = state.dashboard
    if (summary == null) {
        LoadingScreen(state.refreshError, model::refresh)
        return
    }

    var menu by rememberSaveable { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf(false) }
    val range = state.dashboardRange

    PullToRefreshBox(state.refreshing, model::refresh) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.refreshError != null) item {
                RefreshError(state.refreshError, model::refresh)
            }
            item {
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { menu = true },
                        label = { Text(range.label()) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Period.entries.forEach { period ->
                            DropdownMenuItem(text = { Text(period.label) }, onClick = {
                                menu = false
                                model.dashboardRange(DashboardRange(period))
                            })
                        }
                        DropdownMenuItem(text = { Text("Custom range") }, onClick = {
                            menu = false
                            custom = true
                        })
                    }
                }
            }
            item { CashFlowSummary(summary) }
            if (summary.recent.isEmpty()) item {
                StatusPanel(
                    "No transactions in this range",
                    "Choose another date range or add a transaction.",
                    Modifier.fillMaxWidth(),
                )
            }
            item { SpendingTrend(summary.trend) }
            item {
                BoxWithConstraints {
                    if (maxWidth >= 640.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            SpendingBreakdown("Spending by category", summary.categories, summary.expenses, Modifier.weight(1f))
                            SpendingBreakdown("Payment methods", summary.payments, summary.expenses, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            SpendingBreakdown("Spending by category", summary.categories, summary.expenses)
                            SpendingBreakdown("Payment methods", summary.payments, summary.expenses)
                        }
                    }
                }
            }
            if (summary.recent.isNotEmpty()) {
                item { SectionHeading("Recent transactions", Modifier.padding(top = 8.dp)) }
                items(summary.recent, key = { it.key }) { entry ->
                    EntryRow(entry, showDate = true, onClick = { model.showDetails(entry) })
                }
            }
        }
    }
    if (custom) {
        DatesDialog(range.dates(LocalDate.now()), { custom = false }) {
            model.dashboardRange(DashboardRange(null, it))
            custom = false
        }
    }
}

@Composable
internal fun CashFlowSummary(summary: Dashboard) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeading("Net cash flow")
                Text(money(summary.net), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = colors.onPrimaryContainer.copy(alpha = 0.25f))
            BoxWithConstraints {
                val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                if (maxWidth < 290.dp || fontScale >= 1.35f) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("Income", summary.income)
                        Metric("Expenses", summary.expenses)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Income", summary.income, Modifier.weight(1f))
                        Metric("Expenses", summary.expenses, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, amount: BigDecimal, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(money(amount), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SpendingTrend(trend: List<Total>) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading("Spending trend")
        Text("Six months through the selected end date", style = MaterialTheme.typography.bodyMedium)
        if (trend.isEmpty()) {
            Text("No expenses in this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        val fill = MaterialTheme.colorScheme.primary
        val baseline = MaterialTheme.colorScheme.outlineVariant
        val max = trend.maxOf(Total::amount)
        Canvas(
            Modifier.fillMaxWidth().height(96.dp).semantics {
                contentDescription = trend.joinToString(". ") { "${it.label}, ${money(it.amount)}" }
            },
        ) {
            val slot = size.width / trend.size
            val width = slot * 0.5f
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            trend.forEachIndexed { index, total ->
                val height = ratio(total.amount, max) * size.height
                if (height > 0f) drawRoundRect(
                    color = fill,
                    topLeft = Offset(index * slot + (slot - width) / 2f, size.height - height),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            trend.forEach { total ->
                Text(
                    total.label.substringBefore(' '), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                )
            }
        }
        var expanded by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide monthly amounts" else "Show monthly amounts")
        }
        if (expanded) {
            trend.forEach { total -> BreakdownAmount(total) }
        }
    }
}

@Composable
private fun SpendingBreakdown(title: String, values: List<Total>, total: BigDecimal, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading(title)
        if (values.isEmpty()) {
            Text("No expenses in this range.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val fills = MaterialTheme.colorScheme
            var all by rememberSaveable { mutableStateOf(false) }
            (if (all) values else values.take(5)).forEach { line ->
                BreakdownAmount(line)
                Box(Modifier.fillMaxWidth().height(4.dp).background(fills.surfaceContainerHighest, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(ratio(line.amount, total)).height(4.dp).background(fills.primary, RoundedCornerShape(2.dp)))
                }
            }
            if (values.size > 5) TextButton(onClick = { all = !all }) {
                Text(if (all) "Show fewer" else "Show all ${values.size}")
            }
        }
    }
}

@Composable
private fun BreakdownAmount(total: Total) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(total.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(money(total.amount), style = MaterialTheme.typography.labelLarge)
    }
}

private fun ratio(value: BigDecimal, max: BigDecimal): Float =
    if (max.signum() <= 0) 0f else value.divide(max, MathContext.DECIMAL64).toFloat().coerceIn(0f, 1f)

@Composable
private fun DatesDialog(initial: Dates, dismiss: () -> Unit, apply: (Dates) -> Unit) {
    var from by rememberSaveable { mutableStateOf(initial.from?.toString()) }
    var to by rememberSaveable { mutableStateOf(initial.to?.toString()) }
    val first = from?.let(LocalDate::parse)
    val last = to?.let(LocalDate::parse)
    val valid = first == null || last == null || first <= last
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Date range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateButton("From", first, { from = it?.toString() }, Modifier.fillMaxWidth())
                DateButton("Through", last, { to = it?.toString() }, Modifier.fillMaxWidth())
                if (!valid) ErrorText("The start date must be on or before the end date.")
            }
        },
        confirmButton = { Button(onClick = { apply(Dates(first, last)) }, enabled = valid) { Text("Apply") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}
