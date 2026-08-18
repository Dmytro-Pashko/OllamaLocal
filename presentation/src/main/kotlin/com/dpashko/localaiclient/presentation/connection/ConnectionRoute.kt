package com.dpashko.localaiclient.presentation.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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

    ConnectionScreen(
        state = state,
        onProviderSelected = viewModel::onProviderSelected,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onConnectClick = viewModel::connect,
        onModelSelected = viewModel::onModelSelected,
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
    onModelSelected: (String) -> Unit,
    onOpenConversations: () -> Unit,
) {
    var isProviderMenuExpanded by remember { mutableStateOf(false) }
    var isModelMenuExpanded by remember { mutableStateOf(false) }

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

            ExposedDropdownMenuBox(
                expanded = isProviderMenuExpanded,
                onExpandedChange = {
                    if (!state.isConnecting) {
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
                    enabled = !state.isConnecting,
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
                enabled = !state.isConnecting,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.port,
                onValueChange = onPortChanged,
                label = { Text("Port") },
                singleLine = true,
                enabled = !state.isConnecting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onConnectClick,
                enabled = !state.isConnecting,
            ) {
                if (state.isConnecting) {
                    CircularProgressIndicator()
                } else {
                    Text("Connect")
                }
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
                onClick = onOpenConversations,
                enabled = state.isConnected && state.selectedModelName != null,
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
