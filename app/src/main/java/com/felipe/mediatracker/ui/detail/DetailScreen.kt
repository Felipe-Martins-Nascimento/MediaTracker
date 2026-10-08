package com.felipe.mediatracker.ui.detail

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipe.mediatracker.R
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.ui.components.CoverImage
import com.felipe.mediatracker.ui.components.EmptyView
import com.felipe.mediatracker.ui.components.LoadingView
import com.felipe.mediatracker.ui.components.TypeBadge
import com.felipe.mediatracker.ui.creatorAndYear
import com.felipe.mediatracker.ui.label
import com.felipe.mediatracker.ui.progressSummary
import kotlin.math.roundToInt

@Composable
fun DetailRoute(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    // Evento de uso único: após excluir, volta uma vez (não repete após rotação).
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DetailEvent.Deleted -> currentOnBack()
            }
        }
    }

    DetailScreen(
        uiState = uiState,
        onBack = onBack,
        onEdit = onEdit,
        onProgressChange = viewModel::onProgressChange,
        onRatingChange = viewModel::onRatingChange,
        onDelete = viewModel::onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onProgressChange: (Int) -> Unit,
    onRatingChange: (Int?) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val item = (uiState as? DetailUiState.Content)?.item

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.title.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { shareItem(context, item) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share))
                        }
                        IconButton(onClick = { onEdit(item.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                DetailUiState.Loading -> LoadingView()
                DetailUiState.NotFound -> EmptyView(
                    title = stringResource(R.string.detail_not_found_title),
                    message = stringResource(R.string.detail_not_found_message),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
                is DetailUiState.Content -> DetailContent(
                    item = uiState.item,
                    onEdit = { onEdit(uiState.item.id) },
                    onProgressChange = onProgressChange,
                    onRatingChange = onRatingChange,
                )
            }
        }
    }

    if (showDeleteDialog && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_message, item.title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailContent(
    item: MediaItem,
    onEdit: () -> Unit,
    onProgressChange: (Int) -> Unit,
    onRatingChange: (Int?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // rola com fontes grandes e em modo paisagem
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(item)
            ProgressCard(item = item, onEdit = onEdit, onProgressChange = onProgressChange)
            RatingCard(item = item, onRatingChange = onRatingChange)
            if (item.notes.isNotBlank()) {
                SectionCard(title = stringResource(R.string.detail_notes_title)) {
                    Text(item.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun Header(item: MediaItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CoverImage(
            title = item.title,
            coverUrl = item.coverUrl,
            contentDescription = stringResource(R.string.cover_description, item.title),
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2f / 3f),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.headlineSmall)
            item.creatorAndYear()?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TypeBadge(item.type)
            Text(
                item.status.label(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProgressCard(item: MediaItem, onEdit: () -> Unit, onProgressChange: (Int) -> Unit) {
    SectionCard(title = stringResource(R.string.detail_progress_title)) {
        if (!item.hasKnownTotal) {
            Text(stringResource(R.string.detail_unknown_total_message), style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.detail_set_total)) }
            return@SectionCard
        }

        val animatedProgress by animateFloatAsState(targetValue = item.progressFraction, label = "detailProgress")
        // Valor "em arraste" fica local; só grava no DataStore quando o usuário solta o controle.
        var draft by remember(item.currentUnit) { mutableFloatStateOf(item.currentUnit.toFloat()) }
        val draftValue = draft.roundToInt()
        val summary = item.copy(currentUnit = draftValue).progressSummary()

        Text(summary, style = MaterialTheme.typography.bodyLarge)
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())

        val sliderDescription = stringResource(R.string.detail_progress_slider_description)
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onProgressChange(draft.roundToInt()) },
            valueRange = 0f..item.totalUnits.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = sliderDescription
                    stateDescription = summary
                },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onProgressChange(item.currentUnit - 1) },
                enabled = item.currentUnit > 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.detail_decrease)) }
            OutlinedButton(
                onClick = { onProgressChange(item.currentUnit + 1) },
                enabled = item.currentUnit < item.totalUnits,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.detail_increase)) }
        }
        Button(
            onClick = { onProgressChange(item.totalUnits) },
            enabled = item.currentUnit < item.totalUnits,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.detail_complete)) }
    }
}

@Composable
private fun RatingCard(item: MediaItem, onRatingChange: (Int?) -> Unit) {
    SectionCard(title = stringResource(R.string.detail_rating_title)) {
        var draft by remember(item.rating) { mutableStateOf(item.rating) }

        Text(
            text = draft?.let { stringResource(R.string.detail_rating_value, it) }
                ?: stringResource(R.string.detail_rating_none),
            style = MaterialTheme.typography.bodyLarge,
        )
        val sliderDescription = stringResource(R.string.detail_rating_slider_description)
        Slider(
            value = (draft ?: 0).toFloat(),
            onValueChange = { draft = it.roundToInt() },
            onValueChangeFinished = { onRatingChange(draft) },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = sliderDescription },
        )
        if (draft != null) {
            TextButton(onClick = {
                draft = null
                onRatingChange(null)
            }) { Text(stringResource(R.string.detail_rating_clear)) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

/** Integração com o sistema: abre a folha de compartilhamento do Android com um texto pronto. */
private fun shareItem(context: Context, item: MediaItem) {
    val text = context.getString(R.string.share_text, item.title, item.progressPercent)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
}
