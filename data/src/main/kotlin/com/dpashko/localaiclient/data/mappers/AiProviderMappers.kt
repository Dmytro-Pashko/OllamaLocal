package com.dpashko.localaiclient.data.mappers

import com.dpashko.localaiclient.data.models.remote.OllamaChatMessageDto
import com.dpashko.localaiclient.data.models.remote.LmStudioModelDto
import com.dpashko.localaiclient.data.models.remote.OllamaModelDto
import com.dpashko.localaiclient.domain.models.ai.AiModel
import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.conversation.MessageRole

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
