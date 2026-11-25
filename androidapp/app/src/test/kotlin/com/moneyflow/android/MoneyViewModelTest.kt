package com.moneyflow.android

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
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.EntryWrite
import com.moneyflow.android.domain.Kind
import com.moneyflow.android.ui.MoneyViewModel
import com.moneyflow.android.ui.Overlay
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val vault = FakeVault()
    private val preferences = FakePreferences()
    private lateinit var model: MoneyViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        if (::model.isInitialized) model.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test fun logsInPersistsSessionAndClearsDataOnSignOut() = runTest {
        createModel()
        advanceUntilIdle()
        assertFalse(model.state.value.restoring)
        assertNull(model.state.value.accountName)

        model.login(" ಮಾಯಾ ", "test-password")
        advanceUntilIdle()
        assertEquals(" ಮಾಯಾ ", model.state.value.accountName)
        assertEquals("test-token", vault.session?.token)
        assertNotNull(model.state.value.data)
        assertEquals(1, repository.loads)

        model.signOut()
        advanceUntilIdle()
        assertNull(model.state.value.accountName)
        assertNull(model.state.value.data)
        assertNull(vault.session)
    }

    @Test fun restoresSessionAndReturnsToLoginOnProtected401() = runTest {
        vault.session = Session("maya", "old")
        createModel()
        advanceUntilIdle()
        assertEquals("maya", model.state.value.accountName)
        assertNotNull(model.state.value.dashboard)

        repository.loadError = ApiFailure.Expired()
        model.refresh()
        advanceUntilIdle()
        assertNull(model.state.value.accountName)
        assertNull(model.state.value.dashboard)
        assertNull(vault.session)
        assertTrue(model.state.value.authNotice.orEmpty().contains("expired"))
    }

    @Test fun rejectsBadCredentialsWithoutCreatingSession() = runTest {
        repository.loginError = ApiFailure.Rejected(401, "Invalid credentials")
        createModel()
        advanceUntilIdle()
        model.login("maya", "invalid-password")
        advanceUntilIdle()
        assertEquals("Invalid credentials", model.state.value.authError)
        assertNull(vault.session)
        assertNull(model.state.value.accountName)
        assertEquals(0, repository.loads)
    }

    @Test fun preventsDuplicateWritesAndRefreshesAfterAcknowledgment() = runTest {
        vault.session = Session("maya", "test-token")
        repository.saveGate = CompletableDeferred()
        createModel()
        advanceUntilIdle()
        model.openEntry(Kind.Expense)
        completeDraft()
        model.saveEntry()
        model.saveEntry()
        advanceUntilIdle()

        assertTrue(model.state.value.writing)
        assertNull(model.state.value.message)
        assertNull(preferences.settings.value.lastPayment)
        assertEquals(1, repository.saves)
        assertEquals(1, repository.loads)

        repository.saveGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(model.state.value.writing)
        assertEquals("Transaction added", model.state.value.message)
        assertNull(model.state.value.overlay)
        assertEquals(2, repository.loads)
        assertEquals("upi", preferences.settings.value.lastPayment)
    }

    @Test fun preservesDraftOnFailureAndKeepsSnapshotDuringRefreshError() = runTest {
        vault.session = Session("maya", "test-token")
        repository.saveError = ApiFailure.Network("Couldn’t reach MoneyFlow.")
        createModel()
        advanceUntilIdle()
        model.requestExpenseShortcut()
        completeDraft()
        val initial = model.state.value.overlay as Overlay.Editor
        assertEquals(LocalDate.of(2026, 11, 26), initial.draft.date)
        model.saveEntry()
        advanceUntilIdle()

        assertEquals(initial, model.state.value.overlay)
        assertTrue(model.state.value.actionError.orEmpty().contains("couldn’t be confirmed"))
        assertNull(model.state.value.message)
        assertEquals(1, repository.loads)

        model.dismissOverlay()
        repository.loadError = ApiFailure.Network("Offline")
        model.refresh()
        advanceUntilIdle()
        assertEquals("Offline", model.state.value.refreshError)
        assertNotNull(model.state.value.data)
    }

    @Test fun filtersLedgerAndValidatesBeforeMutation() = runTest {
        vault.session = Session("maya", "test-token")
        createModel()
        advanceUntilIdle()
        model.openEntry(Kind.Expense)
        model.saveEntry()
        advanceUntilIdle()
        assertEquals(4, (model.state.value.overlay as Overlay.Editor).errors.size)
        assertEquals(0, repository.saves)
        model.search(Kind.Expense, "unmatched")
        assertTrue(model.state.value.expenseLedger.entries.isEmpty())
    }

    private fun completeDraft() {
        val editor = model.state.value.overlay as Overlay.Editor
        model.updateDraft(editor.draft.copy(amount = "40.10", title = "Lunch", categoryId = 4, method = "upi"))
    }

    private fun createModel() {
        val clock = Clock.fixed(Instant.parse("2026-11-25T20:00:00Z"), ZoneId.of("Asia/Kolkata"))
        model = MoneyViewModel(repository, vault, preferences, clock)
    }

    private class FakeVault : SessionVault {
        var session: Session? = null
        override suspend fun read() = session
        override suspend fun save(session: Session) { this.session = session }
        override suspend fun clear() { session = null }
    }

    private class FakePreferences : Preferences {
        override val settings = MutableStateFlow(Settings())
        override suspend fun appearance(value: Appearance) { settings.value = settings.value.copy(appearance = value) }
        override suspend fun dynamicColor(enabled: Boolean) { settings.value = settings.value.copy(dynamicColor = enabled) }
        override suspend fun lastPayment(value: String?) { settings.value = settings.value.copy(lastPayment = value) }
    }

    private class FakeRepository : MoneyRepository {
        var loads = 0
        var saves = 0
        var loginError: ApiFailure? = null
        var loadError: ApiFailure? = null
        var saveError: ApiFailure? = null
        var saveGate: CompletableDeferred<Unit>? = null

        override suspend fun login(userName: String, password: String): String {
            loginError?.let { throw it }
            return "test-token"
        }

        override suspend fun register(fullName: String, userName: String, email: String, password: String) = Unit

        override suspend fun load(token: String): AccountData {
            loads++
            loadError?.let { throw it }
            return AccountData(emptyList(), emptyList(), listOf(Category(4, "Food")), emptyList())
        }

        override suspend fun saveEntry(token: String, entry: EntryWrite) {
            saves++
            saveGate?.await()
            saveError?.let { throw it }
        }

        override suspend fun deleteEntry(token: String, entry: Entry) = Unit
        override suspend fun addCategory(token: String, kind: Kind, name: String) = Unit
        override suspend fun deleteCategory(token: String, kind: Kind, id: Int) = Unit
    }
}
