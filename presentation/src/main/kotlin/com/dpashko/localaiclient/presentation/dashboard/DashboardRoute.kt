package com.dpashko.localaiclient.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics
import com.dpashko.localaiclient.domain.models.storage.StoragePrivacyStats
import com.dpashko.localaiclient.presentation.R
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
        onDeleteAllSessionData = viewModel::deleteAllSessionData,
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
    onDeleteAllSessionData: () -> Unit,
    onOpenConversation: (AiProvider, String, Int, String, Long) -> Unit,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isDeleteAllDialogVisible by remember { mutableStateOf(false) }
    var isStopAllDialogVisible by remember { mutableStateOf(false) }
    var stopGenerationCandidateId by remember { mutableStateOf<Long?>(null) }

    if (isDeleteAllDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteAllDialogVisible = false },
            title = { Text("Delete all conversations?") },
            text = { Text("This permanently deletes all conversations and messages on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteAllDialogVisible = false
                        onDeleteAllSessionData()
                    },
                ) {
                    Text("Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteAllDialogVisible = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (isStopAllDialogVisible) {
        AlertDialog(
            onDismissRequest = { isStopAllDialogVisible = false },
            title = { Text("Stop all generations?") },
            text = { Text("All active assistant responses will be stopped.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isStopAllDialogVisible = false
                        onStopAllGenerations()
                    },
                    enabled = !state.isStoppingAll,
                ) {
                    Text("Stop all")
                }
            },
            dismissButton = {
                TextButton(onClick = { isStopAllDialogVisible = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    stopGenerationCandidateId?.let { conversationId ->
        AlertDialog(
            onDismissRequest = { stopGenerationCandidateId = null },
            title = { Text("Stop generation?") },
            text = { Text("The selected assistant response will be stopped.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        stopGenerationCandidateId = null
                        onStopGeneration(conversationId)
                    },
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { stopGenerationCandidateId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

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
                .verticalScroll(rememberScrollState())
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
                    onClick = { isStopAllDialogVisible = true },
                    enabled = state.activeGenerations.isNotEmpty() && !state.isStoppingAll,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancel),
                            contentDescription = null,
                        )
                        Text("Stop all")
                    }
                }
            }

            if (state.activeGenerations.isEmpty()) {
                Text(
                    text = "No active generations",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                state.activeGenerations.forEach { generation ->
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
                        onStop = { stopGenerationCandidateId = generation.conversationId },
                    )
                }
            }

            ProviderDiagnosticsSection(
                diagnostics = state.providerDiagnostics,
                isRefreshing = state.isRefreshingDiagnostics,
                onRefresh = onRefreshProviderDiagnostics,
            )

            StoragePrivacySection(
                stats = state.storagePrivacyStats,
                deleteMessage = state.sessionDeleteMessage,
                isDeleting = state.isDeletingSessionData,
                onDeleteAll = { isDeleteAllDialogVisible = true },
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
private fun StoragePrivacySection(
    stats: StoragePrivacyStats?,
    deleteMessage: String?,
    isDeleting: Boolean,
    onDeleteAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Local storage",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (stats == null) {
            Text("Loading storage details")
        } else {
            Text("Active conversations: ${stats.activeConversationCount}")
            Text("Archived conversations: ${stats.archivedConversationCount}")
            Text("Messages: ${stats.messageCount}")
            Text("Active generations: ${stats.activeGenerationCount}")
            Text("Local database: ${stats.databaseSizeBytes.toReadableSize()}")
        }

        deleteMessage?.let { message ->
            Text(message)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDeleteAll,
            enabled = !isDeleting,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                )
                Text("Delete all conversations")
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = null,
                    )
                    Text("Refresh")
                }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancel),
                            contentDescription = null,
                        )
                        Text("Stop")
                    }
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

private fun Long.toReadableSize(): String {
    if (this < 1_024L) {
        return "$this B"
    }
    val kib = this / 1_024.0
    if (kib < 1_024.0) {
        return "%.1f KB".format(kib)
    }
    return "%.1f MB".format(kib / 1_024.0)
}
