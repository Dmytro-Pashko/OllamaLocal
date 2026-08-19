package com.dpashko.localaiclient.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dpashko.localaiclient.data.models.local.ConversationEntity
import com.dpashko.localaiclient.data.models.local.ConversationListItemEntity
import com.dpashko.localaiclient.data.models.local.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room access contract for conversations and messages stored on device.
 */
@Dao
interface ConversationDao {
    /**
     * Observes conversation list rows with a derived active-generation flag.
     */
    @Query(
        """
        SELECT
            conversations.id,
            conversations.title,
            conversations.isPinned,
            conversations.isArchived,
            conversations.archivedAtMillis,
            conversations.modelName,
            conversations.createdAtMillis,
            conversations.updatedAtMillis,
            EXISTS(
                SELECT 1 FROM messages
                WHERE messages.conversationId = conversations.id
                    AND messages.role = 'ASSISTANT'
                    AND messages.status = 'GENERATING'
            ) AS hasGeneratingMessage
        FROM conversations
        WHERE conversations.isArchived = 0
        ORDER BY conversations.isPinned DESC, conversations.updatedAtMillis DESC
        """,
    )
    fun observeConversations(): Flow<List<ConversationListItemEntity>>

    /**
     * Observes conversations matching title, model, or message text.
     */
    @Query(
        """
        SELECT
            conversations.id,
            conversations.title,
            conversations.isPinned,
            conversations.isArchived,
            conversations.archivedAtMillis,
            conversations.modelName,
            conversations.createdAtMillis,
            conversations.updatedAtMillis,
            EXISTS(
                SELECT 1 FROM messages
                WHERE messages.conversationId = conversations.id
                    AND messages.role = 'ASSISTANT'
                    AND messages.status = 'GENERATING'
            ) AS hasGeneratingMessage
        FROM conversations
        WHERE conversations.isArchived = :isArchived
            AND (
                :query = ''
                OR conversations.title LIKE '%' || :query || '%'
                OR conversations.modelName LIKE '%' || :query || '%'
                OR EXISTS(
                    SELECT 1 FROM messages
                    WHERE messages.conversationId = conversations.id
                        AND messages.content LIKE '%' || :query || '%'
                )
            )
        ORDER BY conversations.isPinned DESC, conversations.updatedAtMillis DESC
        """,
    )
    fun observeConversations(
        query: String,
        isArchived: Boolean,
    ): Flow<List<ConversationListItemEntity>>

    /**
     * Observes all messages for [conversationId] in chronological order.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    /**
     * Returns all messages for [conversationId] in chronological order.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC, id ASC")
    suspend fun getMessages(conversationId: Long): List<MessageEntity>

    /**
     * Returns one message by id, or null when it no longer exists.
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessage(messageId: Long): MessageEntity?

    /**
     * Returns sent, non-empty messages that are eligible for provider context.
     */
    @Query(
        """
        SELECT * FROM messages
        WHERE conversationId = :conversationId
            AND status = 'SENT'
            AND content != ''
        ORDER BY createdAtMillis ASC, id ASC
        """,
    )
    suspend fun getContextMessages(conversationId: Long): List<MessageEntity>

    /**
     * Counts messages currently stored for [conversationId].
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int

    /**
     * Observes whether [conversationId] has a generating assistant message.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM messages
            WHERE conversationId = :conversationId
                AND role = 'ASSISTANT'
                AND status = 'GENERATING'
        )
        """,
    )
    fun observeHasGeneratingMessage(conversationId: Long): Flow<Boolean>

    /**
     * Returns true when a message row exists for [messageId].
     */
    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId)")
    suspend fun messageExists(messageId: Long): Boolean

    /**
     * Returns the owning conversation id for [messageId], or null when absent.
     */
    @Query("SELECT conversationId FROM messages WHERE id = :messageId")
    suspend fun getConversationIdForMessage(messageId: Long): Long?

    /**
     * Returns conversation ids that currently contain generating assistant messages.
     */
    @Query(
        """
        SELECT DISTINCT conversationId FROM messages
        WHERE role = 'ASSISTANT'
            AND status = 'GENERATING'
        """,
    )
    suspend fun getGeneratingConversationIds(): List<Long>

    /**
     * Inserts a conversation and returns the generated id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    /**
     * Inserts a message and returns the generated id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Updates both display title and last activity timestamp for a conversation.
     */
    @Query("UPDATE conversations SET title = :title, updatedAtMillis = :updatedAtMillis WHERE id = :conversationId")
    suspend fun updateConversationTitleAndTimestamp(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
    )

    /**
     * Updates auto-generated title only while the title has not been manually edited.
     */
    @Query(
        """
        UPDATE conversations
        SET title = :title,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :conversationId
            AND isTitleManuallyEdited = 0
        """,
    )
    suspend fun updateConversationAutoTitleAndTimestamp(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
    ): Int

    /**
     * Updates a user-edited title and protects it from future auto-title updates.
     */
    @Query(
        """
        UPDATE conversations
        SET title = :title,
            isTitleManuallyEdited = 1,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :conversationId
        """,
    )
    suspend fun renameConversation(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
    ): Int

    /**
     * Updates only the last activity timestamp for a conversation.
     */
    @Query("UPDATE conversations SET updatedAtMillis = :updatedAtMillis WHERE id = :conversationId")
    suspend fun updateConversationTimestamp(
        conversationId: Long,
        updatedAtMillis: Long,
    )

    /**
     * Updates the local favorite pin flag for a conversation.
     */
    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updateConversationPinned(
        conversationId: Long,
        isPinned: Boolean,
    ): Int

    /**
     * Moves a conversation between the active and archived lists.
     */
    @Query(
        """
        UPDATE conversations
        SET isArchived = :isArchived,
            archivedAtMillis = :archivedAtMillis,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :conversationId
        """,
    )
    suspend fun updateConversationArchived(
        conversationId: Long,
        isArchived: Boolean,
        archivedAtMillis: Long?,
        updatedAtMillis: Long,
    ): Int

    /**
     * Completes a generating assistant placeholder and returns the affected row count.
     */
    @Query(
        """
        UPDATE messages
        SET content = :content,
            status = 'SENT',
            errorMessage = NULL
        WHERE id = :messageId
            AND role = 'ASSISTANT'
            AND status = 'GENERATING'
        """,
    )
    suspend fun completeAssistantMessage(
        messageId: Long,
        content: String,
    ): Int

    /**
     * Updates partial assistant content while generation is still running.
     */
    @Query(
        """
        UPDATE messages
        SET content = :content
        WHERE id = :messageId
            AND role = 'ASSISTANT'
            AND status = 'GENERATING'
        """,
    )
    suspend fun updateGeneratingAssistantContent(
        messageId: Long,
        content: String,
    ): Int

    /**
     * Marks a generating assistant placeholder as failed and returns the affected row count.
     */
    @Query(
        """
        UPDATE messages
        SET status = 'FAILED',
            errorMessage = :errorMessage
        WHERE id = :messageId
            AND role = 'ASSISTANT'
            AND status = 'GENERATING'
        """,
    )
    suspend fun failAssistantMessage(
        messageId: Long,
        errorMessage: String,
    ): Int

    /**
     * Cancels generating assistant messages for one conversation and returns affected rows.
     */
    @Query(
        """
        UPDATE messages
        SET status = 'CANCELED',
            errorMessage = :message
        WHERE conversationId = :conversationId
            AND role = 'ASSISTANT'
            AND status = 'GENERATING'
        """,
    )
    suspend fun cancelGeneratingAssistantMessages(
        conversationId: Long,
        message: String,
    ): Int

    /**
     * Cancels generating assistant messages for multiple conversations and returns affected rows.
     */
    @Query(
        """
        UPDATE messages
        SET status = 'CANCELED',
            errorMessage = :message
        WHERE conversationId IN (:conversationIds)
            AND role = 'ASSISTANT'
            AND status = 'GENERATING'
        """,
    )
    suspend fun cancelGeneratingAssistantMessages(
        conversationIds: List<Long>,
        message: String,
    ): Int

    /**
     * Updates last activity timestamps for conversations touched by bulk cancellation.
     */
    @Query("UPDATE conversations SET updatedAtMillis = :updatedAtMillis WHERE id IN (:conversationIds)")
    suspend fun updateConversationTimestamps(
        conversationIds: List<Long>,
        updatedAtMillis: Long,
    )

    /**
     * Resets a failed assistant message for retry and returns the affected row count.
     */
    @Query(
        """
        UPDATE messages
        SET content = '',
            status = 'GENERATING',
            errorMessage = NULL
        WHERE id = :messageId
            AND role = 'ASSISTANT'
            AND status = 'FAILED'
        """,
    )
    suspend fun retryAssistantMessage(messageId: Long): Int

    /**
     * Updates a user message and returns the affected row count.
     */
    @Query(
        """
        UPDATE messages
        SET content = :content,
            status = 'SENT',
            errorMessage = NULL
        WHERE id = :messageId
            AND conversationId = :conversationId
            AND role = 'USER'
        """,
    )
    suspend fun updateUserMessage(
        conversationId: Long,
        messageId: Long,
        content: String,
    ): Int

    /**
     * Deletes messages newer than the edited message in the same conversation.
     */
    @Query(
        """
        DELETE FROM messages
        WHERE conversationId = :conversationId
            AND (
                createdAtMillis > :createdAtMillis
                OR (createdAtMillis = :createdAtMillis AND id > :messageId)
            )
        """,
    )
    suspend fun deleteMessagesAfter(
        conversationId: Long,
        createdAtMillis: Long,
        messageId: Long,
    )

    /**
     * Counts messages earlier than the supplied message position in one conversation.
     */
    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE conversationId = :conversationId
            AND (
                createdAtMillis < :createdAtMillis
                OR (createdAtMillis = :createdAtMillis AND id < :messageId)
            )
        """,
    )
    suspend fun getEarlierMessageCount(
        conversationId: Long,
        createdAtMillis: Long,
        messageId: Long,
    ): Int

    /**
     * Deletes a conversation; message rows are removed by the Room foreign-key cascade.
     */
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)

    /**
     * Deletes all message rows for permanent session cleanup.
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    /**
     * Deletes all conversation rows for permanent session cleanup.
     */
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
