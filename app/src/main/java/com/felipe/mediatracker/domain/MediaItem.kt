package com.felipe.mediatracker.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Tipo de mídia acompanhada pelo usuário. Define a "unidade" de progresso (páginas, minutos, episódios). */
@Serializable
enum class MediaType { BOOK, MOVIE, SERIES }

/** Situação derivada do progresso. Nunca é gravada, para não existir estado inconsistente. */
enum class MediaStatus { PLANNED, IN_PROGRESS, COMPLETED }

/**
 * Item da biblioteca do usuário (modelo de domínio imutável).
 *
 * - [totalUnits] igual a 0 significa "total desconhecido" (comum em livros vindos da API).
 * - [rating] é opcional (null = ainda sem nota), de 0 a 10.
 */
@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val type: MediaType,
    val creator: String? = null,
    val year: Int? = null,
    val coverUrl: String? = null,
    val totalUnits: Int = 0,
    val currentUnit: Int = 0,
    val rating: Int? = null,
    val notes: String = "",
    val addedAt: Long = 0L,
) {
    val hasKnownTotal: Boolean
        get() = totalUnits > 0

    val status: MediaStatus
        get() = when {
            hasKnownTotal && currentUnit >= totalUnits -> MediaStatus.COMPLETED
            currentUnit > 0 -> MediaStatus.IN_PROGRESS
            else -> MediaStatus.PLANNED
        }

    /** Progresso entre 0f e 1f. Fica em 0f quando o total é desconhecido. */
    val progressFraction: Float
        get() = if (hasKnownTotal) currentUnit.coerceIn(0, totalUnits).toFloat() / totalUnits else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).roundToInt()

    /** Retorna uma cópia com o progresso limitado ao intervalo válido [0, totalUnits]. */
    fun withProgress(value: Int): MediaItem =
        copy(currentUnit = value.coerceIn(0, totalUnits.coerceAtLeast(0)))

    /** Retorna uma cópia com a nota limitada a [0, 10]. `null` remove a nota. */
    fun withRating(value: Int?): MediaItem = copy(rating = value?.coerceIn(0, 10))
}
