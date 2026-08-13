package com.dpashko.localollamaapp.presentation.conversationlist

import com.dpashko.localollamaapp.presentation.ui.models.ConversationUi

data class ConversationListUiState(
    val host: String = "",
    val port: Int = 11434,
    val selectedModelName: String = "",
    val conversations: List<ConversationUi> = emptyList(),
    val errorMessage: String? = null,
)
