@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moneyflow.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource
import com.moneyflow.android.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneyflow.android.domain.Category
import com.moneyflow.android.domain.Dates
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.Kind
import com.moneyflow.android.domain.LedgerFilter
import com.moneyflow.android.domain.LedgerSort
import com.moneyflow.android.domain.displayDate
import com.moneyflow.android.domain.methodName
import com.moneyflow.android.domain.parseAmount
import java.time.LocalDate

@Composable
internal fun LedgerScreen(kind: Kind, state: MoneyState, model: MoneyViewModel) {
    val data = state.data
    if (data == null) {
        LoadingScreen(state.refreshError, model::refresh)
        return
    }
    val ledger = state.ledger(kind)
    val categories = data.categories(kind)
    val methods = (kind.methods + data.entries(kind).mapNotNull(Entry::method)).filter { it.isNotBlank() }.distinct()
    var filters by rememberSaveable { mutableStateOf(false) }
    var sortMenu by rememberSaveable { mutableStateOf(false) }
    val active = ledger.filter != LedgerFilter()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current

    PullToRefreshBox(state.refreshing, model::refresh) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        ) {
            if (state.refreshError != null) item { RefreshError(state.refreshError, model::refresh) }
            item("search") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = ledger.query,
                        onValueChange = { model.search(kind, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search ${kind.label.lowercase()}") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (ledger.query.isNotEmpty()) IconButton(onClick = { model.search(kind, "") }) {
                                Icon(Icons.Default.Close, "Clear search")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focus.clearFocus()
                            keyboard?.hide()
                        }),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = active,
                            onClick = {
                                focus.clearFocus()
                                filters = true
                            },
                            label = { Text(if (active) "Filters applied" else "Filters") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        )
                        Box {
                            FilterChip(
                                selected = ledger.sort != LedgerSort.Newest,
                                onClick = {
                                    focus.clearFocus()
                                    sortMenu = true
                                },
                                label = { Text(ledger.sort.label) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            )
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                LedgerSort.entries.forEach { option ->
                                    DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                        model.sort(kind, option)
                                        sortMenu = false
                                    })
                                }
                            }
                        }
                    }
                }
            }
            if (ledger.entries.isEmpty()) {
                item {
                    val searching = ledger.query.isNotBlank() || active
                    StatusPanel(
                        title = if (searching) "No matching transactions" else "No ${kind.label.lowercase()} yet",
                        body = if (searching) "Try a different search or clear your filters." else "Add a transaction to start tracking your ${kind.label.lowercase()}.",
                        modifier = Modifier.fillMaxWidth(),
                        actionLabel = if (searching) "Clear search and filters" else null,
                        action = {
                            model.search(kind, "")
                            model.filter(kind, LedgerFilter())
                        },
                    )
                }
            } else {
                itemsIndexed(ledger.entries, key = { _, entry -> entry.key }) { index, entry ->
                    Column {
                        if (ledger.sort.byDate && (index == 0 || ledger.entries[index - 1].date != entry.date)) {
                            SectionHeading(displayDate(entry.date), Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp))
                        }
                        EntryRow(entry, showDate = !ledger.sort.byDate, onClick = { model.showDetails(entry) })
                    }
                }
            }
        }
    }
    if (filters) FilterSheet(kind, ledger.filter, categories, methods, { filters = false }) {
        model.filter(kind, it)
        filters = false
    }
}

@Composable
internal fun EntryRow(entry: Entry, showDate: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val income = entry.kind == Kind.Income
    val detail = listOfNotNull(
        entry.category.takeIf { it != entry.name },
        methodName(entry.method),
        displayDate(entry.date).takeIf { showDate },
    ).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "View ${entry.kind.singular}", onClick = onClick)
            .heightIn(min = 64.dp).padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = if (income) colors.secondaryContainer else colors.tertiaryContainer,
            contentColor = if (income) colors.onSecondaryContainer else colors.onTertiaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                painterResource(if (income) R.drawable.ic_income else R.drawable.ic_expense),
                contentDescription = if (income) "Income" else "Expense",
                modifier = Modifier.padding(8.dp).size(20.dp),
            )
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            if (maxWidth < 225.dp || LocalDensity.current.fontScale > 1.25f) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    EntryRowText(entry, detail)
                    Text(entry.signedAmount, style = MaterialTheme.typography.titleSmall)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { EntryRowText(entry, detail) }
                    Text(entry.signedAmount, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun EntryRowText(entry: Entry, detail: String) {
    Text(entry.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text(
        detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
    if (!entry.note.isNullOrBlank()) {
        Text(
            entry.note, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FilterSheet(
    kind: Kind,
    initial: LedgerFilter,
    categories: List<Category>,
    methods: List<String>,
    dismiss: () -> Unit,
    apply: (LedgerFilter) -> Unit,
) {
    var from by rememberSaveable { mutableStateOf(initial.dates.from?.toString()) }
    var to by rememberSaveable { mutableStateOf(initial.dates.to?.toString()) }
    var category by rememberSaveable { mutableStateOf(initial.categoryId) }
    var method by rememberSaveable { mutableStateOf(initial.method) }
    var minimum by rememberSaveable { mutableStateOf(initial.minimum?.toPlainString().orEmpty()) }
    var maximum by rememberSaveable { mutableStateOf(initial.maximum?.toPlainString().orEmpty()) }
    val start = from?.let(LocalDate::parse)
    val end = to?.let(LocalDate::parse)
    val min = parseAmount(minimum)
    val max = parseAmount(maximum)
    val dateError = start != null && end != null && start > end
    val minError = minimum.isNotBlank() && min == null
    val maxError = maximum.isNotBlank() && max == null
    val amountError = min != null && max != null && min > max

    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeading("Filter ${kind.label.lowercase()}")
            DateButton("From", start, { from = it?.toString() }, Modifier.fillMaxWidth())
            DateButton("Through", end, { to = it?.toString() }, Modifier.fillMaxWidth())
            if (dateError) ErrorText("The start date must be on or before the end date.")
            OptionMenu(
                "Category", categories.firstOrNull { it.id == category }?.name ?: "All categories",
                listOf<Category?>(null) + categories, { it?.name ?: "All categories" }, { category = it?.id },
            )
            OptionMenu(
                kind.methodLabel, method?.let(::methodName) ?: "All", listOf<String?>(null) + methods,
                { it?.let(::methodName) ?: "All" }, { method = it },
            )
            OutlinedTextField(
                minimum, { minimum = it }, Modifier.fillMaxWidth(), label = { Text("Minimum amount") },
                singleLine = true, isError = minError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                maximum, { maximum = it }, Modifier.fillMaxWidth(), label = { Text("Maximum amount") },
                singleLine = true, isError = maxError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            )
            if (minError || maxError) ErrorText("Enter a valid amount with up to two decimal places.")
            if (amountError) ErrorText("The minimum must be less than or equal to the maximum.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { apply(LedgerFilter(Dates(start, end), category, method, min, max)) },
                    enabled = !dateError && !minError && !maxError && !amountError,
                ) { Text("Apply filters") }
                TextButton(onClick = { apply(LedgerFilter()) }) { Text("Clear filters") }
            }
        }
    }
}
