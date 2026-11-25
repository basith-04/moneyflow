package com.moneyflow.android

import com.moneyflow.android.data.ApiFailure
import com.moneyflow.android.data.NetworkRepository
import com.moneyflow.android.data.createApi
import com.moneyflow.android.data.wireJson
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.EntryWrite
import com.moneyflow.android.domain.Kind
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiContractTest {
    private val server = MockWebServer()
    private lateinit var repo: NetworkRepository
    private val day = LocalDate.of(2024, 2, 29)
    private val expense = EntryWrite(
        Kind.Expense, null, BigDecimal("123.40"), "Market", day, 4, "upi", null,
    )

    @Before fun startServer() {
        server.start()
        repo = NetworkRepository(createApi(server.url("/").toString()))
    }

    @After fun stopServer() = server.shutdown()

    @Test fun loginAndRegistrationUseExistingCredentialsContract() = runBlocking {
        reply("""{"token":"test-token"}""")
        assertEquals("test-token", repo.login("sam", "sample-password"))
        val signIn = request()
        assertEquals("POST", signIn.method)
        assertEquals("/auth/login", signIn.path)
        assertEquals("application/json", signIn.getHeader("Accept"))
        assertNull(signIn.getHeader("Authorization"))
        assertEquals(setOf("userName", "password"), json(signIn).keys)

        reply("""{"message":"user added","user_id":9}""", 201)
        repo.register("Sam Example", "sam", "sam@example.test", "sample-password")
        val registration = request()
        assertEquals("POST", registration.method)
        assertEquals("/auth/register", registration.path)
        assertNull(registration.getHeader("Authorization"))
        assertEquals(setOf("fullName", "userName", "email", "password"), json(registration).keys)
    }

    @Test fun readsAllFourCollectionsUsingBearerAndExactDecimals() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val result = when (request.path) {
                    "/expenses" -> """[{"expense_id":2,"title":"Market","amount":9999999999.99,"date":"2024-02-29","name":"Food","category_id":4,"note":null,"payment_mode":null}]"""
                    "/income" -> """[{"income_id":1,"amount":0.30,"date":"2024-02-29","name":"Work","category_id":5,"credited_to":"Bank Account","note":"Invoice"}]"""
                    "/category" -> """[{"id":4,"name":"Food","user_id":3}]"""
                    "/category?type=income" -> """[{"id":5,"name":"Work"}]"""
                    else -> return MockResponse().setResponseCode(404)
                }
                return MockResponse().setBody(result)
            }
        }
        val data = repo.load("test-token")
        assertEquals(BigDecimal("9999999999.99"), data.expenses.single().amount)
        assertEquals(day, data.expenses.single().date)
        assertNull(data.expenses.single().method)
        assertEquals(BigDecimal("0.30"), data.incomes.single().amount)
        assertEquals("Bank Account", data.incomes.single().method)
        assertEquals("Food", data.expenseCategories.single().name)
        assertEquals("Work", data.incomeCategories.single().name)
        val requests = List(4) { request() }
        assertTrue(requests.all { it.method == "GET" && it.getHeader("Authorization") == "Bearer test-token" })
        assertEquals(setOf("/expenses", "/income", "/category", "/category?type=income"), requests.map { it.path }.toSet())
    }

    @Test fun createsAndEditsExpensesWithDifferentPaymentContracts() = runBlocking {
        reply("""{"message":"Expense added","expense_id":7}""", 201)
        repo.saveEntry("token", expense)
        val creation = request()
        assertEquals("POST", creation.method)
        assertEquals("/expenses", creation.path)
        assertEquals("Bearer token", creation.getHeader("Authorization"))
        val payload = json(creation)
        assertEquals("123.40", payload.getValue("amount").jsonPrimitive.content)
        assertFalse(payload.getValue("amount").jsonPrimitive.isString)
        assertEquals("2024-02-29", payload.getValue("date").jsonPrimitive.content)
        assertEquals("upi", payload.getValue("payment_mode").jsonPrimitive.content)
        assertEquals(JsonNull, payload["note"])

        reply("""{"message":"Expense updated"}""")
        repo.saveEntry("token", expense.copy(id = 7))
        val update = request()
        assertEquals("PATCH", update.method)
        assertEquals("/expenses/7", update.path)
        val patch = json(update)
        assertFalse(patch.containsKey("payment_mode"))
        assertEquals(JsonNull, patch["note"])
    }

    @Test fun incomeAndCategoryMutationsUseExistingPathsAndFields() = runBlocking {
        val income = expense.copy(kind = Kind.Income, method = "Cash")
        reply("""{"message":"Income added","income_id":6}""", 201)
        repo.saveEntry("token", income)
        val creation = request()
        assertEquals("POST", creation.method)
        assertEquals("/income", creation.path)
        assertEquals(setOf("amount", "date", "category_id", "credited_to", "note"), json(creation).keys)

        reply("""{"message":"Income updated"}""")
        repo.saveEntry("token", income.copy(id = 6, method = "Bank Account"))
        val update = request()
        assertEquals("PATCH", update.method)
        assertEquals("/income/6", update.path)
        assertEquals("Bank Account", json(update).getValue("credited_to").jsonPrimitive.content)

        reply("""{"message":"Income deleted"}""")
        repo.deleteEntry("token", Entry(Kind.Income, 6, BigDecimal.ONE, day, 4, "Work"))
        val deletion = request()
        assertEquals("DELETE", deletion.method)
        assertEquals("/income/6", deletion.path)

        reply("""{"message":"Expense deleted"}""")
        repo.deleteEntry("token", Entry(Kind.Expense, 7, BigDecimal.ONE, day, 4, "Food"))
        val expenseDeletion = request()
        assertEquals("DELETE", expenseDeletion.method)
        assertEquals("/expenses/7", expenseDeletion.path)

        reply("""{"message":"category added","category_id":4}""", 201)
        repo.addCategory("token", Kind.Income, "Work")
        val categoryCreation = request()
        assertEquals("POST", categoryCreation.method)
        assertEquals("/category?type=income", categoryCreation.path)
        assertEquals(setOf("name"), json(categoryCreation).keys)

        reply("""{"message":"Category deleted"}""")
        repo.deleteCategory("token", Kind.Income, 4)
        val categoryDeletion = request()
        assertEquals("DELETE", categoryDeletion.method)
        assertEquals("/category/4?type=income", categoryDeletion.path)
    }

    @Test fun distinguishesBadCredentialsExpiredAuthorizationValidationAndDecodeErrors() = runBlocking {
        reply("""{"error":"Invalid credentials"}""", 401)
        val login = runCatching { repo.login("sam", "wrong") }.exceptionOrNull() as ApiFailure.Rejected
        assertEquals(401, login.status)
        assertEquals("Invalid credentials", login.message)

        reply("""{"message":"Invalid token"}""", 401)
        assertTrue(runCatching { repo.deleteCategory("old", Kind.Expense, 4) }.exceptionOrNull() is ApiFailure.Expired)

        reply("""{"error":"invalid income category"}""", 400)
        val validation = runCatching {
            repo.saveEntry("token", expense.copy(kind = Kind.Income, method = "Cash"))
        }.exceptionOrNull() as ApiFailure.Rejected
        assertEquals("invalid income category", validation.message)

        reply("""{"unexpected":"object"}""")
        assertTrue(runCatching { repo.login("sam", "example") }.exceptionOrNull() is ApiFailure.InvalidResponse)

        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(runCatching { repo.login("sam", "example") }.exceptionOrNull() is ApiFailure.InvalidResponse)
    }

    @Test fun neverRepeatsCreatesOrDeletesAfter503() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "0"))
        reply("""{"message":"Expense added","expense_id":9}""", 201)
        assertTrue(runCatching { repo.saveEntry("token", expense) }.exceptionOrNull() is ApiFailure.Rejected)
        assertEquals(1, server.requestCount)

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                MockResponse().setResponseCode(503).setHeader("Retry-After", "0")
        }
        assertTrue(runCatching { repo.deleteCategory("token", Kind.Expense, 4) }.exceptionOrNull() is ApiFailure.Rejected)
        assertEquals(2, server.requestCount)
    }

    private fun reply(body: String, status: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body))
    }

    private fun request(): RecordedRequest = checkNotNull(server.takeRequest(2, TimeUnit.SECONDS))

    private fun json(request: RecordedRequest) = wireJson.parseToJsonElement(request.body.readUtf8()).jsonObject
}
