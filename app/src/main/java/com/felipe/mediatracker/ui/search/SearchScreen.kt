package com.felipe.mediatracker.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipe.mediatracker.R
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.ui.components.EmptyView
import com.felipe.mediatracker.ui.components.ErrorView
import com.felipe.mediatracker.ui.components.LoadingView
import com.felipe.mediatracker.ui.components.MediaCard

@Composable
fun SearchRoute(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        uiState = uiState,
        onSearch = viewModel::search,
        onAdd = viewModel::onAdd,
        onOpenItem = onOpenItem,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onSearch: (String) -> Unit,
    onAdd: (MediaItem) -> Unit,
    onOpenItem: (String) -> Unit,
    onBack: () -> Unit,
) {
    // O texto digitado sobrevive à rotação e à recriação do processo (rememberSaveable).
    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    fun submit() {
        focusManager.clearFocus()
        onSearch(query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                trailingIcon = {
                    IconButton(onClick = { submit() }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                SearchResults(
                    uiState = uiState,
                    onRetry = onSearch,
                    onAdd = onAdd,
                    onOpenItem = onOpenItem,
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onRetry: (String) -> Unit,
    onAdd: (MediaItem) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    when (val state = uiState.state) {
        SearchState.Idle -> EmptyView(
            icon = Icons.Default.Search,
            title = stringResource(R.string.search_idle_title),
            message = stringResource(R.string.search_idle_message),
        )
        SearchState.QueryTooShort -> EmptyView(
            icon = Icons.Default.Search,
            title = stringResource(R.string.search_short_title),
            message = stringResource(R.string.search_short_message, SearchViewModel.MIN_QUERY_LENGTH),
        )
        SearchState.Loading -> LoadingView()
        is SearchState.Empty -> EmptyView(
            icon = Icons.Default.Search,
            title = stringResource(R.string.search_empty_title),
            message = stringResource(R.string.search_empty_message, state.query),
        )
        is SearchState.Error -> ErrorView(
            title = stringResource(
                when (state.reason) {
                    SearchError.NETWORK -> R.string.search_error_network_title
                    SearchError.UNKNOWN -> R.string.search_error_unknown_title
                }
            ),
            message = stringResource(
                when (state.reason) {
                    SearchError.NETWORK -> R.string.search_error_network_message
                    SearchError.UNKNOWN -> R.string.search_error_unknown_message
                }
            ),
            retryLabel = stringResource(R.string.action_retry),
            onRetry = { onRetry(state.query) },
        )
        is SearchState.Results -> ResultsList(
            items = state.items,
            savedIds = uiState.savedIds,
            onAdd = onAdd,
            onOpenItem = onOpenItem,
        )
    }
}

@Composable
private fun ResultsList(
    items: List<MediaItem>,
    savedIds: Set<String>,
    onAdd: (MediaItem) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp) // em tablets, evita cards esticados por toda a largura
                .fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                val isSaved = item.id in savedIds
                MediaCard(
                    item = item,
                    showProgress = false,
                    onClick = if (isSaved) ({ onOpenItem(item.id) }) else null,
                    trailing = { AddToLibraryAction(item = item, isSaved = isSaved, onAdd = onAdd) },
                )
            }
        }
    }
}

@Composable
private fun AddToLibraryAction(item: MediaItem, isSaved: Boolean, onAdd: (MediaItem) -> Unit) {
    if (isSaved) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.search_in_library),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    } else {
        val description = stringResource(R.string.search_add_description, item.title)
        FilledTonalButton(
            onClick = { onAdd(item) },
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.search_add),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
