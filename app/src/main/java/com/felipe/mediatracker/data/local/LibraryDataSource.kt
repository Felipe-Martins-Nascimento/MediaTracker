package com.felipe.mediatracker.data.local

import com.felipe.mediatracker.domain.MediaItem
import kotlinx.coroutines.flow.Flow

/** Fonte de dados local da biblioteca. Interface para permitir fakes nos testes. */
interface LibraryDataSource {
    /** Emite a lista atualizada a cada alteração (exposição reativa). */
    val items: Flow<List<MediaItem>>

    /** Insere ou substitui o item com o mesmo id. */
    suspend fun upsert(item: MediaItem)

    /** Insere apenas se o id ainda não existir (idempotente). */
    suspend fun insertIfAbsent(item: MediaItem)

    /** Altera um item de forma atômica. Não faz nada se o id não existir. */
    suspend fun update(id: String, transform: (MediaItem) -> MediaItem)

    suspend fun delete(id: String)
}
