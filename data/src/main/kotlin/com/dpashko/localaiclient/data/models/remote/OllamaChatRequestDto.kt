package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * Native Ollama chat request.
 */
@Serializable
data class OllamaChatRequestDto(
    /** Ollama model name. */
    val model: String,
    /** Conversation context translated to Ollama messages. */
    val messages: List<OllamaChatMessageDto>,
    /** Whether Ollama should return newline-delimited streaming chunks. */
    val stream: Boolean,
)
