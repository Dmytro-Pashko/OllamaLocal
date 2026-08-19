package com.dpashko.localaiclient.presentation.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.domain.models.connection.AiProvider

@Composable
fun ConnectionRoute(
    viewModel: ConnectionViewModel,
    onOpenConversations: (AiProvider, String, Int, String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(
        state.isConnected,
        state.selectedModelName,
    ) {
        val port = state.port.toIntOrNull()
        val modelName = state.selectedModelName
        if (state.isConnected && port != null && modelName != null) {
            onOpenConversations(state.provider, state.host, port, modelName)
        }
    }

    ConnectionScreen(
        state = state,
        onProviderSelected = viewModel::onProviderSelected,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onConnectClick = viewModel::connect,
        onRefreshModelsClick = viewModel::refreshModels,
        onModelSelected = viewModel::onModelSelected,
        onPresetSelected = viewModel::applyPreset,
        onSavePreset = viewModel::saveCurrentAsPreset,
        onDeletePreset = viewModel::deletePreset,
        onOpenConversations = {
            val port = state.port.toIntOrNull() ?: return@ConnectionScreen
            val modelName = state.selectedModelName ?: return@ConnectionScreen
            onOpenConversations(state.provider, state.host, port, modelName)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionScreen(
    state: ConnectionUiState,
    onProviderSelected: (AiProvider) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onConnectClick: () -> Unit,
    onRefreshModelsClick: () -> Unit,
    onModelSelected: (String) -> Unit,
    onPresetSelected: (String) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onOpenConversations: () -> Unit,
) {
    var isProviderMenuExpanded by remember { mutableStateOf(false) }
    var isModelMenuExpanded by remember { mutableStateOf(false) }
    var isPresetMenuExpanded by remember { mutableStateOf(false) }
    var isSavePresetDialogVisible by remember { mutableStateOf(false) }
    var presetNameText by remember { mutableStateOf("") }
    val selectedPreset = state.presets.firstOrNull { it.id == state.selectedPresetId }

    if (isSavePresetDialogVisible) {
        AlertDialog(
            onDismissRequest = { isSavePresetDialogVisible = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = presetNameText,
                    onValueChange = { presetNameText = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isSavePresetDialogVisible = false
                        onSavePreset(presetNameText)
                    },
                    enabled = presetNameText.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSavePresetDialogVisible = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Local AI Client",
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                text = state.providerHealth.displayText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1f),
                    expanded = isPresetMenuExpanded,
                    onExpandedChange = {
                        if (!state.isBusy && state.presets.isNotEmpty()) {
                            isPresetMenuExpanded = !isPresetMenuExpanded
                        }
                    },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = selectedPreset?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.isBusy && state.presets.isNotEmpty(),
                        label = { Text("Presets") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPresetMenuExpanded)
                        },
                    )

                    ExposedDropdownMenu(
                        expanded = isPresetMenuExpanded,
                        onDismissRequest = { isPresetMenuExpanded = false },
                    ) {
                        state.presets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(preset.name)
                                        Text(
                                            text = "${preset.provider.displayName} ${preset.host}:${preset.port}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                                onClick = {
                                    onPresetSelected(preset.id)
                                    isPresetMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        state.selectedPresetId?.let(onDeletePreset)
                    },
                    enabled = !state.isBusy && state.selectedPresetId != null,
                ) {
                    Text("Delete")
                }
            }

            ExposedDropdownMenuBox(
                expanded = isProviderMenuExpanded,
                onExpandedChange = {
                    if (!state.isBusy) {
                        isProviderMenuExpanded = !isProviderMenuExpanded
                    }
                },
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = state.provider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !state.isBusy,
                    label = { Text("Provider") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderMenuExpanded)
                    },
                )

                ExposedDropdownMenu(
                    expanded = isProviderMenuExpanded,
                    onDismissRequest = { isProviderMenuExpanded = false },
                ) {
                    AiProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = {
                                onProviderSelected(provider)
                                isProviderMenuExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.host,
                onValueChange = onHostChanged,
                label = { Text("IP address") },
                singleLine = true,
                enabled = !state.isBusy,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.port,
                onValueChange = onPortChanged,
                label = { Text("Port") },
                singleLine = true,
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onConnectClick,
                enabled = !state.isBusy,
            ) {
                if (state.isConnecting) {
                    CircularProgressIndicator()
                } else {
                    Text("Connect")
                }
            }

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    presetNameText = selectedPreset?.name ?: "${state.provider.displayName} ${state.host}"
                    isSavePresetDialogVisible = true
                },
                enabled = !state.isBusy && state.canSavePreset,
            ) {
                Text("Save preset")
            }

            ExposedDropdownMenuBox(
                expanded = isModelMenuExpanded,
                onExpandedChange = {
                    if (state.isConnected && state.models.isNotEmpty()) {
                        isModelMenuExpanded = !isModelMenuExpanded
                    }
                },
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = state.selectedModelName.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = state.isConnected,
                    label = { Text("Model") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelMenuExpanded)
                    },
                )

                ExposedDropdownMenu(
                    expanded = isModelMenuExpanded,
                    onDismissRequest = { isModelMenuExpanded = false },
                ) {
                    state.models.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(model.name)
                                    model.detailsText?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onModelSelected(model.name)
                                isModelMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefreshModelsClick,
                enabled = state.isConnected && !state.isBusy,
            ) {
                if (state.isRefreshingModels) {
                    CircularProgressIndicator()
                } else {
                    Text("Refresh models")
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenConversations,
                enabled = state.isConnected && state.selectedModelName != null && !state.isBusy,
            ) {
                Text("To Conversations")
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
