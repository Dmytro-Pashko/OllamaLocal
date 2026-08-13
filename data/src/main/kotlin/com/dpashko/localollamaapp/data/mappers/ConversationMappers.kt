package com.dpashko.localollamaapp.data.mappers

import com.dpashko.localollamaapp.data.models.local.ConversationEntity
import com.dpashko.localollamaapp.data.models.local.MessageEntity
import com.dpashko.localollamaapp.domain.models.conversation.Conversation
import com.dpashko.localollamaapp.domain.models.conversation.Message

fun ConversationEntity.toDomain(): Conversation =
    Conversation(
        id = id,
        title = title,
        modelName = modelName,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )

fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        createdAtMillis = createdAtMillis,
    )
