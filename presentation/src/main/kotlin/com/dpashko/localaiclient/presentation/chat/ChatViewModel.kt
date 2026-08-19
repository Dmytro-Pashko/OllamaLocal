package com.dpashko.localaiclient.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.conversation.ConversationSettings
import com.dpashko.localaiclient.domain.usecases.CompactConversationUseCase
import com.dpashko.localaiclient.domain.usecases.CreateMessageBranchUseCase
import com.dpashko.localaiclient.domain.usecases.EditMessageAndRegenerateUseCase
import com.dpashko.localaiclient.domain.usecases.EstimateConversationContextUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveConversationBranchesUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveConversationSettingsUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveHasGeneratingMessageUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveMessagesUseCase
import com.dpashko.localaiclient.domain.usecases.RegenerateLastAssistantResponseUseCase
import com.dpashko.localaiclient.domain.usecases.RetryGenerationUseCase
import com.dpashko.localaiclient.domain.usecases.SaveConversationSettingsUseCase
import com.dpashko.localaiclient.domain.usecases.SendMessageUseCase
import com.dpashko.localaiclient.domain.usecases.StopGenerationUseCase
import com.dpashko.localaiclient.domain.usecases.SwitchConversationBranchUseCase
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
    private val observeConversationBranchesUseCase: ObserveConversationBranchesUseCase,
    private val observeHasGeneratingMessageUseCase: ObserveHasGeneratingMessageUseCase,
    private val observeConversationSettingsUseCase: ObserveConversationSettingsUseCase,
    private val estimateConversationContextUseCase: EstimateConversationContextUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val retryGenerationUseCase: RetryGenerationUseCase,
    private val regenerateLastAssistantResponseUseCase: RegenerateLastAssistantResponseUseCase,
    private val createMessageBranchUseCase: CreateMessageBranchUseCase,
    private val compactConversationUseCase: CompactConversationUseCase,
    private val switchConversationBranchUseCase: SwitchConversationBranchUseCase,
    private val editMessageAndRegenerateUseCase: EditMessageAndRegenerateUseCase,
    private val stopGenerationUseCase: StopGenerationUseCase,
    private val saveConversationSettingsUseCase: SaveConversationSettingsUseCase,
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
                refreshContextEstimate()
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

        viewModelScope.launch {
            observeConversationSettingsUseCase(conversationId).collect { settings ->
                settings ?: return@collect
                _uiState.update {
                    it.copy(
                        modelName = settings.modelName,
                        generationTimeoutMinutes = settings.generationTimeoutMinutes,
                        systemPrompt = settings.systemPrompt,
                    )
                }
                refreshContextEstimate()
            }
        }

        viewModelScope.launch {
            observeConversationBranchesUseCase(conversationId).collect { branches ->
                _uiState.update {
                    it.copy(branches = branches)
                }
                refreshContextEstimate()
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
     * Saves generation settings for this conversation.
     */
    fun saveConversationSettings(
        modelName: String,
        timeoutMinutes: String,
        systemPrompt: String,
    ) {
        val minutes = timeoutMinutes.toIntOrNull()
        if (minutes == null) {
            _uiState.update { it.copy(errorMessage = "Enter a timeout from 1 to 1440 minutes.") }
            return
        }

        viewModelScope.launch {
            when (
                val result = saveConversationSettingsUseCase(
                    ConversationSettings.fromMinutes(
                        conversationId = conversationId,
                        modelName = modelName,
                        minutes = minutes,
                        systemPrompt = systemPrompt,
                    ),
                )
            ) {
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }

                is AppResult.Success -> {
                    _uiState.update { it.copy(errorMessage = null) }
                }
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
     * Regenerates the newest assistant response in this conversation.
     */
    fun regenerateLastAssistantResponse() {
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

            val result = regenerateLastAssistantResponseUseCase(
                config = ConnectionConfig(
                    provider = provider,
                    host = host,
                    port = port,
                ),
                conversationId = conversationId,
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
     * Creates an alternative branch from a user message and starts a fresh assistant response.
     */
    fun branchFromMessage(userMessageId: Long) {
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

            val result = createMessageBranchUseCase(
                config = ConnectionConfig(
                    provider = provider,
                    host = host,
                    port = port,
                ),
                conversationId = conversationId,
                userMessageId = userMessageId,
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
     * Switches the visible branch for this conversation.
     */
    fun switchBranch(branchId: Long) {
        if (_uiState.value.isSending || _uiState.value.hasGeneratingMessage) {
            return
        }

        viewModelScope.launch {
            when (
                val result = switchConversationBranchUseCase(
                    conversationId = conversationId,
                    branchId = branchId,
                )
            ) {
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }

                is AppResult.Success -> {
                    _uiState.update { it.copy(errorMessage = null) }
                    refreshContextEstimate()
                }
            }
        }
    }

    /**
     * Summarizes older local history for future provider context.
     */
    fun compactConversation() {
        if (_uiState.value.isSending || _uiState.value.hasGeneratingMessage || _uiState.value.isCompactingConversation) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCompactingConversation = true,
                    errorMessage = null,
                )
            }

            val result = compactConversationUseCase(
                config = ConnectionConfig(
                    provider = provider,
                    host = host,
                    port = port,
                ),
                conversationId = conversationId,
            )

            when (result) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isCompactingConversation = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCompactingConversation = false,
                            errorMessage = null,
                        )
                    }
                    refreshContextEstimate()
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

    private suspend fun refreshContextEstimate() {
        when (val result = estimateConversationContextUseCase(conversationId)) {
            is AppResult.Failure -> Unit
            is AppResult.Success -> {
                _uiState.update { it.copy(contextEstimate = result.data) }
            }
        }
    }
}
