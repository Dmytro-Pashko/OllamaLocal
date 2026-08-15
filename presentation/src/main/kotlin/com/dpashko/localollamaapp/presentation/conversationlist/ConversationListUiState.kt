package com.dpashko.localollamaapp.presentation.conversationlist

import com.dpashko.localollamaapp.domain.models.connection.AiProvider
import com.dpashko.localollamaapp.presentation.ui.models.ConversationUi

data class ConversationListUiState(
    val provider: AiProvider = AiProvider.OLLAMA,
    val host: String = "",
    val port: Int = AiProvider.OLLAMA.defaultPort,
    val selectedModelName: String = "",
    val conversations: List<ConversationUi> = emptyList(),
    val errorMessage: String? = null,
)
