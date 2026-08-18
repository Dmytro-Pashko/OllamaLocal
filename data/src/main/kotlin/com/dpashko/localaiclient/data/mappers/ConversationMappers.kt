package com.dpashko.localaiclient.data.mappers

import com.dpashko.localaiclient.data.models.local.ConversationListItemEntity
import com.dpashko.localaiclient.data.models.local.MessageEntity
import com.dpashko.localaiclient.domain.models.conversation.Conversation
import com.dpashko.localaiclient.domain.models.conversation.Message

fun ConversationListItemEntity.toDomain(): Conversation =
    Conversation(
        id = id,
        title = title,
        modelName = modelName,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        hasGeneratingMessage = hasGeneratingMessage,
    )

fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        status = status,
        errorMessage = errorMessage,
        createdAtMillis = createdAtMillis,
    )
