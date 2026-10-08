package com.felipe.mediatracker.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipe.mediatracker.R
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.ui.components.EmptyView
import com.felipe.mediatracker.ui.components.ErrorView
import com.felipe.mediatracker.ui.components.LoadingView
import com.felipe.mediatracker.ui.components.MediaCard
import com.felipe.mediatracker.ui.theme.MediaTrackerTheme

/** Ponto de entrada da tela: liga o ViewModel à tela "burra" ([LibraryScreen]). */
@Composable
fun LibraryRoute(
    viewModel: LibraryViewModel,
    onOpenItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onAddManual: () -> Unit,
    onOpenViewsDemo: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onRetry = viewModel::onRetry,
        onOpenItem = onOpenItem,
        onOpenSearch = onOpenSearch,
        onAddManual = onAddManual,
        onOpenViewsDemo = onOpenViewsDemo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onRetry: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onAddManual: () -> Unit,
    onOpenViewsDemo: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search_books))
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_views_demo)) },
                            onClick = {
                                menuExpanded = false
                                onOpenViewsDemo()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddManual,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.library_add_manual)) },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                LibraryUiState.Loading -> LoadingView()
                LibraryUiState.Error -> ErrorView(
                    title = stringResource(R.string.library_error_title),
                    message = stringResource(R.string.library_error_message),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = onRetry,
                )
                is LibraryUiState.Content -> LibraryContent(
                    content = uiState,
                    onFilterSelected = onFilterSelected,
                    onOpenItem = onOpenItem,
                    onOpenSearch = onOpenSearch,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    content: LibraryUiState.Content,
    onFilterSelected: (LibraryFilter) -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
) {
    // Os filtros só fazem sentido se existir algo salvo.
    if (!content.libraryIsEmpty) {
        FilterRow(selected = content.filter, onSelected = onFilterSelected)
    }

    when {
        content.libraryIsEmpty -> EmptyView(
            title = stringResource(R.string.library_empty_title),
            message = stringResource(R.string.library_empty_message),
            actionLabel = stringResource(R.string.library_find_books),
            onAction = onOpenSearch,
        )
        content.items.isEmpty() -> EmptyView(
            title = stringResource(R.string.library_empty_filter_title),
            message = stringResource(R.string.library_empty_filter_message),
        )
        else -> LibraryGrid(items = content.items, onOpenItem = onOpenItem)
    }
}

@Composable
private fun FilterRow(selected: LibraryFilter, onSelected: (LibraryFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(LibraryFilter.entries) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

@Composable
private fun LibraryGrid(items: List<MediaItem>, onOpenItem: (String) -> Unit) {
    // Grade adaptativa: 1 coluna em celulares e mais colunas em telas largas, sem cortar conteúdo.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            MediaCard(item = item, onClick = { onOpenItem(item.id) })
        }
    }
}

@Composable
private fun LibraryFilter.label(): String = stringResource(
    when (this) {
        LibraryFilter.ALL -> R.string.filter_all
        LibraryFilter.IN_PROGRESS -> R.string.filter_in_progress
        LibraryFilter.PLANNED -> R.string.filter_planned
        LibraryFilter.COMPLETED -> R.string.filter_completed
    }
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LibraryScreenEmptyPreview() {
    MediaTrackerTheme {
        LibraryScreen(
            uiState = LibraryUiState.Content(emptyList(), LibraryFilter.ALL, libraryIsEmpty = true),
            onFilterSelected = {},
            onRetry = {},
            onOpenItem = {},
            onOpenSearch = {},
            onAddManual = {},
            onOpenViewsDemo = {},
        )
    }
}
