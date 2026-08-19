package com.dpashko.localaiclient.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.usecases.EditMessageAndRegenerateUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveHasGeneratingMessageUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveMessagesUseCase
import com.dpashko.localaiclient.domain.usecases.RetryGenerationUseCase
import com.dpashko.localaiclient.domain.usecases.SendMessageUseCase
import com.dpashko.localaiclient.domain.usecases.StopGenerationUseCase
import com.dpashko.localaiclient.presentation.Routes
import com.dpashko.localaiclient.presentation.common.toUserMessage
import com.dpashko.localaiclient.presentation.ui.models.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Coordinates chat timeline state, composer edits, and generation commands for one conversation.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val observeHasGeneratingMessageUseCase: ObserveHasGeneratingMessageUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val retryGenerationUseCase: RetryGenerationUseCase,
    private val editMessageAndRegenerateUseCase: EditMessageAndRegenerateUseCase,
    private val stopGenerationUseCase: StopGenerationUseCase,
) : ViewModel() {
    private val provider = AiProvider.fromRouteValue(savedStateHandle[Routes.ArgProvider])
    private val host = Uri.decode(savedStateHandle[Routes.ArgHost] ?: "")
    private val port = savedStateHandle[Routes.ArgPort] ?: provider.defaultPort
    private val modelName = Uri.decode(savedStateHandle[Routes.ArgModelName] ?: "")
    private val conversationId = savedStateHandle[Routes.ArgConversationId] ?: 0L

    private val _uiState = MutableStateFlow(
        ChatUiState(
            provider = provider,
            host = host,
            port = port,
            modelName = modelName,
            conversationId = conversationId,
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Keep timeline rendering driven by Room-backed conversation state.
        viewModelScope.launch {
            observeMessagesUseCase(conversationId).collect { messages ->
                _uiState.update {
                    it.copy(messages = messages.map { message -> message.toUi() })
                        .withSearchMatches()
                }
            }
        }

        // Separately observe generation state so controls can block duplicate requests.
        viewModelScope.launch {
            observeHasGeneratingMessageUseCase(conversationId).collect { hasGeneratingMessage ->
                _uiState.update {
                    it.copy(hasGeneratingMessage = hasGeneratingMessage)
                }
            }
        }
    }

    /**
     * Updates the composer draft and clears stale action errors.
     */
    fun onMessageChanged(messageText: String) {
        _uiState.update {
            it.copy(
                messageText = messageText,
                errorMessage = null,
            )
        }
    }

    /**
     * Updates local message search within the currently loaded conversation.
     */
    fun onChatSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                chatSearchQuery = query,
                currentSearchMatchIndex = 0,
                errorMessage = null,
            ).withSearchMatches()
        }
    }

    /**
     * Moves search focus to the next matching message.
     */
    fun moveToNextSearchMatch() {
        _uiState.update {
            if (it.searchMatchMessageIds.isEmpty()) {
                it
            } else {
                it.copy(
                    currentSearchMatchIndex = (it.currentSearchMatchIndex + 1) % it.searchMatchMessageIds.size,
                )
            }
        }
    }

    /**
     * Moves search focus to the previous matching message.
     */
    fun moveToPreviousSearchMatch() {
        _uiState.update {
            if (it.searchMatchMessageIds.isEmpty()) {
                it
            } else {
                it.copy(
                    currentSearchMatchIndex = (
                        it.currentSearchMatchIndex - 1 + it.searchMatchMessageIds.size
                        ) % it.searchMatchMessageIds.size,
                )
            }
        }
    }

    /**
     * Moves the composer into edit mode for an existing user message.
     */
    fun startEditingMessage(
        messageId: Long,
        content: String,
    ) {
        if (_uiState.value.isSending) {
            return
        }

        _uiState.update {
            it.copy(
                editingMessageId = messageId,
                messageText = content,
                errorMessage = null,
            )
        }
    }

    /**
     * Leaves edit mode and clears the current draft.
     */
    fun cancelEditingMessage() {
        _uiState.update {
            it.copy(
                editingMessageId = null,
                messageText = "",
                errorMessage = null,
            )
        }
    }

    /**
     * Sends a new message or saves the edited message when edit mode is active.
     */
    fun sendMessage() {
        val state = _uiState.value
        val content = state.messageText
        state.editingMessageId?.let { messageId ->
            saveEditedMessage(messageId, content)
            return
        }

        if (content.isBlank() || state.isSending || state.hasGeneratingMessage) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    messageText = "",
                    errorMessage = null,
                )
            }

            val result = sendMessageUseCase(
                config = ConnectionConfig(
                    provider = provider,
                    host = host,
                    port = port,
                ),
                conversationId = conversationId,
                modelName = modelName,
                content = content,
            )

            when (result) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * Persists edited content and starts replacement generation from the edited context.
     */
    private fun saveEditedMessage(
        messageId: Long,
        content: String,
    ) {
        if (content.isBlank() || _uiState.value.isSending) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    messageText = "",
                    editingMessageId = null,
                    errorMessage = null,
                )
            }

            val result = editMessageAndRegenerateUseCase(
                config = ConnectionConfig(
                    provider = provider,
                    host = host,
                    port = port,
                ),
                conversationId = conversationId,
                modelName = modelName,
                messageId = messageId,
                content = content,
            )

            when (result) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            editingMessageId = messageId,
                            messageText = content,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * Retries generation for an existing assistant placeholder.
     */
    fun retryGeneration(assistantMessageId: Long) {
        if (_uiState.value.isSending || _uiState.value.hasGeneratingMessage) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    errorMessage = null,
                )
            }

            val result = retryGenerationUseCase(
                config = ConnectionConfig(
                    provider = provider,
                    host = host,
                    port = port,
                ),
                conversationId = conversationId,
                modelName = modelName,
                assistantMessageId = assistantMessageId,
            )

            when (result) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * Stops the active generation for this conversation and marks the placeholder canceled.
     */
    fun stopGeneration() {
        if (_uiState.value.isSending || !_uiState.value.hasGeneratingMessage) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    errorMessage = null,
                )
            }

            when (val result = stopGenerationUseCase(conversationId)) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    private fun ChatUiState.withSearchMatches(): ChatUiState {
        val query = chatSearchQuery.trim()
        val matches = if (query.isBlank()) {
            emptyList()
        } else {
            messages
                .filter { it.content.contains(query, ignoreCase = true) }
                .map { it.id }
        }
        return copy(
            searchMatchMessageIds = matches,
            currentSearchMatchIndex = currentSearchMatchIndex.coerceIn(
                minimumValue = 0,
                maximumValue = (matches.size - 1).coerceAtLeast(0),
            ),
        )
    }
}
