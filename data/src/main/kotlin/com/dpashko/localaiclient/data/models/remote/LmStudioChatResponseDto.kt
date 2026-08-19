package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * OpenAI-compatible chat completion response returned by LM Studio.
 */
@Serializable
data class LmStudioChatResponseDto(
    /** Candidate completions returned by the provider. */
    val choices: List<LmStudioChatChoiceDto> = emptyList(),
)

/**
 * One LM Studio completion choice.
 */
@Serializable
data class LmStudioChatChoiceDto(
    /** Assistant message payload for the selected choice. */
    val message: OllamaChatMessageDto? = null,
)

/**
 * OpenAI-compatible streaming chat completion chunk returned by LM Studio.
 */
@Serializable
data class LmStudioChatStreamResponseDto(
    /** Candidate streaming deltas returned by the provider. */
    val choices: List<LmStudioChatStreamChoiceDto> = emptyList(),
)

/**
 * One LM Studio streaming completion choice.
 */
@Serializable
data class LmStudioChatStreamChoiceDto(
    /** Partial assistant content for this stream event. */
    val delta: LmStudioChatDeltaDto? = null,
)

/**
 * Partial OpenAI-compatible assistant message content.
 */
@Serializable
data class LmStudioChatDeltaDto(
    /** Text delta emitted by the provider. */
    val content: String? = null,
)
