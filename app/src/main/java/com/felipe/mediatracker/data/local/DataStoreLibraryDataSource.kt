package com.felipe.mediatracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.felipe.mediatracker.domain.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

/** Instância única do DataStore, criada por processo. */
val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(name = "media_library")

/**
 * Persiste a biblioteca como uma lista JSON dentro de um DataStore de preferências.
 * Todas as escritas passam por [DataStore.edit], que é transacional e serializada.
 */
class DataStoreLibraryDataSource(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : LibraryDataSource {

    private val listSerializer = ListSerializer(MediaItem.serializer())
    private val libraryKey = stringPreferencesKey("library_json")

    override val items: Flow<List<MediaItem>> = dataStore.data
        .catch { error ->
            // Falha de leitura de disco não deve derrubar o app: começa vazio.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> decode(preferences[libraryKey]) }

    override suspend fun upsert(item: MediaItem) {
        mutate { current ->
            if (current.any { it.id == item.id }) current.map { if (it.id == item.id) item else it }
            else current + item
        }
    }

    override suspend fun insertIfAbsent(item: MediaItem) {
        mutate { current -> if (current.any { it.id == item.id }) current else current + item }
    }

    override suspend fun update(id: String, transform: (MediaItem) -> MediaItem) {
        mutate { current -> current.map { if (it.id == id) transform(it) else it } }
    }

    override suspend fun delete(id: String) {
        mutate { current -> current.filterNot { it.id == id } }
    }

    private suspend fun mutate(change: (List<MediaItem>) -> List<MediaItem>) {
        dataStore.edit { preferences ->
            val next = change(decode(preferences[libraryKey]))
            preferences[libraryKey] = json.encodeToString(listSerializer, next)
        }
    }

    private fun decode(raw: String?): List<MediaItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(listSerializer, raw)
        } catch (e: IllegalArgumentException) {
            // SerializationException herda de IllegalArgumentException.
            // Dado corrompido não deve quebrar o app: começa com a lista vazia.
            emptyList()
        }
    }
}
