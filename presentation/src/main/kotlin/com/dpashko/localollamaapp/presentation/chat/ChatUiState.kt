package com.dpashko.localollamaapp.presentation.chat

import com.dpashko.localollamaapp.presentation.ui.models.MessageUi

data class ChatUiState(
    val host: String = "",
    val port: Int = 11434,
    val modelName: String = "",
    val conversationId: Long = 0L,
    val messages: List<MessageUi> = emptyList(),
    val messageText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)
