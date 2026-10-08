package com.felipe.mediatracker.data

import com.felipe.mediatracker.data.local.LibraryDataSource
import com.felipe.mediatracker.data.remote.BookRemoteDataSource
import com.felipe.mediatracker.data.remote.toMediaItem
import com.felipe.mediatracker.domain.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Única porta de entrada de dados para a interface (ViewModels). */
interface MediaRepository {
    /** Biblioteca do usuário, do item mais recente para o mais antigo. */
    val library: Flow<List<MediaItem>>

    /** Observa um item específico. Emite `null` se ele não existir (ou for removido). */
    fun observeItem(id: String): Flow<MediaItem?>

    /**
     * Busca livros na API remota. Resultados recentes ficam em cache em memória (TTL).
     * @throws java.io.IOException em falha de rede.
     */
    suspend fun searchBooks(query: String): List<MediaItem>

    /** Cria ou substitui um item (usado pelo formulário). */
    suspend fun save(item: MediaItem)

    /** Adiciona à biblioteca se ainda não existir. Idempotente. */
    suspend fun addToLibrary(item: MediaItem)

    suspend fun updateProgress(id: String, value: Int)

    suspend fun updateRating(id: String, rating: Int?)

    suspend fun delete(id: String)
}

class DefaultMediaRepository(
    private val local: LibraryDataSource,
    private val remote: BookRemoteDataSource,
    private val clock: () -> Long = System::currentTimeMillis,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
) : MediaRepository {

    private data class CachedSearch(val savedAt: Long, val items: List<MediaItem>)

    private val cacheMutex = Mutex()
    private val cache = HashMap<String, CachedSearch>()

    override val library: Flow<List<MediaItem>> =
        local.items.map { items -> items.sortedByDescending { it.addedAt } }

    override fun observeItem(id: String): Flow<MediaItem?> =
        local.items.map { items -> items.firstOrNull { it.id == id } }.distinctUntilChanged()

    override suspend fun searchBooks(query: String): List<MediaItem> {
        val key = query.trim().lowercase()
        require(key.isNotEmpty()) { "A busca não pode ser vazia" }

        cacheMutex.withLock {
            val cached = cache[key]
            if (cached != null && clock() - cached.savedAt < cacheTtlMs) return cached.items
        }

        val fresh = remote.searchBooks(key)
            .mapNotNull { it.toMediaItem() }
            .distinctBy { it.id }

        cacheMutex.withLock { cache[key] = CachedSearch(savedAt = clock(), items = fresh) }
        return fresh
    }

    override suspend fun save(item: MediaItem) = local.upsert(item)

    override suspend fun addToLibrary(item: MediaItem) =
        local.insertIfAbsent(item.copy(addedAt = clock()))

    override suspend fun updateProgress(id: String, value: Int) =
        local.update(id) { it.withProgress(value) }

    override suspend fun updateRating(id: String, rating: Int?) =
        local.update(id) { it.withRating(rating) }

    override suspend fun delete(id: String) = local.delete(id)

    companion object {
        const val DEFAULT_CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
