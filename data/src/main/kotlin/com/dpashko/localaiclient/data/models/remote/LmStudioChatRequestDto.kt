package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * OpenAI-compatible chat completion request sent to LM Studio.
 */
@Serializable
data class LmStudioChatRequestDto(
    /** LM Studio model id. */
    val model: String,
    /** Conversation context translated to chat messages. */
    val messages: List<OllamaChatMessageDto>,
    /** Whether LM Studio should stream response chunks. */
    val stream: Boolean,
)
