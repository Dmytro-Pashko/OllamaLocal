package com.dpashko.localollamaapp.data.mappers

import com.dpashko.localollamaapp.data.models.remote.OllamaChatMessageDto
import com.dpashko.localollamaapp.data.models.remote.LmStudioModelDto
import com.dpashko.localollamaapp.data.models.remote.OllamaModelDto
import com.dpashko.localollamaapp.domain.models.ai.AiModel
import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.conversation.MessageRole

fun OllamaModelDto.toDomain(): AiModel =
    AiModel(
        name = name,
        parameterSize = details?.parameterSize,
        quantizationLevel = details?.quantizationLevel,
    )

fun LmStudioModelDto.toDomain(): AiModel =
    AiModel(
        name = id,
        parameterSize = null,
        quantizationLevel = null,
    )

fun Message.toRemoteDto(): OllamaChatMessageDto =
    OllamaChatMessageDto(
        role = when (role) {
            MessageRole.USER -> "user"
            MessageRole.ASSISTANT -> "assistant"
        },
        content = content,
    )
