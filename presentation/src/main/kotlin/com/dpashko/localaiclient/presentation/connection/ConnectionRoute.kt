package com.dpashko.localaiclient.presentation.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ConnectionEvent.PresetSaved -> onBack()
            }
        }
    }

    ConnectionScreen(
        state = state,
        onBack = onBack,
        onProviderSelected = viewModel::onProviderSelected,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onConnectClick = viewModel::connect,
        onRefreshModelsClick = viewModel::refreshModels,
        onModelSelected = viewModel::onModelSelected,
        onSavePreset = viewModel::saveCurrentAsPreset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionScreen(
    state: ConnectionUiState,
    onBack: () -> Unit,
    onProviderSelected: (AiProvider) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onConnectClick: () -> Unit,
    onRefreshModelsClick: () -> Unit,
    onModelSelected: (String) -> Unit,
    onSavePreset: (String) -> Unit,
) {
    var isProviderMenuExpanded by remember { mutableStateOf(false) }
    var isModelMenuExpanded by remember { mutableStateOf(false) }
    var isSavePresetDialogVisible by remember { mutableStateOf(false) }
    var presetNameText by remember { mutableStateOf("") }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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

            Text(
                text = state.providerHealth.displayText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    presetNameText = "${state.provider.displayName} ${state.host}"
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
