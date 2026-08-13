package com.dpashko.localollamaapp.data.mappers

import com.dpashko.localollamaapp.data.models.remote.OllamaChatMessageDto
import com.dpashko.localollamaapp.data.models.remote.OllamaModelDto
import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.conversation.MessageRole
import com.dpashko.localollamaapp.domain.models.ollama.OllamaModel

fun OllamaModelDto.toDomain(): OllamaModel =
    OllamaModel(
        name = name,
        parameterSize = details?.parameterSize,
        quantizationLevel = details?.quantizationLevel,
    )

fun Message.toOllamaDto(): OllamaChatMessageDto =
    OllamaChatMessageDto(
        role = when (role) {
            MessageRole.USER -> "user"
            MessageRole.ASSISTANT -> "assistant"
        },
        content = content,
    )
