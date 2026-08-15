package com.dpashko.localollamaapp.presentation.conversationlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpashko.localollamaapp.domain.models.connection.AiProvider
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ConversationListRoute(
    viewModel: ConversationListViewModel,
    onBack: () -> Unit,
    onOpenConversation: (AiProvider, String, Int, String, Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

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
            }
        }
    }

    ConversationListScreen(
        state = state,
        onBack = onBack,
        onCreateConversation = viewModel::createConversation,
        onDeleteConversation = viewModel::deleteConversation,
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
    state: ConversationListUiState,
    onBack: () -> Unit,
    onCreateConversation: () -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onOpenConversation: (com.dpashko.localollamaapp.presentation.ui.models.ConversationUi) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateConversation) {
                Text("+")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Tap + to start with ${state.selectedModelName}.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = state.conversations,
                        key = { it.id },
                    ) { conversation ->
                        SwipeConversationItem(
                            conversation = conversation,
                            onDelete = { onDeleteConversation(conversation.id) },
                            onOpen = { onOpenConversation(conversation) },
                        )
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
    conversation: com.dpashko.localollamaapp.presentation.ui.models.ConversationUi,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
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
            )
            HorizontalDivider()
        }
    }
}
