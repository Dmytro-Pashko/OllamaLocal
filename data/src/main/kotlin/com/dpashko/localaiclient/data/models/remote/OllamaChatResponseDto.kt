package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Native Ollama chat response chunk or final response.
 */
@Serializable
data class OllamaChatResponseDto(
    /** Model name echoed by Ollama. */
    val model: String? = null,
    /** Provider timestamp string for this response chunk. */
    @SerialName("created_at")
    val createdAt: String? = null,
    /** Assistant message content for this chunk. */
    val message: OllamaChatMessageDto? = null,
    /** Whether Ollama considers the response complete. */
    val done: Boolean = true,
    /** Provider-specific completion reason. */
    @SerialName("done_reason")
    val doneReason: String? = null,
)
