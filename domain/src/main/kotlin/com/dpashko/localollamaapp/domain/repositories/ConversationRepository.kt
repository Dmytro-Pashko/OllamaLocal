package com.dpashko.localollamaapp.domain.repositories

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.conversation.Conversation
import com.dpashko.localollamaapp.domain.models.conversation.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(): Flow<List<Conversation>>

    fun observeMessages(conversationId: Long): Flow<List<Message>>

    fun observeHasGeneratingMessage(conversationId: Long): Flow<Boolean>

    suspend fun getContextMessages(conversationId: Long): AppResult<List<Message>>

    suspend fun messageExists(messageId: Long): AppResult<Boolean>

    suspend fun createConversation(modelName: String): AppResult<Long>

    suspend fun deleteConversation(conversationId: Long): AppResult<Unit>

    suspend fun addUserMessage(
        conversationId: Long,
        content: String,
    ): AppResult<Long>

    suspend fun addAssistantPlaceholder(conversationId: Long): AppResult<Long>

    suspend fun completeAssistantMessage(
        messageId: Long,
        content: String,
    ): AppResult<Unit>

    suspend fun failAssistantMessage(
        messageId: Long,
        errorMessage: String,
    ): AppResult<Unit>

    suspend fun retryAssistantMessage(messageId: Long): AppResult<Unit>
}
