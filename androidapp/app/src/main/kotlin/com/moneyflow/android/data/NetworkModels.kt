package com.moneyflow.android.data

import com.moneyflow.android.domain.Category
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.Kind
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

internal object DecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("MoneyFlow.Decimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val primitive = (decoder as JsonDecoder).decodeJsonElement() as? JsonPrimitive
        return primitive?.content?.toBigDecimalOrNull()
            ?: throw SerializationException("Invalid decimal amount")
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        (encoder as JsonEncoder).encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }
}

internal object DaySerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("MoneyFlow.Day", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = try {
        LocalDate.parse(decoder.decodeString())
    } catch (_: DateTimeParseException) {
        throw SerializationException("Invalid ledger date")
    }
}

@Serializable
internal data class LoginBody(val userName: String, val password: String)

@Serializable
internal data class RegistrationBody(val fullName: String, val userName: String, val email: String, val password: String)

@Serializable
internal data class LoginReply(val token: String)

@Serializable
internal data class Acknowledgment(
    val message: String,
    @SerialName("expense_id") val expenseId: Int? = null,
    @SerialName("income_id") val incomeId: Int? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("user_id") val userId: Int? = null,
)

@Serializable
internal data class ErrorReply(val error: String? = null, val message: String? = null)

@Serializable
internal data class CategoryReply(val id: Int, val name: String) {
    fun category() = Category(id, name)
}

@Serializable
internal data class CategoryBody(val name: String)

@Serializable
internal data class ExpenseReply(
    @SerialName("expense_id") val id: Int,
    val title: String,
    @Serializable(with = DecimalSerializer::class) val amount: BigDecimal,
    @Serializable(with = DaySerializer::class) val date: LocalDate,
    val name: String,
    @SerialName("category_id") val categoryId: Int,
    val note: String? = null,
    @SerialName("payment_mode") val paymentMode: String? = null,
) {
    fun entry() = Entry(Kind.Expense, id, amount, date, categoryId, name, title, paymentMode, note)
}

@Serializable
internal data class IncomeReply(
    @SerialName("income_id") val id: Int,
    @Serializable(with = DecimalSerializer::class) val amount: BigDecimal,
    @Serializable(with = DaySerializer::class) val date: LocalDate,
    val name: String,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("credited_to") val creditedTo: String,
    val note: String? = null,
) {
    fun entry() = Entry(Kind.Income, id, amount, date, categoryId, name, method = creditedTo, note = note)
}

@Serializable
internal data class ExpenseBody(
    val title: String,
    @Serializable(with = DecimalSerializer::class) val amount: BigDecimal,
    @Serializable(with = DaySerializer::class) val date: LocalDate,
    @SerialName("category_id") val categoryId: Int,
    val note: String?,
    @SerialName("payment_mode") val paymentMode: String? = null,
)

@Serializable
internal data class IncomeBody(
    @Serializable(with = DecimalSerializer::class) val amount: BigDecimal,
    @Serializable(with = DaySerializer::class) val date: LocalDate,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("credited_to") val creditedTo: String,
    val note: String?,
)
