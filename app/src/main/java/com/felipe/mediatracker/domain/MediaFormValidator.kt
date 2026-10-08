package com.felipe.mediatracker.domain

/** Texto bruto digitado no formulário. Tudo é String porque vem direto dos campos de texto. */
data class MediaFormInput(
    val title: String = "",
    val creator: String = "",
    val year: String = "",
    val totalUnits: String = "",
    val rating: String = "",
    val notes: String = "",
)

/** Tipos de erro de validação. A interface converte cada um em uma mensagem localizada. */
enum class FormError {
    REQUIRED,
    TOO_LONG,
    NOT_A_NUMBER,
    YEAR_OUT_OF_RANGE,
    TOTAL_OUT_OF_RANGE,
    RATING_OUT_OF_RANGE,
}

data class FormErrors(
    val title: FormError? = null,
    val creator: FormError? = null,
    val year: FormError? = null,
    val totalUnits: FormError? = null,
    val rating: FormError? = null,
    val notes: FormError? = null,
) {
    val hasErrors: Boolean
        get() = listOf(title, creator, year, totalUnits, rating, notes).any { it != null }
}

/** Regras de validação do formulário, sem dependência de Android (testável na JVM). */
object MediaFormValidator {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_CREATOR_LENGTH = 80
    const val MAX_NOTES_LENGTH = 500
    const val MIN_YEAR = 1800
    const val MAX_YEARS_IN_FUTURE = 5
    const val MAX_TOTAL_UNITS = 100_000

    fun validate(input: MediaFormInput, currentYear: Int): FormErrors {
        val title = input.title.trim()
        val year = input.year.trim()
        val total = input.totalUnits.trim()
        val rating = input.rating.trim()

        return FormErrors(
            title = when {
                title.isEmpty() -> FormError.REQUIRED
                title.length > MAX_TITLE_LENGTH -> FormError.TOO_LONG
                else -> null
            },
            creator = if (input.creator.trim().length > MAX_CREATOR_LENGTH) FormError.TOO_LONG else null,
            year = when {
                year.isEmpty() -> null // opcional
                year.toIntOrNull() == null -> FormError.NOT_A_NUMBER
                year.toInt() !in MIN_YEAR..(currentYear + MAX_YEARS_IN_FUTURE) -> FormError.YEAR_OUT_OF_RANGE
                else -> null
            },
            totalUnits = when {
                total.isEmpty() -> FormError.REQUIRED
                total.toIntOrNull() == null -> FormError.NOT_A_NUMBER
                total.toInt() !in 1..MAX_TOTAL_UNITS -> FormError.TOTAL_OUT_OF_RANGE
                else -> null
            },
            rating = when {
                rating.isEmpty() -> null // opcional
                rating.toIntOrNull() == null -> FormError.NOT_A_NUMBER
                rating.toInt() !in 0..10 -> FormError.RATING_OUT_OF_RANGE
                else -> null
            },
            notes = if (input.notes.length > MAX_NOTES_LENGTH) FormError.TOO_LONG else null,
        )
    }
}
