package com.dpashko.localaiclient.presentation.conversationlist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.usecases.CreateConversationUseCase
import com.dpashko.localaiclient.domain.usecases.DeleteConversationUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveFilteredConversationsUseCase
import com.dpashko.localaiclient.domain.usecases.SetConversationPinnedUseCase
import com.dpashko.localaiclient.domain.usecases.StopAllGenerationsUseCase
import com.dpashko.localaiclient.presentation.Routes
import com.dpashko.localaiclient.presentation.common.toUserMessage
import com.dpashko.localaiclient.presentation.ui.models.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Coordinates the conversation list, creation, deletion, and disconnect behavior.
 */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeFilteredConversationsUseCase: ObserveFilteredConversationsUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val setConversationPinnedUseCase: SetConversationPinnedUseCase,
    private val stopAllGenerationsUseCase: StopAllGenerationsUseCase,
) : ViewModel() {
    private val provider = AiProvider.fromRouteValue(savedStateHandle[Routes.ArgProvider])
    private val host = Uri.decode(savedStateHandle[Routes.ArgHost] ?: "")
    private val port = savedStateHandle[Routes.ArgPort] ?: provider.defaultPort
    private val selectedModelName = Uri.decode(savedStateHandle[Routes.ArgModelName] ?: "")

    private val _uiState = MutableStateFlow(
        ConversationListUiState(
            provider = provider,
            host = host,
            port = port,
            selectedModelName = selectedModelName,
        ),
    )
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ConversationListEvent>()
    val events: SharedFlow<ConversationListEvent> = _events.asSharedFlow()
    private var observeConversationsJob: Job? = null

    init {
        observeConversations(query = "")
    }

    /**
     * Updates local search and restarts the Room-backed conversation observation.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                errorMessage = null,
            )
        }
        observeConversations(query)
    }

    private fun observeConversations(query: String) {
        observeConversationsJob?.cancel()
        // The list is fully driven by local storage so generation updates survive navigation.
        observeConversationsJob = viewModelScope.launch {
            observeFilteredConversationsUseCase(query).collect { conversations ->
                _uiState.update {
                    it.copy(conversations = conversations.map { conversation -> conversation.toUi() })
                }
            }
        }
    }

    /**
     * Creates a conversation for the selected model and emits navigation to its chat screen.
     */
    fun createConversation() {
        viewModelScope.launch {
            when (val result = createConversationUseCase(selectedModelName)) {
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }

                is AppResult.Success -> {
                    _uiState.update { it.copy(errorMessage = null) }
                    _events.emit(
                        ConversationListEvent.OpenConversation(
                            provider = provider,
                            host = host,
                            port = port,
                            modelName = selectedModelName,
                            conversationId = result.data,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Deletes a conversation after domain logic cancels any related generation work.
     */
    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            when (val result = deleteConversationUseCase(conversationId)) {
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
     * Pins or unpins a conversation in the local list.
     */
    fun setConversationPinned(
        conversationId: Long,
        isPinned: Boolean,
    ) {
        viewModelScope.launch {
            when (val result = setConversationPinnedUseCase(conversationId, isPinned)) {
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
     * Stops all active generations before returning to the connection screen.
     */
    fun disconnect() {
        if (_uiState.value.isDisconnecting) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDisconnecting = true,
                    errorMessage = null,
                )
            }

            when (val result = stopAllGenerationsUseCase()) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDisconnecting = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDisconnecting = false,
                            errorMessage = null,
                        )
                    }
                    _events.emit(ConversationListEvent.Disconnected)
                }
            }
        }
    }
}

/**
 * One-shot conversation list events that should not be stored in [ConversationListUiState].
 */
sealed interface ConversationListEvent {
    /**
     * Navigation command for opening an existing or newly created conversation.
     */
    data class OpenConversation(
        /** Provider to use when continuing the chat. */
        val provider: AiProvider,
        /** LAN host of the connected provider. */
        val host: String,
        /** Provider port. */
        val port: Int,
        /** Selected model name. */
        val modelName: String,
        /** Conversation id to open. */
        val conversationId: Long,
    ) : ConversationListEvent

    /** Navigation command emitted after disconnect completes. */
    data object Disconnected : ConversationListEvent
}
