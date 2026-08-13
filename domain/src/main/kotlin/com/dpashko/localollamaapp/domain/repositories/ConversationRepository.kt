package com.dpashko.localollamaapp.domain.repositories

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.conversation.Conversation
import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.conversation.MessageRole
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(): Flow<List<Conversation>>

    fun observeMessages(conversationId: Long): Flow<List<Message>>

    suspend fun getMessages(conversationId: Long): AppResult<List<Message>>

    suspend fun createConversation(modelName: String): AppResult<Long>

    suspend fun deleteConversation(conversationId: Long): AppResult<Unit>

    suspend fun addMessage(
        conversationId: Long,
        role: MessageRole,
        content: String,
    ): AppResult<Long>
}
