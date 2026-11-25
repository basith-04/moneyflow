package com.moneyflow.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneyflow.android.data.ApiFailure
import com.moneyflow.android.data.Appearance
import com.moneyflow.android.data.MoneyRepository
import com.moneyflow.android.data.Preferences
import com.moneyflow.android.data.Session
import com.moneyflow.android.data.SessionVault
import com.moneyflow.android.data.Settings
import com.moneyflow.android.domain.AccountData
import com.moneyflow.android.domain.Category
import com.moneyflow.android.domain.Dashboard
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
import com.moneyflow.android.domain.validateCategory
import com.moneyflow.android.domain.validateEntry
import com.moneyflow.android.domain.validateRegistration
import com.moneyflow.android.domain.validatedWrite
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LedgerState(
    val query: String = "",
    val filter: LedgerFilter = LedgerFilter(),
    val sort: LedgerSort = LedgerSort.Newest,
    val entries: List<Entry> = emptyList(),
)

internal data class DashboardRange(val period: Period? = Period.ThisMonth, val custom: Dates = Dates()) {
    fun dates(today: LocalDate) = period?.dates(today) ?: custom
    fun label() = period?.label ?: custom.label()
}

internal sealed interface Overlay {
    data class Editor(val draft: EntryDraft, val errors: Map<EntryField, String> = emptyMap()) : Overlay
    data class Details(val entry: Entry) : Overlay
    data class DeleteEntry(val entry: Entry) : Overlay
    data class AddCategory(val kind: Kind, val name: String = "") : Overlay
    data class DeleteCategory(val kind: Kind, val category: Category) : Overlay
}

internal data class MoneyState(
    val restoring: Boolean = true,
    val accountName: String? = null,
    val accountKey: Long = 0,
    val authBusy: Boolean = false,
    val authError: String? = null,
    val authNotice: String? = null,
    val registration: Boolean = false,
    val settings: Settings = Settings(),
    val data: AccountData? = null,
    val refreshing: Boolean = false,
    val refreshError: String? = null,
    val expenseLedger: LedgerState = LedgerState(),
    val incomeLedger: LedgerState = LedgerState(),
    val dashboardRange: DashboardRange = DashboardRange(),
    val dashboard: Dashboard? = null,
    val categoryKind: Kind = Kind.Expense,
    val overlay: Overlay? = null,
    val actionError: String? = null,
    val writing: Boolean = false,
    val message: String? = null,
) {
    fun ledger(kind: Kind) = if (kind == Kind.Expense) expenseLedger else incomeLedger
}

internal class MoneyViewModel(
    private val repository: MoneyRepository,
    private val vault: SessionVault,
    private val preferences: Preferences,
    private val clock: Clock? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MoneyState())
    val state: StateFlow<MoneyState> = mutableState.asStateFlow()
    private var session: Session? = null
    private var refreshJob: Job? = null
    private var lastRefreshMillis: Long? = null
    private var shortcutPending = false

    init {
        viewModelScope.launch { preferences.settings.collect { value -> update { copy(settings = value) } } }
        viewModelScope.launch { restore() }
    }

    private fun today() = clock?.let(LocalDate::now) ?: LocalDate.now()
    private fun nowMillis() = clock?.millis() ?: System.currentTimeMillis()
    private inline fun update(change: MoneyState.() -> MoneyState) = mutableState.update(change)

    private suspend fun restore() {
        try {
            val saved = vault.read()
            if (saved == null) update { copy(restoring = false) } else enter(saved)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            update { copy(restoring = false, authError = "Couldn’t restore your saved session. Sign in again.") }
        }
    }

    fun showRegistration(show: Boolean) {
        if (!state.value.authBusy) update { copy(registration = show, authError = null, authNotice = null) }
    }

    fun login(userName: String, password: String) {
        if (state.value.authBusy || session != null) return
        if (userName.isEmpty() || password.isEmpty()) {
            update { copy(authError = "Enter your username and password.", authNotice = null) }
            return
        }
        authenticate {
            val token = repository.login(userName, password)
            val newSession = Session(userName, token)
            vault.save(newSession)
            enter(newSession)
        }
    }

    fun register(fullName: String, userName: String, email: String, password: String) {
        if (state.value.authBusy || session != null) return
        val full = fullName.trim()
        val address = email.trim()
        val error = validateRegistration(full, userName, address, password)
        if (error != null) {
            update { copy(authError = error) }
            return
        }
        authenticate {
            repository.register(full, userName, address, password)
            update { copy(registration = false, authNotice = "Account created. Sign in to continue.") }
        }
    }

    private fun authenticate(action: suspend () -> Unit) {
        update { copy(authBusy = true, authError = null, authNotice = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (error: ApiFailure) {
                update { copy(authError = error.message) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                update { copy(authError = "Couldn’t store your session securely. Try signing in again.") }
            } finally {
                update { copy(authBusy = false) }
            }
        }
    }

    private fun enter(value: Session) {
        session = value
        update {
            MoneyState(
                restoring = false, accountName = value.userName, accountKey = accountKey + 1,
                settings = settings,
            )
        }
        openPendingShortcut()
        refresh()
    }

    fun signOut() {
        val current = session ?: return
        if (state.value.writing) return
        refreshJob?.cancel()
        update { copy(writing = true, refreshing = false) }
        viewModelScope.launch {
            try {
                vault.clear()
                leave()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (session === current) update { copy(writing = false, message = "Couldn’t remove the saved session. Try again.") }
            }
        }
    }

    private fun leave(notice: String? = null) {
        session = null
        lastRefreshMillis = null
        shortcutPending = false
        update { MoneyState(restoring = false, accountKey = accountKey, settings = settings, authNotice = notice) }
    }

    private suspend fun expire(expected: Session) {
        if (session !== expected) return
        val notice = ApiFailure.Expired().message
        val cleared = try {
            vault.clear()
            true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            false
        }
        leave(if (cleared) notice else "$notice Couldn’t remove the saved session.")
    }

    fun foreground() {
        if (session == null) return
        update { copy(dashboard = data?.let { dashboard(it, dashboardRange.dates(today()), today()) }) }
        val since = lastRefreshMillis
        if (since == null || nowMillis() - since >= 30_000L) refresh()
    }

    fun refresh() {
        val current = session ?: return
        if (state.value.refreshing || state.value.writing) return
        update { copy(refreshing = true, refreshError = null) }
        refreshJob = viewModelScope.launch { load(current) }
    }

    private suspend fun load(current: Session) {
        try {
            val snapshot = repository.load(current.token)
            if (session !== current) return
            lastRefreshMillis = nowMillis()
            update {
                copy(
                    data = snapshot,
                    dashboard = dashboard(snapshot, dashboardRange.dates(today()), today()),
                    expenseLedger = expenseLedger.withEntries(snapshot.expenses),
                    incomeLedger = incomeLedger.withEntries(snapshot.incomes),
                )
            }
        } catch (_: ApiFailure.Expired) {
            expire(current)
        } catch (error: ApiFailure) {
            if (session === current) update { copy(refreshError = error.message) }
        } finally {
            if (session === current) update { copy(refreshing = false) }
        }
    }

    fun dashboardRange(value: DashboardRange) = update {
        val today = today()
        copy(dashboardRange = value, dashboard = data?.let { dashboard(it, value.dates(today), today) })
    }

    fun search(kind: Kind, value: String) = ledger(kind) { copy(query = value) }
    fun filter(kind: Kind, value: LedgerFilter) = ledger(kind) { copy(filter = value) }
    fun sort(kind: Kind, value: LedgerSort) = ledger(kind) { copy(sort = value) }
    fun selectCategoryKind(kind: Kind) = update { copy(categoryKind = kind) }

    private fun ledger(kind: Kind, change: LedgerState.() -> LedgerState) = update {
        val next = ledger(kind).change().withEntries(data?.entries(kind).orEmpty())
        if (kind == Kind.Expense) copy(expenseLedger = next) else copy(incomeLedger = next)
    }

    private fun LedgerState.withEntries(source: List<Entry>) =
        copy(entries = filterLedger(source, filter, query, sort))

    fun requestExpenseShortcut() {
        shortcutPending = true
        openPendingShortcut()
    }

    private fun openPendingShortcut() {
        if (shortcutPending && session != null && !state.value.writing) {
            shortcutPending = false
            openEntry(Kind.Expense)
        }
    }

    fun openEntry(kind: Kind, original: Entry? = null) {
        val prior = state.value.settings.lastPayment?.takeIf { it in Kind.Expense.methods }
        val draft = EntryDraft(
            kind, original?.date ?: today(), original?.id, original?.amount?.toPlainString().orEmpty(),
            original?.title.orEmpty(), original?.categoryId,
            if (original != null) original.method else prior.takeIf { kind == Kind.Expense },
            original?.note.orEmpty(),
        )
        show(Overlay.Editor(draft))
    }

    fun updateDraft(value: EntryDraft) {
        val editor = state.value.overlay as? Overlay.Editor ?: return
        if (state.value.writing) return
        val errors = if (editor.errors.isEmpty()) emptyMap() else
            validateEntry(value, state.value.data?.categories(value.kind).orEmpty())
        update { copy(overlay = Overlay.Editor(value, errors), actionError = null) }
    }

    fun saveEntry() {
        val draft = (state.value.overlay as? Overlay.Editor)?.draft ?: return
        val snapshot = state.value.data ?: return
        val errors = validateEntry(draft, snapshot.categories(draft.kind))
        if (errors.isNotEmpty()) {
            update { copy(overlay = Overlay.Editor(draft, errors), actionError = null) }
            return
        }
        val initial = draft.id == null
        mutate(
            success = if (initial) "Transaction added" else "Changes saved",
            afterAcknowledgment = {
                if (initial && draft.kind == Kind.Expense) {
                    try {
                        preferences.lastPayment(draft.method)
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                    }
                }
            },
        ) { token -> repository.saveEntry(token, draft.validatedWrite()) }
    }

    fun showDetails(entry: Entry) = show(Overlay.Details(entry))
    fun askDelete(entry: Entry) = show(Overlay.DeleteEntry(entry))
    fun askAddCategory(kind: Kind) = show(Overlay.AddCategory(kind))
    fun askDeleteCategory(kind: Kind, category: Category) = show(Overlay.DeleteCategory(kind, category))

    private fun show(value: Overlay?) {
        if (!state.value.writing) update { copy(overlay = value, actionError = null) }
    }

    fun dismissOverlay() = show(null)

    fun deleteEntry(entry: Entry) {
        mutate("Transaction deleted") { repository.deleteEntry(it, entry) }
    }

    fun categoryName(value: String) {
        val dialog = state.value.overlay as? Overlay.AddCategory ?: return
        if (!state.value.writing) update { copy(overlay = dialog.copy(name = value), actionError = null) }
    }

    fun addCategory() {
        val dialog = state.value.overlay as? Overlay.AddCategory ?: return
        val snapshot = state.value.data ?: return
        val error = validateCategory(dialog.name, snapshot.categories(dialog.kind))
        if (error != null) {
            update { copy(actionError = error) }
            return
        }
        mutate("Category added") { repository.addCategory(it, dialog.kind, dialog.name.trim()) }
    }

    fun deleteCategory(kind: Kind, category: Category) {
        if (state.value.data?.entries(kind)?.any { it.categoryId == category.id } == true) {
            update { copy(actionError = "This category is used by transactions. Change those transactions first.") }
            return
        }
        mutate("Category deleted") { repository.deleteCategory(it, kind, category.id) }
    }

    private fun mutate(
        success: String,
        afterAcknowledgment: suspend () -> Unit = {},
        operation: suspend (String) -> Unit,
    ) {
        val current = session ?: return
        if (state.value.writing || state.value.refreshing) return
        update { copy(writing = true, actionError = null) }
        viewModelScope.launch {
            try {
                operation(current.token)
                if (session !== current) return@launch
                update { copy(overlay = null, message = success, refreshing = true, refreshError = null) }
                afterAcknowledgment()
                load(current)
            } catch (_: ApiFailure.Expired) {
                expire(current)
            } catch (error: ApiFailure) {
                if (session === current) {
                    val failure = if (error is ApiFailure.Network || error is ApiFailure.InvalidResponse) {
                        "${error.message} The change couldn’t be confirmed. Refresh and check before trying again."
                    } else {
                        error.message
                    }
                    update { copy(actionError = failure) }
                }
            } finally {
                if (session === current) {
                    update { copy(writing = false) }
                    openPendingShortcut()
                }
            }
        }
    }

    fun appearance(value: Appearance) = changePreference { preferences.appearance(value) }
    fun dynamicColor(value: Boolean) = changePreference { preferences.dynamicColor(value) }

    private fun changePreference(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                update { copy(message = "Couldn’t save your preference. Try again.") }
            }
        }
    }

    fun websiteUnavailable() = update { copy(message = "Couldn’t open the MoneyFlow website.") }

    fun consumeMessage(value: String) = update { if (message == value) copy(message = null) else this }
}
