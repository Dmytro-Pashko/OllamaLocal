package com.dpashko.localaiclient.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.domain.models.conversation.MessageRole
import com.dpashko.localaiclient.domain.models.conversation.MessageStatus
import com.dpashko.localaiclient.presentation.ui.models.MessageUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    ChatScreen(
        state = state,
        onBack = onBack,
        onMessageChanged = viewModel::onMessageChanged,
        onSendClick = viewModel::sendMessage,
        onCancelEditClick = viewModel::cancelEditingMessage,
        onEditClick = { message -> viewModel.startEditingMessage(message.id, message.content) },
        onRetryClick = viewModel::retryGeneration,
        onStopGenerationClick = viewModel::stopGeneration,
        onSearchQueryChanged = viewModel::onChatSearchQueryChanged,
        onPreviousSearchMatch = viewModel::moveToPreviousSearchMatch,
        onNextSearchMatch = viewModel::moveToNextSearchMatch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCancelEditClick: () -> Unit,
    onEditClick: (MessageUi) -> Unit,
    onRetryClick: (Long) -> Unit,
    onStopGenerationClick: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPreviousSearchMatch: () -> Unit,
    onNextSearchMatch: () -> Unit,
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val hasGeneratingMessage = state.messages.any { it.status == MessageStatus.GENERATING }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var actionMessage by remember { mutableStateOf<MessageUi?>(null) }
    var isSearchMode by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    LaunchedEffect(state.currentSearchMatchMessageId) {
        val messageId = state.currentSearchMatchMessageId ?: return@LaunchedEffect
        val index = state.messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(hasGeneratingMessage) {
        while (hasGeneratingMessage) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
        nowMillis = System.currentTimeMillis()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.chatSearchQuery,
                                onValueChange = onSearchQueryChanged,
                                label = { Text("Search") },
                                singleLine = true,
                            )
                            Text(
                                text = if (state.chatSearchQuery.isBlank()) {
                                    "0/0"
                                } else if (state.searchMatchMessageIds.isEmpty()) {
                                    "0/0"
                                } else {
                                    "${state.currentSearchMatchIndex + 1}/${state.searchMatchMessageIds.size}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    } else {
                        Column {
                            Text("Conversation")
                            Text(
                                text = state.modelName,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchMode) {
                                isSearchMode = false
                                onSearchQueryChanged("")
                            } else {
                                onBack()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (isSearchMode) {
                        TextButton(
                            onClick = onPreviousSearchMatch,
                            enabled = state.searchMatchMessageIds.isNotEmpty(),
                        ) {
                            Text("Prev")
                        }
                        TextButton(
                            onClick = onNextSearchMatch,
                            enabled = state.searchMatchMessageIds.isNotEmpty(),
                        ) {
                            Text("Next")
                        }
                    } else {
                        TextButton(onClick = { isSearchMode = true }) {
                            Text("Search")
                        }
                    }
                    if (state.hasGeneratingMessage) {
                        TextButton(
                            onClick = onStopGenerationClick,
                            enabled = !state.isSending,
                        ) {
                            Text("Stop")
                        }
                    }
                },
            )
        },
        bottomBar = {
            MessageInputBar(
                messageText = state.messageText,
                isSending = state.isSending,
                hasGeneratingMessage = state.hasGeneratingMessage,
                isEditing = state.editingMessageId != null,
                onMessageChanged = onMessageChanged,
                onSendClick = onSendClick,
                onCancelEditClick = onCancelEditClick,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.messages.isEmpty()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    text = "Start the conversation",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.messages,
                        key = { it.id },
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentSearchMatch = message.id == state.currentSearchMatchMessageId,
                            isEditEnabled = !state.isSending,
                            isRetryEnabled = !state.isSending && !state.hasGeneratingMessage,
                            nowMillis = nowMillis,
                            isActionsMenuExpanded = actionMessage?.id == message.id,
                            onMessageLongPress = { actionMessage = message },
                            onDismissActionsMenu = { actionMessage = null },
                            onCopyClick = {
                                clipboardManager.setText(AnnotatedString(message.displayText(nowMillis)))
                                actionMessage = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Message copied")
                                }
                            },
                            onEditClick = {
                                actionMessage = null
                                onEditClick(message)
                            },
                            onRetryClick = onRetryClick,
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
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageUi,
    isCurrentSearchMatch: Boolean,
    isEditEnabled: Boolean,
    isRetryEnabled: Boolean,
    nowMillis: Long,
    isActionsMenuExpanded: Boolean,
    onMessageLongPress: () -> Unit,
    onDismissActionsMenu: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    onRetryClick: (Long) -> Unit,
) {
    val isUser = message.role == MessageRole.USER
    val bubbleText = message.displayText(nowMillis)
    val bubbleColor = when {
        isCurrentSearchMatch -> MaterialTheme.colorScheme.tertiaryContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        message.status == MessageStatus.FAILED -> MaterialTheme.colorScheme.error
        isCurrentSearchMatch -> MaterialTheme.colorScheme.onTertiaryContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onMessageLongPress,
                )
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (message.status == MessageStatus.GENERATING) {
                    CircularProgressIndicator()
                }
                Text(
                    text = bubbleText,
                    color = textColor,
                )
            }
            Text(
                modifier = Modifier.align(Alignment.End),
                text = message.createdAtText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (!isUser && message.status == MessageStatus.FAILED) {
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { onRetryClick(message.id) },
                    enabled = isRetryEnabled,
                ) {
                    Text("Retry")
                }
            }
            DropdownMenu(
                expanded = isActionsMenuExpanded,
                onDismissRequest = onDismissActionsMenu,
            ) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = onCopyClick,
                )
                if (isUser && isEditEnabled) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = onEditClick,
                    )
                }
            }
        }
    }
}

private fun MessageUi.displayText(nowMillis: Long): String =
    when (status) {
        MessageStatus.GENERATING -> content.ifBlank {
            "Generating... ${formatElapsedTime(nowMillis - createdAtMillis)}"
        }
        MessageStatus.FAILED -> content.ifBlank { errorMessage ?: "Generation failed." }
        MessageStatus.CANCELED -> errorMessage ?: "Generation stopped."
        MessageStatus.SENT -> content
    }

private fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis.coerceAtLeast(0L) / 1_000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun MessageInputBar(
    messageText: String,
    isSending: Boolean,
    hasGeneratingMessage: Boolean,
    isEditing: Boolean,
    onMessageChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCancelEditClick: () -> Unit,
) {
    val isSubmitEnabled = if (isEditing) {
        !isSending && messageText.isNotBlank()
    } else {
        !isSending && !hasGeneratingMessage && messageText.isNotBlank()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = messageText,
            onValueChange = onMessageChanged,
            enabled = !isSending,
            minLines = 1,
            maxLines = 4,
            placeholder = { Text(if (isEditing) "Edit message" else "Message") },
        )

        if (isEditing) {
            TextButton(
                onClick = onCancelEditClick,
                enabled = !isSending,
            ) {
                Text("Cancel")
            }
        }

        Button(
            onClick = onSendClick,
            enabled = isSubmitEnabled,
        ) {
            if (isSending) {
                CircularProgressIndicator()
            } else {
                Text(if (isEditing) "Save" else "Send")
            }
        }
    }
}
