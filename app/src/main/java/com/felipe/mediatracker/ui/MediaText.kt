package com.felipe.mediatracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipe.mediatracker.R
import com.felipe.mediatracker.domain.FormError
import com.felipe.mediatracker.domain.MediaFormValidator
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.domain.MediaStatus
import com.felipe.mediatracker.domain.MediaType

/** Converte tipos de domínio em textos localizados. Fica na camada de UI para o domínio não depender de Android. */

@Composable
fun MediaType.label(): String = stringResource(
    when (this) {
        MediaType.BOOK -> R.string.type_book
        MediaType.MOVIE -> R.string.type_movie
        MediaType.SERIES -> R.string.type_series
    }
)

@Composable
fun MediaStatus.label(): String = stringResource(
    when (this) {
        MediaStatus.PLANNED -> R.string.status_planned
        MediaStatus.IN_PROGRESS -> R.string.status_in_progress
        MediaStatus.COMPLETED -> R.string.status_completed
    }
)

/** Ex.: "120 de 300 páginas", ou aviso quando o total é desconhecido. */
@Composable
fun MediaItem.progressSummary(): String {
    if (!hasKnownTotal) return stringResource(R.string.progress_unknown_total)
    return stringResource(
        when (type) {
            MediaType.BOOK -> R.string.progress_pages
            MediaType.MOVIE -> R.string.progress_minutes
            MediaType.SERIES -> R.string.progress_episodes
        },
        currentUnit,
        totalUnits,
    )
}

/** Ex.: "300 páginas". Vazio quando o total é desconhecido. */
@Composable
fun MediaItem.totalSummary(): String? {
    if (!hasKnownTotal) return null
    return stringResource(
        when (type) {
            MediaType.BOOK -> R.string.units_pages
            MediaType.MOVIE -> R.string.units_minutes
            MediaType.SERIES -> R.string.units_episodes
        },
        totalUnits,
    )
}

/** Ex.: "Autor • 1954". */
fun MediaItem.creatorAndYear(): String? =
    listOfNotNull(creator?.takeIf { it.isNotBlank() }, year?.toString())
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" • ")

@Composable
fun FormError.message(): String = when (this) {
    FormError.REQUIRED -> stringResource(R.string.error_required)
    FormError.TOO_LONG -> stringResource(R.string.error_too_long)
    FormError.NOT_A_NUMBER -> stringResource(R.string.error_not_a_number)
    FormError.YEAR_OUT_OF_RANGE -> stringResource(R.string.error_year_range, MediaFormValidator.MIN_YEAR)
    FormError.TOTAL_OUT_OF_RANGE -> stringResource(R.string.error_total_range, MediaFormValidator.MAX_TOTAL_UNITS)
    FormError.RATING_OUT_OF_RANGE -> stringResource(R.string.error_rating_range)
}
