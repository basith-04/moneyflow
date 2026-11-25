package com.moneyflow.android.domain

import java.math.BigDecimal
import java.time.LocalDate

private val digits = Regex("[0-9]+(?:[.,][0-9]{0,2})?|[.,][0-9]{1,2}")
private val emailAddress = Regex("[^@\\s]+@[^@\\s]+")
private val largestAmount = BigDecimal("9999999999.99")

fun parseAmount(text: String): BigDecimal? {
    val value = buildString {
        text.trim().forEach { character ->
            append(character.digitToIntOrNull()?.digitToChar() ?: character)
        }
    }.replace('٫', '.')
    if (!digits.matches(value)) return null
    return value.replace(',', '.').toBigDecimalOrNull()?.takeIf { it <= largestAmount }
}

data class EntryDraft(
    val kind: Kind,
    val date: LocalDate,
    val id: Int? = null,
    val amount: String = "",
    val title: String = "",
    val categoryId: Int? = null,
    val method: String? = null,
    val note: String = "",
)

data class EntryWrite(
    val kind: Kind,
    val id: Int?,
    val amount: BigDecimal,
    val title: String,
    val date: LocalDate,
    val categoryId: Int,
    val method: String?,
    val note: String?,
)

enum class EntryField { Amount, Title, Category, Method }

fun validateEntry(draft: EntryDraft, categories: List<Category>): Map<EntryField, String> = buildMap {
    val parsed = parseAmount(draft.amount)
    if (parsed == null) {
        put(EntryField.Amount, "Enter an amount with up to 10 digits and 2 decimal places.")
    } else if (draft.id == null && parsed.signum() == 0) {
        put(EntryField.Amount, "Enter an amount greater than zero.")
    }
    if (draft.kind == Kind.Expense) {
        val title = draft.title.trim()
        if (title.isEmpty()) put(EntryField.Title, "Enter an expense title.")
        if (title.codePointCount(0, title.length) > 160) put(EntryField.Title, "Use at most 160 characters.")
    }
    if (categories.none { it.id == draft.categoryId }) put(EntryField.Category, "Choose a category.")
    if (draft.kind == Kind.Income && draft.method !in draft.kind.methods) {
        put(EntryField.Method, "Choose where the income was credited.")
    }
    if (draft.kind == Kind.Expense && draft.id == null &&
        draft.method !in draft.kind.methods
    ) {
        put(EntryField.Method, "Choose a payment method.")
    }
}

fun EntryDraft.validatedWrite() = EntryWrite(
    kind, id, checkNotNull(parseAmount(amount)), title.trim(), date, checkNotNull(categoryId),
    method, note.takeUnless(String::isBlank),
)

fun validateCategory(name: String, categories: List<Category>): String? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> "Enter a category name."
        trimmed.codePointCount(0, trimmed.length) > 120 -> "Use at most 120 characters."
        categories.any { it.name == trimmed } -> "That category already exists."
        else -> null
    }
}

fun validateRegistration(fullName: String, userName: String, email: String, password: String): String? = when {
    fullName.isBlank() || fullName.codePointCount(0, fullName.length) > 120 ->
        "Enter a full name with up to 120 characters."
    userName.isBlank() || userName.codePointCount(0, userName.length) > 80 ->
        "Enter a username with up to 80 characters."
    email.codePointCount(0, email.length) > 255 || !emailAddress.matches(email) -> "Enter a valid email address."
    password.isEmpty() -> "Enter a password."
    password.toByteArray(Charsets.UTF_8).size > 72 -> "Choose a shorter password."
    else -> null
}
