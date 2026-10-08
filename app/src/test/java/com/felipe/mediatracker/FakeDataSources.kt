package com.felipe.mediatracker

import com.felipe.mediatracker.data.local.LibraryDataSource
import com.felipe.mediatracker.data.remote.BookDto
import com.felipe.mediatracker.data.remote.BookRemoteDataSource
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.domain.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

/** Fake em memória da fonte local: permite testar Repository e ViewModels sem DataStore. */
class FakeLibraryDataSource(initial: List<MediaItem> = emptyList()) : LibraryDataSource {
    private val state = MutableStateFlow(initial)
    override val items: Flow<List<MediaItem>> = state

    override suspend fun upsert(item: MediaItem) = state.update { current ->
        if (current.any { it.id == item.id }) current.map { if (it.id == item.id) item else it } else current + item
    }

    override suspend fun insertIfAbsent(item: MediaItem) = state.update { current ->
        if (current.any { it.id == item.id }) current else current + item
    }

    override suspend fun update(id: String, transform: (MediaItem) -> MediaItem) = state.update { current ->
        current.map { if (it.id == id) transform(it) else it }
    }

    override suspend fun delete(id: String) = state.update { current -> current.filterNot { it.id == id } }
}

/** Fake da fonte remota: conta chamadas (para testar cache) e pode simular falha de rede. */
class FakeBookRemoteDataSource(var result: List<BookDto> = emptyList()) : BookRemoteDataSource {
    var calls = 0
        private set
    var failWith: Throwable? = null

    override suspend fun searchBooks(query: String): List<BookDto> {
        calls++
        failWith?.let { throw it }
        return result
    }
}

fun networkError() = IOException("sem rede")

fun sampleBookDto(key: String = "/works/OL1W", title: String? = "Dom Casmurro") = BookDto(
    key = key,
    title = title,
    authorNames = listOf("Machado de Assis"),
    firstPublishYear = 1899,
    pagesMedian = 256,
    coverId = 123L,
)

fun sampleItem(
    id: String = "id-1",
    title: String = "Item",
    type: MediaType = MediaType.BOOK,
    total: Int = 100,
    current: Int = 0,
    addedAt: Long = 0L,
) = MediaItem(id = id, title = title, type = type, totalUnits = total, currentUnit = current, addedAt = addedAt)
