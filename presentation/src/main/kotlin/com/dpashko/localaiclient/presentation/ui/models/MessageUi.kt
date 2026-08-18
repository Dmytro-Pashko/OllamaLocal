package com.dpashko.localaiclient.presentation.ui.models

import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.conversation.MessageRole
import com.dpashko.localaiclient.domain.models.conversation.MessageStatus
import com.dpashko.localaiclient.presentation.common.toMessageTimeText

/**
 * Chat message prepared for timeline rendering.
 */
data class MessageUi(
    /** Local message id. */
    val id: Long,
    /** Sender role used for bubble alignment and styling. */
    val role: MessageRole,
    /** Message body shown in the chat bubble. */
    val content: String,
    /** Lifecycle state used for progress, retry, and cancellation UI. */
    val status: MessageStatus,
    /** Error text shown for failed assistant messages. */
    val errorMessage: String?,
    /** Raw timestamp retained for stable sorting or comparisons. */
    val createdAtMillis: Long,
    /** Formatted timestamp shown in the chat timeline. */
    val createdAtText: String,
)

/**
 * Maps a persisted domain message into timeline display state.
 */
fun Message.toUi(): MessageUi =
    MessageUi(
        id = id,
        role = role,
        content = content,
        status = status,
        errorMessage = errorMessage,
        createdAtMillis = createdAtMillis,
        createdAtText = createdAtMillis.toMessageTimeText(),
    )
