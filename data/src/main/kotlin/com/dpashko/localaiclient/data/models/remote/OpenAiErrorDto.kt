package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * OpenAI-compatible error envelope returned by LM Studio.
 */
@Serializable
data class OpenAiErrorDto(
    /** Structured error body, when the provider returns one. */
    val error: OpenAiErrorBodyDto? = null,
)

/**
 * OpenAI-compatible error body.
 */
@Serializable
data class OpenAiErrorBodyDto(
    /** Provider error message. */
    val message: String? = null,
)
