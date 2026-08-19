package com.dpashko.localaiclient.presentation.conversationlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.presentation.ui.models.ConversationUi
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ConversationListRoute(
    modifier: Modifier = Modifier,
    viewModel: ConversationListViewModel,
    isArchive: Boolean = false,
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenConversation: (AiProvider, String, Int, String, Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(isArchive) {
        viewModel.setArchiveFilter(isArchive)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ConversationListEvent.OpenConversation -> {
                    onOpenConversation(
                        event.provider,
                        event.host,
                        event.port,
                        event.modelName,
                        event.conversationId,
                    )
                }

                ConversationListEvent.Disconnected -> onDisconnect()
            }
        }
    }

    ConversationListScreen(
        modifier = modifier,
        state = state,
        onOpenSettings = onOpenSettings,
        onDisconnect = viewModel::disconnect,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCreateConversation = viewModel::createConversation,
        onDeleteConversation = viewModel::deleteConversation,
        onRenameConversation = viewModel::renameConversation,
        onSetConversationPinned = viewModel::setConversationPinned,
        onSetConversationArchived = viewModel::setConversationArchived,
        onOpenConversation = { conversation ->
            onOpenConversation(
                state.provider,
                state.host,
                state.port,
                conversation.modelName,
                conversation.id,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListScreen(
    modifier: Modifier = Modifier,
    state: ConversationListUiState,
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCreateConversation: () -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onRenameConversation: (Long, String) -> Unit,
    onSetConversationPinned: (Long, Boolean) -> Unit,
    onSetConversationArchived: (Long, Boolean) -> Unit,
    onOpenConversation: (ConversationUi) -> Unit,
) {
    var pendingDeleteConversation by remember { mutableStateOf<ConversationUi?>(null) }
    var pendingRenameConversation by remember { mutableStateOf<ConversationUi?>(null) }
    var renameText by remember { mutableStateOf("") }

    pendingDeleteConversation?.let { conversation ->
        AlertDialog(
            onDismissRequest = { pendingDeleteConversation = null },
            title = { Text("Delete conversation?") },
            text = { Text("This conversation will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteConversation = null
                        onDeleteConversation(conversation.id)
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConversation = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    pendingRenameConversation?.let { conversation ->
        AlertDialog(
            onDismissRequest = { pendingRenameConversation = null },
            title = { Text("Rename conversation") },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    isError = renameText.length > 48,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRenameConversation = null
                        onRenameConversation(conversation.id, renameText)
                    },
                    enabled = renameText.isNotBlank() && renameText.length <= 48,
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRenameConversation = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isArchive) {
                            "Archive"
                        } else {
                            "${state.provider.displayName} (${state.host})"
                        },
                    )
                },
                actions = {
                    TextButton(
                        onClick = onOpenSettings,
                        enabled = !state.isDisconnecting,
                    ) {
                        Text("Settings")
                    }
                    TextButton(
                        onClick = onDisconnect,
                        enabled = !state.isDisconnecting,
                    ) {
                        Text("Disconnect")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.isArchive) {
                FloatingActionButton(onClick = onCreateConversation) {
                    Text("+")
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    enabled = !state.isDisconnecting,
                    label = { Text("Search conversations") },
                    singleLine = true,
                )

                if (state.conversations.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (state.searchQuery.isBlank()) {
                                if (state.isArchive) "No archived conversations" else "No conversations yet"
                            } else {
                                "No matching conversations"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.searchQuery.isBlank() && !state.isArchive) {
                            Text(
                                text = "Tap + to start with ${state.selectedModelName}.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.conversations,
                            key = { it.id },
                        ) { conversation ->
                            SwipeConversationItem(
                                conversation = conversation,
                                onDeleteRequest = { pendingDeleteConversation = conversation },
                                onRenameRequest = {
                                    renameText = conversation.title
                                    pendingRenameConversation = conversation
                                },
                                onTogglePinned = {
                                    onSetConversationPinned(conversation.id, !conversation.isPinned)
                                },
                                onToggleArchived = {
                                    onSetConversationArchived(conversation.id, !conversation.isArchived)
                                },
                                onOpen = { onOpenConversation(conversation) },
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeConversationItem(
    conversation: ConversationUi,
    onDeleteRequest: () -> Unit,
    onRenameRequest: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleArchived: () -> Unit,
    onOpen: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest()
                false
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        Column {
            ListItem(
                modifier = Modifier.clickable(onClick = onOpen),
                headlineContent = { Text(conversation.title) },
                supportingContent = {
                    Text("${conversation.modelName} • ${conversation.updatedAtText}")
                },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onTogglePinned) {
                            Text(if (conversation.isPinned) "Unpin" else "Pin")
                        }
                        TextButton(onClick = onRenameRequest) {
                            Text("Rename")
                        }
                        TextButton(onClick = onToggleArchived) {
                            Text(if (conversation.isArchived) "Unarchive" else "Archive")
                        }
                        if (conversation.hasGeneratingMessage) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                },
            )
            HorizontalDivider()
        }
    }
}
