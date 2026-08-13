package com.dpashko.localollamaapp.data.repositories

import com.dpashko.localollamaapp.data.database.dao.ConversationDao
import com.dpashko.localollamaapp.data.mappers.toDomain
import com.dpashko.localollamaapp.data.models.local.ConversationEntity
import com.dpashko.localollamaapp.data.models.local.MessageEntity
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.conversation.Conversation
import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.conversation.MessageRole
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
) : ConversationRepository {
    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeConversations()
            .map { conversations -> conversations.map { it.toDomain() } }

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        conversationDao.observeMessages(conversationId)
            .map { messages -> messages.map { it.toDomain() } }

    override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
        safeDatabaseCall {
            conversationDao.getMessages(conversationId).map { it.toDomain() }
        }

    override suspend fun createConversation(modelName: String): AppResult<Long> =
        safeDatabaseCall {
            val now = System.currentTimeMillis()
            conversationDao.insertConversation(
                ConversationEntity(
                    title = "New conversation",
                    modelName = modelName,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ),
            )
        }

    override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
        safeDatabaseCall {
            conversationDao.deleteConversation(conversationId)
        }

    override suspend fun addMessage(
        conversationId: Long,
        role: MessageRole,
        content: String,
    ): AppResult<Long> =
        safeDatabaseCall {
            val messageCount = conversationDao.getMessageCount(conversationId)
            val now = System.currentTimeMillis()
            val messageId = conversationDao.insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    role = role,
                    content = content,
                    createdAtMillis = now,
                ),
            )

            if (role == MessageRole.USER && messageCount == 0) {
                conversationDao.updateConversationTitleAndTimestamp(
                    conversationId = conversationId,
                    title = content.toConversationTitle(),
                    updatedAtMillis = now,
                )
            } else {
                conversationDao.updateConversationTimestamp(
                    conversationId = conversationId,
                    updatedAtMillis = now,
                )
            }

            messageId
        }

    private suspend fun <T> safeDatabaseCall(block: suspend () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private fun String.toConversationTitle(): String =
        lineSequence()
            .firstOrNull()
            ?.trim()
            ?.take(MAX_TITLE_LENGTH)
            ?.ifBlank { null }
            ?: "New conversation"

    private companion object {
        const val MAX_TITLE_LENGTH = 48
    }
}
