@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moneyflow.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moneyflow.android.BuildConfig
import com.moneyflow.android.data.Appearance
import com.moneyflow.android.data.WEBSITE_URL
import com.moneyflow.android.domain.Kind

@Composable
internal fun CategoriesScreen(state: MoneyState, model: MoneyViewModel) {
    val data = state.data
    if (data == null) {
        LoadingScreen(state.refreshError, model::refresh)
        return
    }
    val kind = state.categoryKind
    val categories = data.categories(kind)
    val counts = data.entries(kind).groupingBy { it.categoryId }.eachCount()

    PullToRefreshBox(state.refreshing, model::refresh) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            if (state.refreshError != null) item { RefreshError(state.refreshError, model::refresh) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Kind.entries.forEachIndexed { index, value ->
                            SegmentedButton(
                                selected = kind == value,
                                onClick = { model.selectCategoryKind(value) },
                                shape = SegmentedButtonDefaults.itemShape(index, Kind.entries.size),
                            ) { Text(value.label) }
                        }
                    }
                    FilledTonalButton(
                        onClick = { model.askAddCategory(kind) },
                        enabled = !state.writing,
                    ) {
                        Icon(Icons.Default.Add, null)
                        Text("Add category", Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (categories.isEmpty()) item {
                StatusPanel(
                    "No ${kind.singular} categories",
                    "Create a category to organise and filter your transactions.",
                    Modifier.fillMaxWidth(),
                )
            }
            items(categories, key = { "${kind.name}/${it.id}" }) { category ->
                val count = counts[category.id] ?: 0
                ListItem(
                    supportingContent = {
                        if (count > 0) Text(if (count == 1) "1 transaction" else "$count transactions")
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { model.askDeleteCategory(kind, category) },
                            enabled = !state.writing,
                        ) { Icon(Icons.Default.Delete, "Delete ${category.name}") }
                    },
                ) { Text(category.name) }
                HorizontalDivider()
            }
        }
    }
}

@Composable
internal fun SettingsScreen(state: MoneyState, model: MoneyViewModel) {
    val uri = LocalUriHandler.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionHeading("Appearance")
            Column(Modifier.padding(top = 4.dp)) {
                Appearance.entries.forEach { item ->
                    ChoiceRow(item.label, state.settings.appearance == item) { model.appearance(item) }
                }
            }
            ListItem(
                supportingContent = { Text("Use your device’s colour palette") },
                trailingContent = {
                    Switch(
                        state.settings.dynamicColor,
                        onCheckedChange = model::dynamicColor,
                        modifier = Modifier.semantics { contentDescription = "Dynamic colour" },
                    )
                },
            ) { Text("Dynamic colour") }
        }
        item { HorizontalDivider() }
        item {
            SectionHeading("Account", Modifier.padding(top = 6.dp))
            ListItem(
                supportingContent = { Text(state.accountName.orEmpty()) },
            ) { Text("Signed in as") }
            OutlinedButton(
                onClick = model::signOut,
                enabled = !state.writing,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text("Sign out", color = if (state.writing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            }
        }
        item { HorizontalDivider() }
        item {
            SectionHeading("About", Modifier.padding(top = 6.dp))
            ListItem(
                modifier = Modifier.clickable {
                    try {
                        uri.openUri(WEBSITE_URL)
                    } catch (_: IllegalArgumentException) {
                        model.websiteUnavailable()
                    }
                },
            ) { Text("MoneyFlow website") }
            ListItem(
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            ) { Text("App version") }
        }
    }
}
