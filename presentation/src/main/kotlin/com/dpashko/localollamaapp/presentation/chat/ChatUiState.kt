package com.dpashko.localollamaapp.presentation.chat

import com.dpashko.localollamaapp.domain.models.connection.AiProvider
import com.dpashko.localollamaapp.presentation.ui.models.MessageUi

data class ChatUiState(
    val provider: AiProvider = AiProvider.OLLAMA,
    val host: String = "",
    val port: Int = AiProvider.OLLAMA.defaultPort,
    val modelName: String = "",
    val conversationId: Long = 0L,
    val messages: List<MessageUi> = emptyList(),
    val messageText: String = "",
    val isSending: Boolean = false,
    val hasGeneratingMessage: Boolean = false,
    val errorMessage: String? = null,
)
