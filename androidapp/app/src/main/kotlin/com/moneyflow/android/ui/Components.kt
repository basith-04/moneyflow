@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.moneyflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.moneyflow.android.domain.displayDate
import com.moneyflow.android.domain.pickerDate
import com.moneyflow.android.domain.pickerMillis
import java.time.LocalDate

@Composable
internal fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

@Composable
internal fun StatusPanel(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    action: () -> Unit = {},
) {
    Column(
        modifier.padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeading(title)
        if (body != null) Text(
            body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null) OutlinedButton(onClick = action) { Text(actionLabel) }
    }
}

@Composable
internal fun LoadingScreen(error: String?, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (error == null) {
            LoadingIndicator()
        } else {
            StatusPanel("Couldn’t load MoneyFlow", error, actionLabel = "Retry", action = retry)
        }
    }
}

@Composable
internal fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable
internal fun RefreshError(text: String, retry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = retry) { Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
internal fun <T> OptionMenu(
    label: String,
    current: String,
    options: List<T>,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(current, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option), modifier = Modifier.widthIn(max = 280.dp)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
internal fun DateButton(
    label: String,
    date: LocalDate?,
    onSelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clearable: Boolean = true,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { picking = true },
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(date?.let(::displayDate) ?: "Any date", style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
    if (picking) {
        val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.let(::pickerMillis),
            initialDisplayedMonthMillis = date?.let(::pickerMillis),
            initialDisplayMode = if (windowHeight < 480.dp) DisplayMode.Input else DisplayMode.Picker,
            yearRange = minOf(1, date?.year ?: 1)..maxOf(9999, date?.year ?: 9999),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                Button(
                    onClick = {
                        state.selectedDateMillis?.let { onSelect(pickerDate(it)) }
                        picking = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text("Select") }
            },
            dismissButton = {
                if (clearable && date != null) {
                    TextButton(onClick = {
                        onSelect(null)
                        picking = false
                    }) { Text("Clear") }
                }
                TextButton(onClick = { picking = false }) { Text("Cancel") }
            },
        ) { DatePicker(state, modifier = Modifier.verticalScroll(rememberScrollState())) }
    }
}

@Composable
internal fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick = null)
        Text(label)
    }
}

@Composable
internal fun ConfirmDelete(
    title: String,
    description: String,
    busy: Boolean,
    error: String?,
    dismiss: () -> Unit,
    delete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) dismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description)
                if (error != null) ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = delete, enabled = !busy) {
                Text(if (busy) "Deleting…" else "Delete", color = if (busy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } },
    )
}
