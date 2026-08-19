package com.dpashko.localaiclient.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics
import com.dpashko.localaiclient.presentation.common.toConversationTimeText

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel,
    onOpenConversation: (AiProvider, String, Int, String, Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    DashboardScreen(
        modifier = modifier,
        state = state,
        onStopGeneration = viewModel::stopGeneration,
        onStopAllGenerations = viewModel::stopAllGenerations,
        onRefreshProviderDiagnostics = viewModel::refreshProviderDiagnostics,
        onOpenConversation = onOpenConversation,
    )
}

@Composable
private fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardUiState,
    onStopGeneration: (Long) -> Unit,
    onStopAllGenerations: () -> Unit,
    onRefreshProviderDiagnostics: () -> Unit,
    onOpenConversation: (AiProvider, String, Int, String, Long) -> Unit,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(state.activeGenerations.isNotEmpty()) {
        while (state.activeGenerations.isNotEmpty()) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
        nowMillis = System.currentTimeMillis()
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Active generations",
                    style = MaterialTheme.typography.titleLarge,
                )
                Button(
                    onClick = onStopAllGenerations,
                    enabled = state.activeGenerations.isNotEmpty() && !state.isStoppingAll,
                ) {
                    Text("Stop all")
                }
            }

            if (state.activeGenerations.isEmpty()) {
                Text(
                    text = "No active generations",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn {
                    items(
                        items = state.activeGenerations,
                        key = { it.conversationId },
                    ) { generation ->
                        ActiveGenerationRow(
                            generation = generation,
                            elapsedMillis = nowMillis - generation.assistantMessageCreatedAtMillis,
                            onOpen = {
                                onOpenConversation(
                                    state.provider,
                                    state.host,
                                    state.port,
                                    generation.modelName,
                                    generation.conversationId,
                                )
                            },
                            onStop = { onStopGeneration(generation.conversationId) },
                        )
                    }
                }
            }

            ProviderDiagnosticsSection(
                diagnostics = state.providerDiagnostics,
                isRefreshing = state.isRefreshingDiagnostics,
                onRefresh = onRefreshProviderDiagnostics,
            )

            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ProviderDiagnosticsSection(
    diagnostics: ProviderDiagnostics?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Connection details",
                style = MaterialTheme.typography.titleLarge,
            )
            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
            ) {
                Text("Refresh")
            }
        }

        if (diagnostics == null) {
            Text(
                text = "Not checked",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text("${diagnostics.provider.displayName} • ${diagnostics.providerUrl}")
            Text("Status: ${diagnostics.health.displayText}")
            Text("Latency: ${diagnostics.latencyMillis?.let { "$it ms" } ?: "Unknown"}")
            Text("Models: ${diagnostics.modelCount?.toString() ?: "Unknown"}")
            Text("Last checked: ${diagnostics.lastCheckedAtMillis.toConversationTimeText()}")
            diagnostics.lastError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ActiveGenerationRow(
    generation: ActiveGenerationUi,
    elapsedMillis: Long,
    onOpen: () -> Unit,
    onStop: () -> Unit,
) {
    Column {
        ListItem(
            modifier = Modifier.clickable(onClick = onOpen),
            headlineContent = { Text(generation.title) },
            supportingContent = {
                Text(
                    "${generation.modelName} • ${formatElapsedTime(elapsedMillis)}" +
                        if (generation.isArchived) " • Archived" else "",
                )
            },
            trailingContent = {
                TextButton(onClick = onStop) {
                    Text("Stop")
                }
            },
        )
        HorizontalDivider()
    }
}

private fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis.coerceAtLeast(0L) / 1_000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
