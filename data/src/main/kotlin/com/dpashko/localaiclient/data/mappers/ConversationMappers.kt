package com.dpashko.localaiclient.data.mappers

import com.dpashko.localaiclient.data.models.local.ActiveGenerationEntity
import com.dpashko.localaiclient.data.models.local.ConversationBranchEntity
import com.dpashko.localaiclient.data.models.local.ConversationListItemEntity
import com.dpashko.localaiclient.data.models.local.ConversationSettingsEntity
import com.dpashko.localaiclient.data.models.local.MessageEntity
import com.dpashko.localaiclient.domain.models.conversation.ActiveGeneration
import com.dpashko.localaiclient.domain.models.conversation.Conversation
import com.dpashko.localaiclient.domain.models.conversation.ConversationBranch
import com.dpashko.localaiclient.domain.models.conversation.ConversationSettings
import com.dpashko.localaiclient.domain.models.conversation.Message

fun ConversationListItemEntity.toDomain(): Conversation =
    Conversation(
        id = id,
        title = title,
        isPinned = isPinned,
        isArchived = isArchived,
        archivedAtMillis = archivedAtMillis,
        modelName = modelName,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        hasGeneratingMessage = hasGeneratingMessage,
    )

fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        conversationId = conversationId,
        branchId = branchId,
        role = role,
        content = content,
        status = status,
        errorMessage = errorMessage,
        createdAtMillis = createdAtMillis,
    )

fun ConversationSettingsEntity.toDomain(): ConversationSettings =
    ConversationSettings(
        conversationId = id,
        modelName = modelName,
        generationTimeoutMillis = generationTimeoutMillis,
        systemPrompt = systemPrompt,
    )

fun ConversationBranchEntity.toDomain(activeBranchId: Long): ConversationBranch =
    ConversationBranch(
        id = id,
        conversationId = conversationId,
        title = title,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        isActive = id == activeBranchId,
    )

fun ActiveGenerationEntity.toDomain(): ActiveGeneration =
    ActiveGeneration(
        conversationId = conversationId,
        title = title,
        modelName = modelName,
        isArchived = isArchived,
        assistantMessageCreatedAtMillis = assistantMessageCreatedAtMillis,
    )
