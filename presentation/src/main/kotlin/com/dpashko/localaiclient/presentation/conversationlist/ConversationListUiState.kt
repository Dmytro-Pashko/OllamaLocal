package com.dpashko.localaiclient.presentation.conversationlist

import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.presentation.ui.models.ConversationUi

/**
 * Immutable UI state for the conversation list screen.
 */
data class ConversationListUiState(
    /** Provider selected before entering the conversation list. */
    val provider: AiProvider = AiProvider.OLLAMA,
    /** LAN host used when opening or creating conversations. */
    val host: String = "",
    /** Provider port used when opening or creating conversations. */
    val port: Int = AiProvider.OLLAMA.defaultPort,
    /** Model used for newly created conversations. */
    val selectedModelName: String = "",
    /** Conversations currently available on device. */
    val conversations: List<ConversationUi> = emptyList(),
    /** True when the screen is showing archived conversations. */
    val isArchive: Boolean = false,
    /** Current local conversation search query. */
    val searchQuery: String = "",
    /** True while disconnect is stopping all active generation work. */
    val isDisconnecting: Boolean = false,
    /** User-facing error text for list actions. */
    val errorMessage: String? = null,
)
