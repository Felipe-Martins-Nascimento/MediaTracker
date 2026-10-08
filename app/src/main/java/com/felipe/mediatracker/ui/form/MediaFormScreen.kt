package com.felipe.mediatracker.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipe.mediatracker.R
import com.felipe.mediatracker.domain.MediaFormValidator
import com.felipe.mediatracker.domain.MediaType
import com.felipe.mediatracker.ui.components.EmptyView
import com.felipe.mediatracker.ui.components.LoadingView
import com.felipe.mediatracker.ui.label
import com.felipe.mediatracker.ui.message

@Composable
fun MediaFormRoute(
    viewModel: MediaFormViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MediaFormEvent.Saved -> currentOnSaved()
            }
        }
    }

    MediaFormScreen(
        uiState = uiState,
        onBack = onBack,
        onTypeChange = viewModel::onTypeChange,
        onTitleChange = viewModel::onTitleChange,
        onCreatorChange = viewModel::onCreatorChange,
        onYearChange = viewModel::onYearChange,
        onTotalChange = viewModel::onTotalChange,
        onRatingChange = viewModel::onRatingChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaFormScreen(
    uiState: MediaFormUiState,
    onBack: () -> Unit,
    onTypeChange: (MediaType) -> Unit,
    onTitleChange: (String) -> Unit,
    onCreatorChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onTotalChange: (String) -> Unit,
    onRatingChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.form_title_edit else R.string.form_title_new
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> LoadingView()
                uiState.notFound -> EmptyView(
                    title = stringResource(R.string.detail_not_found_title),
                    message = stringResource(R.string.detail_not_found_message),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
                else -> FormContent(
                    uiState = uiState,
                    onTypeChange = onTypeChange,
                    onTitleChange = onTitleChange,
                    onCreatorChange = onCreatorChange,
                    onYearChange = onYearChange,
                    onTotalChange = onTotalChange,
                    onRatingChange = onRatingChange,
                    onNotesChange = onNotesChange,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun FormContent(
    uiState: MediaFormUiState,
    onTypeChange: (MediaType) -> Unit,
    onTitleChange: (String) -> Unit,
    onCreatorChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onTotalChange: (String) -> Unit,
    onRatingChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val input = uiState.input
    val errors = uiState.errors

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding() // o teclado não cobre o campo em edição
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // O tipo só pode ser escolhido na criação: trocar depois mudaria a unidade do progresso.
            if (!uiState.isEditing) {
                Text(stringResource(R.string.form_type_label), style = MaterialTheme.typography.labelLarge)
                TypeSelector(selected = uiState.type, onSelected = onTypeChange)
            }

            OutlinedTextField(
                value = input.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.form_field_title)) },
                isError = errors.title != null,
                supportingText = {
                    errors.title?.let { Text(it.message()) }
                        ?: Text("${input.title.length}/${MediaFormValidator.MAX_TITLE_LENGTH}")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = input.creator,
                onValueChange = onCreatorChange,
                label = { Text(stringResource(creatorLabel(uiState.type))) },
                isError = errors.creator != null,
                supportingText = errors.creator?.let { error -> { Text(error.message()) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = input.year,
                onValueChange = onYearChange,
                label = { Text(stringResource(R.string.form_field_year)) },
                isError = errors.year != null,
                supportingText = errors.year?.let { error -> { Text(error.message()) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = input.totalUnits,
                onValueChange = onTotalChange,
                label = { Text(stringResource(totalLabel(uiState.type))) },
                isError = errors.totalUnits != null,
                supportingText = errors.totalUnits?.let { error -> { Text(error.message()) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = input.rating,
                onValueChange = onRatingChange,
                label = { Text(stringResource(R.string.form_field_rating)) },
                isError = errors.rating != null,
                supportingText = errors.rating?.let { error -> { Text(error.message()) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = input.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.form_field_notes)) },
                isError = errors.notes != null,
                supportingText = {
                    errors.notes?.let { Text(it.message()) }
                        ?: Text("${input.notes.length}/${MediaFormValidator.MAX_NOTES_LENGTH}")
                },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text(stringResource(R.string.form_save))
            }
        }
    }
}

@Composable
private fun TypeSelector(selected: MediaType, onSelected: (MediaType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MediaType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelected(type) },
                label = { Text(type.label()) },
            )
        }
    }
}

private fun creatorLabel(type: MediaType): Int = when (type) {
    MediaType.BOOK -> R.string.form_field_author
    MediaType.MOVIE -> R.string.form_field_director
    MediaType.SERIES -> R.string.form_field_creator
}

private fun totalLabel(type: MediaType): Int = when (type) {
    MediaType.BOOK -> R.string.form_field_total_pages
    MediaType.MOVIE -> R.string.form_field_total_minutes
    MediaType.SERIES -> R.string.form_field_total_episodes
}
