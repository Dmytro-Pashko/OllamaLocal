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
