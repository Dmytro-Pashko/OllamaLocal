package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * Ollama model descriptor returned by the tags endpoint.
 */
@Serializable
data class OllamaModelDto(
    /** User-facing Ollama model name. */
    val name: String,
    /** Provider model identifier when present. */
    val model: String? = null,
    /** Model size in bytes when provided by Ollama. */
    val size: Long? = null,
    /** Content digest reported by Ollama. */
    val digest: String? = null,
    /** Optional metadata used for UI model details. */
    val details: OllamaModelDetailsDto? = null,
)
