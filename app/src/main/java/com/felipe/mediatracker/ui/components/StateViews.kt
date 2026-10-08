package com.felipe.mediatracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.felipe.mediatracker.R

/** Estado de carregamento: indicador circular centralizado. */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading_description)
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Estado vazio: ícone neutro, título, explicação e (opcionalmente) uma ação. */
@Composable
fun EmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    MessageView(
        icon = icon,
        iconTint = MaterialTheme.colorScheme.primary,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

/** Estado de erro: ícone de alerta na cor de erro, anunciado aos leitores de tela, com ação de tentar de novo. */
@Composable
fun ErrorView(
    title: String,
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageView(
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.error,
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        actionLabel = retryLabel,
        onAction = onRetry,
    )
}

@Composable
private fun MessageView(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    modifier: Modifier,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // rola quando a fonte do sistema está grande
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorativo: o título já descreve a situação
            tint = iconTint,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
