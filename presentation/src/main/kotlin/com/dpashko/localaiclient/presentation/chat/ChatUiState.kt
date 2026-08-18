package com.dpashko.localaiclient.presentation.chat

import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.presentation.ui.models.MessageUi

/**
 * Immutable UI state for a single conversation screen.
 */
data class ChatUiState(
    /** Provider used for all generation requests in this screen. */
    val provider: AiProvider = AiProvider.OLLAMA,
    /** LAN host of the selected local provider. */
    val host: String = "",
    /** Provider port used for generation requests. */
    val port: Int = AiProvider.OLLAMA.defaultPort,
    /** Selected model name for this conversation. */
    val modelName: String = "",
    /** Local conversation id displayed by the screen. */
    val conversationId: Long = 0L,
    /** Messages rendered in the chat timeline. */
    val messages: List<MessageUi> = emptyList(),
    /** Current composer text or draft text for an edited message. */
    val messageText: String = "",
    /** Message being edited, or null when composing a new message. */
    val editingMessageId: Long? = null,
    /** True while a send, edit, retry, or stop action is being committed. */
    val isSending: Boolean = false,
    /** True while an assistant response is already generating. */
    val hasGeneratingMessage: Boolean = false,
    /** User-facing error text for the latest failed UI action. */
    val errorMessage: String? = null,
)
