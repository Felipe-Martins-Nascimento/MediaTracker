package com.felipe.mediatracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.felipe.mediatracker.domain.MediaItem
import com.felipe.mediatracker.domain.MediaType
import com.felipe.mediatracker.ui.creatorAndYear
import com.felipe.mediatracker.ui.label
import com.felipe.mediatracker.ui.progressSummary
import com.felipe.mediatracker.ui.totalSummary

/**
 * Card reutilizável de um item de mídia, usado na biblioteca e nos resultados de busca.
 *
 * @param onClick `null` deixa o card sem ação de clique.
 * @param showProgress mostra barra de progresso e status (biblioteca) ou apenas o total (busca).
 * @param trailing conteúdo opcional embaixo do texto (ex.: botão "Adicionar").
 */
@Composable
fun MediaCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showProgress: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val content: @Composable () -> Unit = { MediaCardContent(item, showProgress, trailing) }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun MediaCardContent(
    item: MediaItem,
    showProgress: Boolean,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverImage(
            title = item.title,
            coverUrl = item.coverUrl,
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(2f / 3f),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.creatorAndYear()?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TypeBadge(item.type)

            if (showProgress) {
                ProgressSummary(item)
            } else {
                item.totalSummary()?.let { total ->
                    Text(
                        text = total,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.height(4.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun ProgressSummary(item: MediaItem) {
    // Animação implícita: a barra desliza até o novo valor quando o progresso muda.
    val animatedProgress by animateFloatAsState(
        targetValue = item.progressFraction,
        label = "mediaCardProgress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (item.hasKnownTotal) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.progressSummary(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = item.status.label(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Selo com o tipo da mídia (Livro, Filme, Série). */
@Composable
fun TypeBadge(type: MediaType, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = type.label(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
