package com.felipe.mediatracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipe.mediatracker.data.MediaRepository
import com.felipe.mediatracker.domain.MediaItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Content(val item: MediaItem) : DetailUiState
}

/** Eventos de uso único (navegação). Não ficam no estado para não disparar de novo após rotação. */
sealed interface DetailEvent {
    data object Deleted : DetailEvent
}

class DetailViewModel(
    private val itemId: String,
    private val repository: MediaRepository,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = repository.observeItem(itemId)
        .map<MediaItem?, DetailUiState> { item ->
            if (item == null) DetailUiState.NotFound else DetailUiState.Content(item)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DetailUiState.Loading)

    private val eventChannel = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = eventChannel.receiveAsFlow()

    fun onProgressChange(value: Int) {
        viewModelScope.launch { repository.updateProgress(itemId, value) }
    }

    fun onRatingChange(rating: Int?) {
        viewModelScope.launch { repository.updateRating(itemId, rating) }
    }

    fun onDelete() {
        viewModelScope.launch {
            repository.delete(itemId)
            eventChannel.send(DetailEvent.Deleted)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
