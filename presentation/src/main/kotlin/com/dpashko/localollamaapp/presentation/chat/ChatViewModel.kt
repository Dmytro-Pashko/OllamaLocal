package com.dpashko.localollamaapp.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.usecases.ObserveHasGeneratingMessageUseCase
import com.dpashko.localollamaapp.domain.usecases.ObserveMessagesUseCase
import com.dpashko.localollamaapp.domain.usecases.SendMessageUseCase
import com.dpashko.localollamaapp.presentation.Routes
import com.dpashko.localollamaapp.presentation.common.toUserMessage
import com.dpashko.localollamaapp.presentation.ui.models.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val observeHasGeneratingMessageUseCase: ObserveHasGeneratingMessageUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {
    private val host = Uri.decode(savedStateHandle[Routes.ArgHost] ?: "")
    private val port = savedStateHandle[Routes.ArgPort] ?: 11434
    private val modelName = Uri.decode(savedStateHandle[Routes.ArgModelName] ?: "")
    private val conversationId = savedStateHandle[Routes.ArgConversationId] ?: 0L

    private val _uiState = MutableStateFlow(
        ChatUiState(
            host = host,
            port = port,
            modelName = modelName,
            conversationId = conversationId,
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeMessagesUseCase(conversationId).collect { messages ->
                _uiState.update {
                    it.copy(messages = messages.map { message -> message.toUi() })
                }
            }
        }

        viewModelScope.launch {
            observeHasGeneratingMessageUseCase(conversationId).collect { hasGeneratingMessage ->
                _uiState.update {
                    it.copy(hasGeneratingMessage = hasGeneratingMessage)
                }
            }
        }
    }

    fun onMessageChanged(messageText: String) {
        _uiState.update {
            it.copy(
                messageText = messageText,
                errorMessage = null,
            )
        }
    }

    fun sendMessage() {
        val content = _uiState.value.messageText
        if (content.isBlank() || _uiState.value.isSending || _uiState.value.hasGeneratingMessage) {
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
                config = OllamaConnectionConfig(
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
}
