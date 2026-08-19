package com.dpashko.localaiclient.presentation.serverselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset

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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSelectionScreen(
    state: ServerSelectionUiState,
    onAddServer: () -> Unit,
    onConnectPreset: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Servers") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddServer,
                enabled = !state.isConnecting,
            ) {
                Text("Add server")
            }

            if (state.presets.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "No servers yet",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.presets,
                        key = { it.id },
                    ) { preset ->
                        ServerPresetItem(
                            preset = preset,
                            isConnecting = state.connectingPresetId == preset.id,
                            onConnect = { onConnectPreset(preset.id) },
                        )
                    }
                }
            }

            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ServerPresetItem(
    preset: ConnectionPreset,
    isConnecting: Boolean,
    onConnect: () -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(preset.name) },
            supportingContent = {
                Column {
                    Text(preset.provider.displayName)
                    Text("${preset.host}:${preset.port}")
                    preset.modelName?.let { modelName ->
                        Text(modelName)
                    }
                }
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onConnect,
                        enabled = !isConnecting,
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator()
                        } else {
                            Text("Connect")
                        }
                    }
                }
            },
        )
        HorizontalDivider()
    }
}
