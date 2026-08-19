package com.dpashko.localaiclient.presentation.serverselection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset
import com.dpashko.localaiclient.presentation.R

@Composable
fun ServerSelectionRoute(
    viewModel: ServerSelectionViewModel,
    onAddServer: () -> Unit,
    onOpenConnected: (AiProvider, String, Int, String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ServerSelectionEvent.OpenConnected -> {
                    onOpenConnected(
                        event.provider,
                        event.host,
                        event.port,
                        event.modelName,
                    )
                }
            }
        }
    }

    ServerSelectionScreen(
        state = state,
        onAddServer = onAddServer,
        onConnectPreset = viewModel::connectPreset,
        onDeletePresetRequest = viewModel::requestDeletePreset,
        onDismissDeletePreset = viewModel::dismissDeletePreset,
        onConfirmDeletePreset = viewModel::confirmDeletePreset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSelectionScreen(
    state: ServerSelectionUiState,
    onAddServer: () -> Unit,
    onConnectPreset: (String) -> Unit,
    onDeletePresetRequest: (String) -> Unit,
    onDismissDeletePreset: () -> Unit,
    onConfirmDeletePreset: () -> Unit,
) {
    state.deletingPresetCandidate?.let {
        AlertDialog(
            onDismissRequest = onDismissDeletePreset,
            title = { Text("Delete server?") },
            text = { Text("This removes the saved server from this device. Conversations are not deleted.") },
            confirmButton = {
                TextButton(onClick = onConfirmDeletePreset) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeletePreset) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Servers") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServer,
                content = { Text("+") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.presets.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No servers yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Tap + to add a server.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.presets,
                        key = { it.id },
                    ) { preset ->
                        SwipeServerPresetItem(
                            preset = preset,
                            isConnecting = state.connectingPresetId == preset.id,
                            onConnect = { onConnectPreset(preset.id) },
                            onDeleteRequest = { onDeletePresetRequest(preset.id) },
                        )
                    }
                }
            }

            state.errorMessage?.let { error ->
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SwipeServerPresetItem(
    preset: ConnectionPreset,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                        text = "Delete",
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        ) {
            ServerPresetItem(
                preset = preset,
                isConnecting = isConnecting,
                onConnect = onConnect,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ServerPresetItem(
    preset: ConnectionPreset,
    isConnecting: Boolean,
    onConnect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_computer),
            contentDescription = null,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = preset.provider.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${preset.host}:${preset.port}",
                style = MaterialTheme.typography.bodySmall,
            )
            preset.modelName?.let { modelName ->
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        TextButton(
            onClick = onConnect,
            enabled = !isConnecting,
        ) {
            if (isConnecting) {
                CircularProgressIndicator()
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                    )
                    Text("Connect")
                }
            }
        }
    }
}
