package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * Ollama version response used for lightweight connection checks.
 */
@Serializable
data class OllamaVersionResponseDto(
    /** Provider version string. */
    val version: String,
)
