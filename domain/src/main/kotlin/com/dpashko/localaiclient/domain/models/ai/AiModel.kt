package com.dpashko.localaiclient.domain.models.ai

/**
 * A model available from a local AI provider.
 */
data class AiModel(
    /** Provider-specific model identifier used when sending chat requests. */
    val name: String,
    /** Human-readable parameter size when the provider exposes it. */
    val parameterSize: String?,
    /** Quantization label when the provider exposes it. */
    val quantizationLevel: String?,
)
