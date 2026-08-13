package com.dpashko.localollamaapp.presentation.conversationlist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.usecases.CreateConversationUseCase
import com.dpashko.localollamaapp.domain.usecases.DeleteConversationUseCase
import com.dpashko.localollamaapp.domain.usecases.ObserveConversationsUseCase
import com.dpashko.localollamaapp.presentation.Routes
import com.dpashko.localollamaapp.presentation.common.toUserMessage
import com.dpashko.localollamaapp.presentation.ui.models.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
) : ViewModel() {
    private val host = Uri.decode(savedStateHandle[Routes.ArgHost] ?: "")
    private val port = savedStateHandle[Routes.ArgPort] ?: 11434
    private val selectedModelName = Uri.decode(savedStateHandle[Routes.ArgModelName] ?: "")

    private val _uiState = MutableStateFlow(
        ConversationListUiState(
            host = host,
            port = port,
            selectedModelName = selectedModelName,
        ),
    )
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ConversationListEvent>()
    val events: SharedFlow<ConversationListEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeConversationsUseCase().collect { conversations ->
                _uiState.update {
                    it.copy(conversations = conversations.map { conversation -> conversation.toUi() })
                }
            }
        }
    }

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
}

sealed interface ConversationListEvent {
    data class OpenConversation(
        val host: String,
        val port: Int,
        val modelName: String,
        val conversationId: Long,
    ) : ConversationListEvent
}
