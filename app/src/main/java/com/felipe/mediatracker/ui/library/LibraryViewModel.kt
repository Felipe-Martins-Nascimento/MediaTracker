package com.felipe.mediatracker.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipe.mediatracker.data.MediaRepository
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.domain.MediaStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class LibraryFilter {
    ALL, IN_PROGRESS, PLANNED, COMPLETED;

    fun matches(item: MediaItem): Boolean = when (this) {
        ALL -> true
        IN_PROGRESS -> item.status == MediaStatus.IN_PROGRESS
        PLANNED -> item.status == MediaStatus.PLANNED
        COMPLETED -> item.status == MediaStatus.COMPLETED
    }
}

/** Estados da tela inicial. "Vazio" é o [Content] com `items` vazio, tratado com visual próprio na UI. */
sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Error : LibraryUiState

    /**
     * @param items itens já filtrados.
     * @param libraryIsEmpty `true` quando não há nenhum item salvo (diferente de "filtro sem resultados").
     */
    data class Content(
        val items: List<MediaItem>,
        val filter: LibraryFilter,
        val libraryIsEmpty: Boolean,
    ) : LibraryUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(repository: MediaRepository) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val attempt = MutableStateFlow(0)

    val uiState: StateFlow<LibraryUiState> = attempt
        .flatMapLatest {
            combine(repository.library, filter) { items, selected -> content(items, selected) }
                .catch { emit(LibraryUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState.Loading)

    fun onFilterSelected(selected: LibraryFilter) {
        filter.value = selected
    }

    fun onRetry() {
        attempt.update { it + 1 }
    }

    private fun content(items: List<MediaItem>, selected: LibraryFilter): LibraryUiState =
        LibraryUiState.Content(
            items = items.filter { selected.matches(it) },
            filter = selected,
            libraryIsEmpty = items.isEmpty(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
