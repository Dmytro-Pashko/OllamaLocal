package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.Serializable

@Serializable
data class LmStudioChatResponseDto(
    val choices: List<LmStudioChatChoiceDto> = emptyList(),
)

@Serializable
data class LmStudioChatChoiceDto(
    val message: OllamaChatMessageDto? = null,
)
