package com.dpashko.localaiclient.presentation.ui.models

import com.dpashko.localaiclient.domain.models.conversation.Conversation
import com.dpashko.localaiclient.presentation.common.toConversationTimeText

/**
 * Conversation list item prepared for rendering.
 */
data class ConversationUi(
    /** Local conversation id. */
    val id: Long,
    /** Conversation title shown as the primary label. */
    val title: String,
    /** Model name shown as secondary context. */
    val modelName: String,
    /** Formatted last-updated text. */
    val updatedAtText: String,
    /** True when the list item should indicate active generation. */
    val hasGeneratingMessage: Boolean,
)

/**
 * Maps domain conversation metadata to list display text.
 */
fun Conversation.toUi(): ConversationUi =
    ConversationUi(
        id = id,
        title = title,
        modelName = modelName,
        updatedAtText = updatedAtMillis.toConversationTimeText(),
        hasGeneratingMessage = hasGeneratingMessage,
    )
