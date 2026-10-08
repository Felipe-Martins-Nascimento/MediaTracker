package com.felipe.mediatracker.data.remote

import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.domain.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Resposta de `GET https://openlibrary.org/search.json`. Só os campos que usamos. */
@Serializable
data class BookSearchResponseDto(
    val docs: List<BookDto> = emptyList(),
)

@Serializable
data class BookDto(
    val key: String,
    val title: String? = null,
    @SerialName("author_name") val authorNames: List<String>? = null,
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("number_of_pages_median") val pagesMedian: Int? = null,
    @SerialName("cover_i") val coverId: Long? = null,
)

/** Mapeia o DTO remoto para o modelo de domínio. Retorna null se o item não tiver título utilizável. */
fun BookDto.toMediaItem(): MediaItem? {
    val cleanTitle = title?.trim().orEmpty()
    if (cleanTitle.isEmpty()) return null
    return MediaItem(
        id = "ol:$key",
        title = cleanTitle,
        type = MediaType.BOOK,
        creator = authorNames?.firstOrNull { it.isNotBlank() }?.trim(),
        year = firstPublishYear,
        coverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
        totalUnits = (pagesMedian ?: 0).coerceAtLeast(0),
    )
}
