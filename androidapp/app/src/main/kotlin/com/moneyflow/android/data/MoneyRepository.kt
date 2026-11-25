package com.moneyflow.android.data

import com.moneyflow.android.domain.AccountData
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.EntryWrite
import com.moneyflow.android.domain.Kind
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal interface MoneyRepository {
    suspend fun login(userName: String, password: String): String
    suspend fun register(fullName: String, userName: String, email: String, password: String)
    suspend fun load(token: String): AccountData
    suspend fun saveEntry(token: String, entry: EntryWrite)
    suspend fun deleteEntry(token: String, entry: Entry)
    suspend fun addCategory(token: String, kind: Kind, name: String)
    suspend fun deleteCategory(token: String, kind: Kind, id: Int)
}

internal class NetworkRepository(private val api: MoneyFlowApi = createApi()) : MoneyRepository {
    override suspend fun login(userName: String, password: String): String = apiCall(protected = false) {
        api.login(LoginBody(userName, password)).token.takeIf { it.isNotBlank() }
            ?: throw ApiFailure.InvalidResponse()
    }

    override suspend fun register(fullName: String, userName: String, email: String, password: String) {
        apiCall(protected = false) { api.register(RegistrationBody(fullName, userName, email, password)) }
    }

    override suspend fun load(token: String): AccountData = apiCall {
        coroutineScope {
            val bearer = bearer(token)
            val expenses = async { api.expenses(bearer).map(ExpenseReply::entry) }
            val income = async { api.income(bearer).map(IncomeReply::entry) }
            val expenseCategories = async { api.categories(bearer, null).map(CategoryReply::category) }
            val incomeCategories = async { api.categories(bearer, Kind.Income.categoryType).map(CategoryReply::category) }
            AccountData(expenses.await(), income.await(), expenseCategories.await(), incomeCategories.await())
        }
    }

    override suspend fun saveEntry(token: String, entry: EntryWrite) {
        apiCall {
            val bearer = bearer(token)
            val id = entry.id
            when (entry.kind) {
                Kind.Expense -> {
                    val body = ExpenseBody(
                        entry.title, entry.amount, entry.date, entry.categoryId, entry.note,
                        entry.method.takeIf { id == null },
                    )
                    if (id == null) api.addExpense(bearer, body) else api.updateExpense(bearer, id, body)
                }
                Kind.Income -> {
                    val body = IncomeBody(entry.amount, entry.date, entry.categoryId, checkNotNull(entry.method), entry.note)
                    if (id == null) api.addIncome(bearer, body) else api.updateIncome(bearer, id, body)
                }
            }
        }
    }

    override suspend fun deleteEntry(token: String, entry: Entry) {
        apiCall {
            when (entry.kind) {
                Kind.Expense -> api.deleteExpense(bearer(token), entry.id)
                Kind.Income -> api.deleteIncome(bearer(token), entry.id)
            }
        }
    }

    override suspend fun addCategory(token: String, kind: Kind, name: String) {
        apiCall { api.addCategory(bearer(token), kind.categoryType, CategoryBody(name)) }
    }

    override suspend fun deleteCategory(token: String, kind: Kind, id: Int) {
        apiCall { api.deleteCategory(bearer(token), id, kind.categoryType) }
    }

    private fun bearer(token: String) = "Bearer $token"
}
