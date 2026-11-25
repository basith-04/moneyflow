package com.moneyflow.android.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal const val API_URL = "https://moneyflow-czo7.onrender.com/"
internal const val WEBSITE_URL = "https://moneyflow-two-khaki.vercel.app/"
private const val AUTH = "Authorization"

internal val wireJson = Json { ignoreUnknownKeys = true }

internal interface MoneyFlowApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginBody): LoginReply

    @POST("auth/register")
    suspend fun register(@Body body: RegistrationBody): Acknowledgment

    @GET("expenses")
    suspend fun expenses(@Header(AUTH) bearer: String): List<ExpenseReply>

    @POST("expenses")
    suspend fun addExpense(@Header(AUTH) bearer: String, @Body body: ExpenseBody): Acknowledgment

    @PATCH("expenses/{id}")
    suspend fun updateExpense(@Header(AUTH) bearer: String, @Path("id") id: Int, @Body body: ExpenseBody): Acknowledgment

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Header(AUTH) bearer: String, @Path("id") id: Int): Acknowledgment

    @GET("income")
    suspend fun income(@Header(AUTH) bearer: String): List<IncomeReply>

    @POST("income")
    suspend fun addIncome(@Header(AUTH) bearer: String, @Body body: IncomeBody): Acknowledgment

    @PATCH("income/{id}")
    suspend fun updateIncome(@Header(AUTH) bearer: String, @Path("id") id: Int, @Body body: IncomeBody): Acknowledgment

    @DELETE("income/{id}")
    suspend fun deleteIncome(@Header(AUTH) bearer: String, @Path("id") id: Int): Acknowledgment

    @GET("category")
    suspend fun categories(@Header(AUTH) bearer: String, @Query("type") type: String?): List<CategoryReply>

    @POST("category")
    suspend fun addCategory(@Header(AUTH) bearer: String, @Query("type") type: String?, @Body body: CategoryBody): Acknowledgment

    @DELETE("category/{id}")
    suspend fun deleteCategory(@Header(AUTH) bearer: String, @Path("id") id: Int, @Query("type") type: String?): Acknowledgment
}

private class SingleSendBody(private val body: RequestBody) : RequestBody() {
    override fun contentType() = body.contentType()
    override fun contentLength() = body.contentLength()
    override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
    override fun isOneShot() = true
}

internal fun createApi(baseUrl: String = API_URL): MoneyFlowApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(65, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder().header("Accept", "application/json")
            original.body?.let { request.method(original.method, SingleSendBody(it)) }
            chain.proceed(request.build())
        }
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            // OkHttp can follow a 503 with Retry-After: 0 even when connection retries are disabled.
            if (chain.request().method != "GET" && response.code == 503) {
                response.newBuilder().removeHeader("Retry-After").build()
            } else {
                response
            }
        }
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(wireJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MoneyFlowApi::class.java)
}

internal sealed class ApiFailure(message: String) : Exception(message) {
    class Expired : ApiFailure("Your session expired. Sign in again to continue.")
    class Rejected(val status: Int, message: String) : ApiFailure(message)
    class Network(message: String) : ApiFailure(message)
    class InvalidResponse : ApiFailure("MoneyFlow returned an unexpected response.")
}

internal suspend fun <T> apiCall(protected: Boolean = true, call: suspend () -> T): T = try {
    call()
} catch (error: HttpException) {
    if (protected && error.code() == 401) throw ApiFailure.Expired()
    val decoded = runCatching {
        error.response()?.errorBody()?.use { wireJson.decodeFromString<ErrorReply>(it.string()) }
    }.getOrNull()
    val description = (decoded?.error ?: decoded?.message)?.trim()?.takeIf { it.isNotEmpty() }
    val message = when {
        error.code() >= 500 -> "The MoneyFlow server couldn’t complete the request."
        error.code() == 429 -> "Too many requests. Try again in a moment."
        else -> description ?: "MoneyFlow couldn’t complete the request (${error.code()})."
    }
    throw ApiFailure.Rejected(error.code(), message)
} catch (error: NullPointerException) {
    // Retrofit rejects successful responses with an empty body for non-null return types.
    if (error.message?.startsWith("Response from ") != true) throw error
    throw ApiFailure.InvalidResponse()
} catch (_: SerializationException) {
    throw ApiFailure.InvalidResponse()
} catch (_: SocketTimeoutException) {
    throw ApiFailure.Network("MoneyFlow took too long to respond.")
} catch (_: IOException) {
    throw ApiFailure.Network("Couldn’t reach MoneyFlow. Check your connection.")
}
