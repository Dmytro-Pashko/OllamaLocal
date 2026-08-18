package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * Ollama tags response containing locally installed models.
 */
@Serializable
data class OllamaTagsResponseDto(
    /** Models available for chat requests. */
    val models: List<OllamaModelDto> = emptyList(),
)
