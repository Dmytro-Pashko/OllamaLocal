package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * Error payload returned by Ollama endpoints.
 */
@Serializable
data class OllamaErrorDto(
    /** Provider error message. */
    val error: String,
)
