package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ollama model metadata nested in model list responses.
 */
@Serializable
data class OllamaModelDetailsDto(
    /** Human-readable parameter size reported by Ollama. */
    @SerialName("parameter_size")
    val parameterSize: String? = null,
    /** Quantization label reported by Ollama. */
    @SerialName("quantization_level")
    val quantizationLevel: String? = null,
)
