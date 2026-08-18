package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * Chat message payload shared by Ollama and OpenAI-compatible provider requests.
 */
@Serializable
data class OllamaChatMessageDto(
    /** Provider role value such as user or assistant. */
    val role: String,
    /** Message content sent to or returned by the provider. */
    val content: String,
)
