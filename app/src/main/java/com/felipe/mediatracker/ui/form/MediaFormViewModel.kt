package com.felipe.mediatracker.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipe.mediatracker.data.MediaRepository
import com.felipe.mediatracker.domain.FormErrors
import com.felipe.mediatracker.domain.MediaFormInput
import com.felipe.mediatracker.domain.MediaFormValidator
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.domain.MediaType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Year
import java.util.UUID

data class MediaFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isSaving: Boolean = false,
    val type: MediaType = MediaType.MOVIE,
    val input: MediaFormInput = MediaFormInput(),
    val errors: FormErrors = FormErrors(),
)

sealed interface MediaFormEvent {
    data object Saved : MediaFormEvent
}

/**
 * Formulário de criação (itemId == null) e edição (itemId != null) de um item.
 * Fluxo unidirecional: a UI só chama `onXxxChange`/`onSave`, e observa [uiState].
 */
class MediaFormViewModel(
    private val itemId: String?,
    private val repository: MediaRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val currentYear: Int = Year.now().value,
) : ViewModel() {

    private var original: MediaItem? = null

    private val _uiState = MutableStateFlow(MediaFormUiState(isEditing = itemId != null, isLoading = itemId != null))
    val uiState: StateFlow<MediaFormUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<MediaFormEvent>(Channel.BUFFERED)
    val events: Flow<MediaFormEvent> = eventChannel.receiveAsFlow()

    init {
        if (itemId != null) loadForEdit(itemId)
    }

    fun onTypeChange(type: MediaType) = _uiState.update { it.copy(type = type) }

    fun onTitleChange(value: String) = updateInput(
        change = { it.copy(title = value) },
        clearError = { it.copy(title = null) },
    )

    fun onCreatorChange(value: String) = updateInput(
        change = { it.copy(creator = value) },
        clearError = { it.copy(creator = null) },
    )

    fun onYearChange(value: String) = updateInput(
        change = { it.copy(year = value.filter(Char::isDigit).take(4)) },
        clearError = { it.copy(year = null) },
    )

    fun onTotalChange(value: String) = updateInput(
        change = { it.copy(totalUnits = value.filter(Char::isDigit).take(6)) },
        clearError = { it.copy(totalUnits = null) },
    )

    fun onRatingChange(value: String) = updateInput(
        change = { it.copy(rating = value.filter(Char::isDigit).take(2)) },
        clearError = { it.copy(rating = null) },
    )

    fun onNotesChange(value: String) = updateInput(
        change = { it.copy(notes = value) },
        clearError = { it.copy(notes = null) },
    )

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading || state.notFound) return

        val errors = MediaFormValidator.validate(state.input, currentYear)
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errors = FormErrors()) }
        viewModelScope.launch {
            repository.save(buildItem(state))
            _uiState.update { it.copy(isSaving = false) }
            eventChannel.send(MediaFormEvent.Saved)
        }
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            val item = repository.observeItem(id).first()
            if (item == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
            } else {
                original = item
                _uiState.value = MediaFormUiState(
                    isEditing = true,
                    type = item.type,
                    input = item.toFormInput(),
                )
            }
        }
    }

    private fun buildItem(state: MediaFormUiState): MediaItem {
        val input = state.input
        val base = original ?: MediaItem(
            id = "manual:${UUID.randomUUID()}",
            title = "",
            type = state.type,
            addedAt = clock(),
        )
        return base.copy(
            title = input.title.trim(),
            creator = input.creator.trim().ifEmpty { null },
            year = input.year.trim().toIntOrNull(),
            totalUnits = input.totalUnits.trim().toInt(),
            notes = input.notes.trim(),
        )
            .withRating(input.rating.trim().toIntOrNull())
            .withProgress(base.currentUnit) // se o total diminuiu, o progresso é ajustado
    }

    private fun updateInput(change: (MediaFormInput) -> MediaFormInput, clearError: (FormErrors) -> FormErrors) {
        _uiState.update { it.copy(input = change(it.input), errors = clearError(it.errors)) }
    }

    private fun MediaItem.toFormInput() = MediaFormInput(
        title = title,
        creator = creator.orEmpty(),
        year = year?.toString().orEmpty(),
        totalUnits = totalUnits.takeIf { it > 0 }?.toString().orEmpty(),
        rating = rating?.toString().orEmpty(),
        notes = notes,
    )
}
