package com.felipe.mediatracker

import android.app.Application
import android.content.Context
import com.felipe.mediatracker.data.DefaultMediaRepository
import com.felipe.mediatracker.data.MediaRepository
import com.felipe.mediatracker.data.local.DataStoreLibraryDataSource
import com.felipe.mediatracker.data.local.libraryDataStore
import com.felipe.mediatracker.data.remote.OpenLibraryDataSource
import kotlinx.serialization.json.Json

/**
 * Injeção de dependências manual: monta o grafo de objetos uma única vez por processo.
 * Os ViewModels recebem [repository] pelas suas factories (ver `ui/ViewModelFactories.kt`).
 */
class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true // a API pode ganhar campos novos sem quebrar o app
        encodeDefaults = true
    }

    val repository: MediaRepository = DefaultMediaRepository(
        local = DataStoreLibraryDataSource(context.applicationContext.libraryDataStore, json),
        remote = OpenLibraryDataSource(json),
    )
}

class MediaTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
