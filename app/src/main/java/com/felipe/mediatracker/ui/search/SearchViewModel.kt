package com.felipe.mediatracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipe.mediatracker.data.MediaRepository
import com.felipe.mediatracker.domain.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

enum class SearchError { NETWORK, UNKNOWN }

/** Estados mutuamente exclusivos da área de resultados. */
sealed interface SearchState {
    data object Idle : SearchState
    data object QueryTooShort : SearchState
    data object Loading : SearchState
    data class Empty(val query: String) : SearchState
    data class Error(val reason: SearchError, val query: String) : SearchState
    data class Results(val query: String, val items: List<MediaItem>) : SearchState
}

/** @param savedIds ids que já estão na biblioteca, para o botão "Adicionar" refletir o estado real. */
data class SearchUiState(
    val state: SearchState = SearchState.Idle,
    val savedIds: Set<String> = emptySet(),
)

class SearchViewModel(private val repository: MediaRepository) : ViewModel() {

    private val searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    private var searchJob: Job? = null

    val uiState: StateFlow<SearchUiState> = combine(
        searchState,
        repository.library.map { items -> items.mapTo(HashSet()) { it.id } },
    ) { state, savedIds -> SearchUiState(state, savedIds) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState())

    fun search(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            searchJob?.cancel()
            searchState.value = SearchState.QueryTooShort
            return
        }
        // Uma nova busca cancela a anterior (a requisição em andamento é interrompida).
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searchState.value = SearchState.Loading
            searchState.value = try {
                val items = repository.searchBooks(query)
                if (items.isEmpty()) SearchState.Empty(query) else SearchState.Results(query, items)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (io: IOException) {
                SearchState.Error(SearchError.NETWORK, query)
            } catch (other: Exception) {
                SearchState.Error(SearchError.UNKNOWN, query)
            }
        }
    }

    fun onAdd(item: MediaItem) {
        viewModelScope.launch { repository.addToLibrary(item) }
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
