@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moneyflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneyflow.android.domain.Category
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.EntryDraft
import com.moneyflow.android.domain.EntryField
import com.moneyflow.android.domain.Kind
import com.moneyflow.android.domain.displayDate
import com.moneyflow.android.domain.methodName
import kotlinx.coroutines.flow.first

@Composable
internal fun WorkspaceOverlay(state: MoneyState, model: MoneyViewModel) {
    val dismiss = model::dismissOverlay
    val busy = state.writing || state.refreshing
    when (val overlay = state.overlay) {
        null -> Unit
        is Overlay.Editor -> key(overlay.draft.kind, overlay.draft.id) {
            val data = state.data
            EntryEditor(
                overlay, data?.categories(overlay.draft.kind).orEmpty(),
                state.writing, state.refreshing, data != null,
                state.actionError ?: state.refreshError.takeIf { data == null },
                model::refresh, dismiss, model::updateDraft, model::saveEntry,
            )
        }
        is Overlay.Details -> EntryDetails(overlay.entry, dismiss, model)
        is Overlay.DeleteEntry -> ConfirmDelete(
            "Delete ${overlay.entry.kind.singular}?",
            "${overlay.entry.name} · ${overlay.entry.signedAmount} · ${displayDate(overlay.entry.date)}",
            busy, state.actionError, dismiss,
        ) { model.deleteEntry(overlay.entry) }
        is Overlay.DeleteCategory -> ConfirmDelete(
            "Delete category?",
            "Delete “${overlay.category.name}”? Categories used by transactions must be reassigned first.",
            busy, state.actionError, dismiss,
        ) { model.deleteCategory(overlay.kind, overlay.category) }
        is Overlay.AddCategory -> {
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text("New ${overlay.kind.singular} category") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            overlay.name, model::categoryName, Modifier.fillMaxWidth(),
                            label = { Text("Name") }, singleLine = true, enabled = !state.writing,
                            isError = state.actionError != null,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (!busy) model.addCategory() }),
                        )
                        state.actionError?.let { ErrorText(it) }
                    }
                },
                confirmButton = {
                    Button(onClick = model::addCategory, enabled = !busy) {
                        Text(if (state.writing) "Saving…" else "Add")
                    }
                },
                dismissButton = { TextButton(onClick = dismiss, enabled = !state.writing) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun EntryEditor(
    editor: Overlay.Editor,
    categories: List<Category>,
    writing: Boolean,
    refreshing: Boolean,
    loaded: Boolean,
    error: String?,
    retry: () -> Unit,
    dismiss: () -> Unit,
    update: (EntryDraft) -> Unit,
    save: () -> Unit,
) {
    val draft = editor.draft
    val expense = draft.kind == Kind.Expense
    val creating = draft.id == null
    val writingNow by rememberUpdatedState(writing)
    val sheet = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { it != SheetValue.Hidden || !writingNow },
    )
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheet,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !writing),
    ) {
        val keyboard = LocalSoftwareKeyboardController.current
        val focus = LocalFocusManager.current
        val amountFocus = remember { FocusRequester() }
        if (creating) LaunchedEffect(sheet) {
            snapshotFlow { sheet.currentValue }.first { it == SheetValue.Expanded }
            amountFocus.requestFocus()
        }

        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${if (creating) "Add" else "Edit"} ${draft.kind.singular}",
                    Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                )
                IconButton(onClick = dismiss, enabled = !writing) { Icon(Icons.Default.Close, "Close editor") }
            }
            if (writing || refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    draft.amount, { update(draft.copy(amount = it)) },
                    Modifier.fillMaxWidth().focusRequester(amountFocus),
                    label = { Text("Amount") }, prefix = { Text("₹") },
                    enabled = !writing, isError = EntryField.Amount in editor.errors,
                    supportingText = editor.errors[EntryField.Amount]?.let { message -> { Text(message) } },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = if (expense) ImeAction.Next else ImeAction.Done),
                    keyboardActions = if (expense) KeyboardActions.Default else KeyboardActions(onDone = {
                        focus.clearFocus()
                        keyboard?.hide()
                    }),
                )
                if (expense) OutlinedTextField(
                    draft.title, { update(draft.copy(title = it)) }, Modifier.fillMaxWidth(),
                    label = { Text("Title") }, singleLine = true, enabled = !writing,
                    isError = EntryField.Title in editor.errors,
                    supportingText = editor.errors[EntryField.Title]?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                )
                OptionMenu(
                    "Category", categories.firstOrNull { it.id == draft.categoryId }?.name ?: "Choose a category",
                    categories, Category::name, { update(draft.copy(categoryId = it.id)) },
                    enabled = !writing && categories.isNotEmpty(),
                )
                editor.errors[EntryField.Category]?.let { ErrorText(it) }
                if (loaded && categories.isEmpty()) {
                    Text("Create a ${draft.kind.singular} category in Categories before saving.")
                }
                if (!expense || creating) {
                    OptionMenu(
                        draft.kind.methodLabel, draft.method?.let(::methodName) ?: "Choose an option",
                        draft.kind.methods, ::methodName, { update(draft.copy(method = it)) },
                        enabled = !writing,
                    )
                } else {
                    DetailField(draft.kind.methodLabel, methodName(draft.method))
                }
                editor.errors[EntryField.Method]?.let { ErrorText(it) }
                DateButton("Date", draft.date, { it?.let { date -> update(draft.copy(date = date)) } },
                    Modifier.fillMaxWidth(), enabled = !writing, clearable = false)
                OutlinedTextField(
                    draft.note, { update(draft.copy(note = it)) }, Modifier.fillMaxWidth(),
                    label = { Text("Note (optional)") }, minLines = 2, enabled = !writing,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                if (error != null) {
                    if (loaded) ErrorText(error) else RefreshError(error, retry)
                }
            }
            HorizontalDivider()
            Button(
                onClick = {
                    focus.clearFocus()
                    keyboard?.hide()
                    save()
                },
                enabled = !writing && !refreshing && categories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(bottom = 4.dp),
            ) { Text(if (writing) "Saving…" else if (creating) "Save ${draft.kind.singular}" else "Save changes") }
        }
    }
}

@Composable
private fun EntryDetails(entry: Entry, dismiss: () -> Unit, model: MoneyViewModel) {
    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeading(if (entry.kind == Kind.Expense) "Expense details" else "Income details")
            Text(entry.signedAmount, style = MaterialTheme.typography.headlineMedium)
            if (entry.title != null) Text(entry.title, style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            DetailField("Category", entry.category)
            DetailField("Date", displayDate(entry.date))
            DetailField(entry.kind.methodLabel, methodName(entry.method))
            entry.note?.takeIf { it.isNotBlank() }?.let { DetailField("Note", it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { model.openEntry(entry.kind, entry) }) { Text("Edit") }
                TextButton(onClick = { model.askDelete(entry) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = dismiss) { Text("Close") }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Box(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
