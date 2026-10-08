package com.felipe.mediatracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.felipe.mediatracker.MediaTrackerApp
import com.felipe.mediatracker.data.MediaRepository
import com.felipe.mediatracker.ui.detail.DetailViewModel
import com.felipe.mediatracker.ui.form.MediaFormViewModel
import com.felipe.mediatracker.ui.library.LibraryViewModel
import com.felipe.mediatracker.ui.search.SearchViewModel

/** Obtém o repositório do contêiner de dependências manual (ver `AppContainer`). */
@Composable
fun rememberRepository(): MediaRepository {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as MediaTrackerApp).container.repository }
}

// Factories dos ViewModels: é aqui que os parâmetros (repositório, id) são injetados.

fun libraryViewModelFactory(repository: MediaRepository) = viewModelFactory {
    initializer { LibraryViewModel(repository) }
}

fun searchViewModelFactory(repository: MediaRepository) = viewModelFactory {
    initializer { SearchViewModel(repository) }
}

fun detailViewModelFactory(itemId: String, repository: MediaRepository) = viewModelFactory {
    initializer { DetailViewModel(itemId, repository) }
}

fun formViewModelFactory(itemId: String?, repository: MediaRepository) = viewModelFactory {
    initializer { MediaFormViewModel(itemId, repository) }
}
