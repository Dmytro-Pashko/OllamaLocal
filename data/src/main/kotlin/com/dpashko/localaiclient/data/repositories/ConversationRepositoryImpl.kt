package com.dpashko.localaiclient.data.repositories

import android.content.Context
import androidx.room.withTransaction
import com.dpashko.localaiclient.data.database.LocalAiClientDatabase
import com.dpashko.localaiclient.data.database.dao.ConversationDao
import com.dpashko.localaiclient.data.mappers.toDomain
import com.dpashko.localaiclient.data.models.local.ConversationEntity
import com.dpashko.localaiclient.data.models.local.MessageEntity
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.conversation.Conversation
import com.dpashko.localaiclient.domain.models.conversation.ConversationSettings
import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.conversation.MessageRole
import com.dpashko.localaiclient.domain.models.conversation.MessageStatus
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.storage.StoragePrivacyStats
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: LocalAiClientDatabase,
    private val conversationDao: ConversationDao,
) : ConversationRepository {
    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeConversations()
            .map { conversations -> conversations.map { it.toDomain() } }

    override fun observeConversations(query: String): Flow<List<Conversation>> =
        observeConversations(query = query, isArchived = false)

    override fun observeConversations(
        query: String,
        isArchived: Boolean,
    ): Flow<List<Conversation>> =
        conversationDao.observeConversations(
            query = query.trim(),
            isArchived = isArchived,
        )
            .map { conversations -> conversations.map { it.toDomain() } }

    override fun observeMessages(conversationId: Long): Flow<List<Message>> =
        conversationDao.observeMessages(conversationId)
            .map { messages -> messages.map { it.toDomain() } }

    override fun observeHasGeneratingMessage(conversationId: Long): Flow<Boolean> =
        conversationDao.observeHasGeneratingMessage(conversationId)

    override fun observeActiveGenerations() =
        conversationDao.observeActiveGenerations()
            .map { generations -> generations.map { it.toDomain() } }

    override fun observeStoragePrivacyStats(): Flow<StoragePrivacyStats> =
        conversationDao.observeStoragePrivacyStats()
            .map { stats ->
                StoragePrivacyStats(
                    activeConversationCount = stats.activeConversationCount,
                    archivedConversationCount = stats.archivedConversationCount,
                    messageCount = stats.messageCount,
                    activeGenerationCount = stats.activeGenerationCount,
                    databaseSizeBytes = databaseSizeBytes(),
                )
            }

    override fun observeConversationSettings(conversationId: Long): Flow<ConversationSettings?> =
        conversationDao.observeConversationSettings(conversationId)
            .map { settings -> settings?.toDomain() }

    override suspend fun getConversationSettings(conversationId: Long): AppResult<ConversationSettings> =
        safeDatabaseCall {
            conversationDao.getConversationSettings(conversationId)?.toDomain()
                ?: throw IllegalStateException("Conversation settings cannot be loaded.")
        }

    override suspend fun getContextMessages(conversationId: Long): AppResult<List<Message>> =
        safeDatabaseCall {
            conversationDao.getContextMessages(conversationId).map { it.toDomain() }
        }

    override suspend fun messageExists(messageId: Long): AppResult<Boolean> =
        safeDatabaseCall {
            conversationDao.messageExists(messageId)
        }

    override suspend fun createConversation(
        modelName: String,
        generationTimeoutMillis: Long,
    ): AppResult<Long> =
        safeDatabaseCall {
            val now = System.currentTimeMillis()
            conversationDao.insertConversation(
                ConversationEntity(
                    title = "New conversation",
                    isPinned = false,
                    isTitleManuallyEdited = false,
                    isArchived = false,
                    archivedAtMillis = null,
                    modelName = modelName,
                    generationTimeoutMillis = generationTimeoutMillis,
                    systemPrompt = "",
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ),
            )
        }

    override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
        safeDatabaseCall {
            conversationDao.deleteConversation(conversationId)
        }

    override suspend fun deleteAllSessionData(): AppResult<Unit> =
        safeDatabaseCall {
            database.withTransaction {
                conversationDao.deleteAllMessages()
                conversationDao.deleteAllConversations()
            }
        }

    override suspend fun setConversationPinned(
        conversationId: Long,
        isPinned: Boolean,
    ): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.updateConversationPinned(
                conversationId = conversationId,
                isPinned = isPinned,
            )
            if (updatedRows == 0) {
                throw IllegalStateException("Conversation cannot be updated.")
            }
        }

    override suspend fun setConversationArchived(
        conversationId: Long,
        isArchived: Boolean,
    ): AppResult<Unit> =
        safeDatabaseCall {
            val now = System.currentTimeMillis()
            val updatedRows = conversationDao.updateConversationArchived(
                conversationId = conversationId,
                isArchived = isArchived,
                archivedAtMillis = if (isArchived) now else null,
                updatedAtMillis = now,
            )
            if (updatedRows == 0) {
                throw IllegalStateException("Conversation cannot be archived.")
            }
        }

    override suspend fun saveConversationSettings(settings: ConversationSettings): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.updateConversationSettings(
                conversationId = settings.conversationId,
                modelName = settings.modelName,
                generationTimeoutMillis = settings.generationTimeoutMillis,
                systemPrompt = settings.systemPrompt,
                updatedAtMillis = System.currentTimeMillis(),
            )
            if (updatedRows == 0) {
                throw IllegalStateException("Conversation settings cannot be saved.")
            }
        }

    override suspend fun renameConversation(
        conversationId: Long,
        title: String,
    ): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.renameConversation(
                conversationId = conversationId,
                title = title,
                updatedAtMillis = System.currentTimeMillis(),
            )
            if (updatedRows == 0) {
                throw IllegalStateException("Conversation cannot be renamed.")
            }
        }

    override suspend fun addUserMessage(
        conversationId: Long,
        content: String,
    ): AppResult<Long> =
        safeDatabaseCall {
            val messageCount = conversationDao.getMessageCount(conversationId)
            val now = System.currentTimeMillis()
            val messageId = conversationDao.insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = content,
                    status = MessageStatus.SENT,
                    errorMessage = null,
                    createdAtMillis = now,
                ),
            )

            if (messageCount == 0) {
                val updatedRows = conversationDao.updateConversationAutoTitleAndTimestamp(
                    conversationId = conversationId,
                    title = content.toConversationTitle(),
                    updatedAtMillis = now,
                )
                if (updatedRows == 0) {
                    conversationDao.updateConversationTimestamp(
                        conversationId = conversationId,
                        updatedAtMillis = now,
                    )
                }
            } else {
                conversationDao.updateConversationTimestamp(
                    conversationId = conversationId,
                    updatedAtMillis = now,
                )
            }

            messageId
        }

    override suspend fun addAssistantPlaceholder(conversationId: Long): AppResult<Long> =
        safeDatabaseCall {
            val now = System.currentTimeMillis()
            val messageId = conversationDao.insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = "",
                    status = MessageStatus.GENERATING,
                    errorMessage = null,
                    createdAtMillis = now,
                ),
            )
            conversationDao.updateConversationTimestamp(
                conversationId = conversationId,
                updatedAtMillis = now,
            )
            messageId
        }

    override suspend fun completeAssistantMessage(
        messageId: Long,
        content: String,
    ): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.completeAssistantMessage(
                messageId = messageId,
                content = content,
            )
            if (updatedRows > 0) {
                updateConversationTimestampForMessage(messageId)
            }
        }

    override suspend fun updateGeneratingAssistantContent(
        messageId: Long,
        content: String,
    ): AppResult<Boolean> =
        safeDatabaseCall {
            conversationDao.updateGeneratingAssistantContent(
                messageId = messageId,
                content = content,
            ) > 0
        }

    override suspend fun failAssistantMessage(
        messageId: Long,
        errorMessage: String,
    ): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.failAssistantMessage(
                messageId = messageId,
                errorMessage = errorMessage,
            )
            if (updatedRows > 0) {
                updateConversationTimestampForMessage(messageId)
            }
        }

    override suspend fun getGeneratingConversationIds(): AppResult<List<Long>> =
        safeDatabaseCall {
            conversationDao.getGeneratingConversationIds()
        }

    override suspend fun cancelGeneratingAssistantMessages(conversationId: Long): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.cancelGeneratingAssistantMessages(
                conversationId = conversationId,
                message = CANCELED_GENERATION_MESSAGE,
            )
            if (updatedRows > 0) {
                conversationDao.updateConversationTimestamp(
                    conversationId = conversationId,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            }
        }

    override suspend fun cancelGeneratingAssistantMessages(conversationIds: List<Long>): AppResult<Unit> =
        safeDatabaseCall {
            if (conversationIds.isEmpty()) {
                return@safeDatabaseCall
            }

            val updatedRows = conversationDao.cancelGeneratingAssistantMessages(
                conversationIds = conversationIds,
                message = CANCELED_GENERATION_MESSAGE,
            )
            if (updatedRows > 0) {
                conversationDao.updateConversationTimestamps(
                    conversationIds = conversationIds,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            }
        }

    override suspend fun retryAssistantMessage(messageId: Long): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.retryAssistantMessage(messageId)
            if (updatedRows == 0) {
                throw IllegalStateException("Message cannot be retried.")
            }
            updateConversationTimestampForMessage(messageId)
        }

    override suspend fun getLatestAssistantMessage(conversationId: Long): AppResult<Message?> =
        safeDatabaseCall {
            conversationDao.getLatestAssistantMessage(conversationId)?.toDomain()
        }

    override suspend fun regenerateAssistantMessage(messageId: Long): AppResult<Unit> =
        safeDatabaseCall {
            val updatedRows = conversationDao.regenerateAssistantMessage(messageId)
            if (updatedRows == 0) {
                throw IllegalStateException("Message cannot be regenerated.")
            }
            updateConversationTimestampForMessage(messageId)
        }

    override suspend fun editUserMessageAndDeleteNewer(
        conversationId: Long,
        messageId: Long,
        content: String,
    ): AppResult<Unit> =
        safeDatabaseCall {
            database.withTransaction {
                val message = conversationDao.getMessage(messageId)
                    ?: throw IllegalStateException("Message cannot be edited.")
                if (message.conversationId != conversationId || message.role != MessageRole.USER) {
                    throw IllegalStateException("Message cannot be edited.")
                }

                val updatedRows = conversationDao.updateUserMessage(
                    conversationId = conversationId,
                    messageId = messageId,
                    content = content,
                )
                if (updatedRows == 0) {
                    throw IllegalStateException("Message cannot be edited.")
                }

                conversationDao.deleteMessagesAfter(
                    conversationId = conversationId,
                    createdAtMillis = message.createdAtMillis,
                    messageId = message.id,
                )

                val now = System.currentTimeMillis()
                val isFirstMessage = conversationDao.getEarlierMessageCount(
                    conversationId = conversationId,
                    createdAtMillis = message.createdAtMillis,
                    messageId = message.id,
                ) == 0

                if (isFirstMessage) {
                    val updatedRows = conversationDao.updateConversationAutoTitleAndTimestamp(
                        conversationId = conversationId,
                        title = content.toConversationTitle(),
                        updatedAtMillis = now,
                    )
                    if (updatedRows == 0) {
                        conversationDao.updateConversationTimestamp(
                            conversationId = conversationId,
                            updatedAtMillis = now,
                        )
                    }
                } else {
                    conversationDao.updateConversationTimestamp(
                        conversationId = conversationId,
                        updatedAtMillis = now,
                    )
                }
            }
        }

    private suspend fun <T> safeDatabaseCall(block: suspend () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private suspend fun updateConversationTimestampForMessage(messageId: Long) {
        conversationDao.getConversationIdForMessage(messageId)?.let { conversationId ->
            conversationDao.updateConversationTimestamp(
                conversationId = conversationId,
                updatedAtMillis = System.currentTimeMillis(),
            )
        }
    }

    private fun String.toConversationTitle(): String =
        lineSequence()
            .firstOrNull()
            ?.trim()
            ?.take(MAX_TITLE_LENGTH)
            ?.ifBlank { null }
            ?: "New conversation"

    private fun databaseSizeBytes(): Long =
        listOf("", "-wal", "-shm")
            .sumOf { suffix -> context.getDatabasePath(DATABASE_NAME + suffix).length() }

    private companion object {
        const val DATABASE_NAME = "local_ai_client.db"
        const val MAX_TITLE_LENGTH = 48
        const val CANCELED_GENERATION_MESSAGE = "Generation stopped."
    }
}
