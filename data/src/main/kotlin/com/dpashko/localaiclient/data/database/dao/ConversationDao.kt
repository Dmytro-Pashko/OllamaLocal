package com.dpashko.localaiclient.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dpashko.localaiclient.data.models.local.ActiveGenerationEntity
import com.dpashko.localaiclient.data.models.local.ConversationBranchEntity
import com.dpashko.localaiclient.data.models.local.ConversationEntity
import com.dpashko.localaiclient.data.models.local.ConversationListItemEntity
import com.dpashko.localaiclient.data.models.local.ConversationSettingsEntity
import com.dpashko.localaiclient.data.models.local.MessageEntity
import com.dpashko.localaiclient.data.models.local.StoragePrivacyStatsEntity
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
                    AND messages.branchId = conversations.activeBranchId
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
                    AND messages.branchId = conversations.activeBranchId
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
                        AND messages.branchId = conversations.activeBranchId
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
    @Query(
        """
        SELECT messages.* FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        WHERE messages.conversationId = :conversationId
            AND messages.branchId = conversations.activeBranchId
        ORDER BY messages.createdAtMillis ASC, messages.id ASC
        """,
    )
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    /**
     * Returns all messages for [conversationId] in chronological order.
     */
    @Query(
        """
        SELECT messages.* FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        WHERE messages.conversationId = :conversationId
            AND messages.branchId = conversations.activeBranchId
        ORDER BY messages.createdAtMillis ASC, messages.id ASC
        """,
    )
    suspend fun getMessages(conversationId: Long): List<MessageEntity>

    /**
     * Returns one message by id, or null when it no longer exists.
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessage(messageId: Long): MessageEntity?

    /**
     * Returns the newest assistant message in one conversation.
     */
    @Query(
        """
        SELECT messages.* FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        WHERE messages.conversationId = :conversationId
            AND messages.branchId = conversations.activeBranchId
            AND role = 'ASSISTANT'
        ORDER BY messages.createdAtMillis DESC, messages.id DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestAssistantMessage(conversationId: Long): MessageEntity?

    /**
     * Observes all branches for one conversation.
     */
    @Query(
        """
        SELECT * FROM conversation_branches
        WHERE conversationId = :conversationId
        ORDER BY createdAtMillis ASC, id ASC
        """,
    )
    fun observeConversationBranches(conversationId: Long): Flow<List<ConversationBranchEntity>>

    /**
     * Returns the currently active branch id for a conversation.
     */
    @Query("SELECT activeBranchId FROM conversations WHERE id = :conversationId")
    suspend fun getActiveBranchId(conversationId: Long): Long?

    /**
     * Observes editable generation settings for one conversation.
     */
    @Query(
        """
        SELECT id, modelName, generationTimeoutMillis, systemPrompt
        FROM conversations
        WHERE id = :conversationId
        """,
    )
    fun observeConversationSettings(conversationId: Long): Flow<ConversationSettingsEntity?>

    /**
     * Returns generation settings for one conversation.
     */
    @Query(
        """
        SELECT id, modelName, generationTimeoutMillis, systemPrompt
        FROM conversations
        WHERE id = :conversationId
        """,
    )
    suspend fun getConversationSettings(conversationId: Long): ConversationSettingsEntity?

    /**
     * Returns sent, non-empty messages that are eligible for provider context.
     */
    @Query(
        """
        SELECT messages.* FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        WHERE messages.conversationId = :conversationId
            AND messages.branchId = conversations.activeBranchId
            AND status = 'SENT'
            AND content != ''
        ORDER BY messages.createdAtMillis ASC, messages.id ASC
        """,
    )
    suspend fun getContextMessages(conversationId: Long): List<MessageEntity>

    /**
     * Counts messages currently stored for [conversationId].
     */
    @Query(
        """
        SELECT COUNT(*) FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        WHERE messages.conversationId = :conversationId
            AND messages.branchId = conversations.activeBranchId
        """,
    )
    suspend fun getMessageCount(conversationId: Long): Int

    /**
     * Observes whether [conversationId] has a generating assistant message.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM messages
            INNER JOIN conversations ON conversations.id = messages.conversationId
            WHERE messages.conversationId = :conversationId
                AND messages.branchId = conversations.activeBranchId
                AND role = 'ASSISTANT'
                AND status = 'GENERATING'
        )
        """,
    )
    fun observeHasGeneratingMessage(conversationId: Long): Flow<Boolean>

    /**
     * Observes conversations that currently have a generating assistant message.
     */
    @Query(
        """
        SELECT
            conversations.id AS conversationId,
            conversations.title,
            conversations.modelName,
            conversations.isArchived,
            MIN(messages.createdAtMillis) AS assistantMessageCreatedAtMillis
        FROM conversations
        INNER JOIN messages ON messages.conversationId = conversations.id
        WHERE messages.role = 'ASSISTANT'
            AND messages.status = 'GENERATING'
            AND messages.branchId = conversations.activeBranchId
        GROUP BY conversations.id
        ORDER BY assistantMessageCreatedAtMillis ASC
        """,
    )
    fun observeActiveGenerations(): Flow<List<ActiveGenerationEntity>>

    /**
     * Observes aggregate local storage counters for the dashboard.
     */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM conversations WHERE isArchived = 0) AS activeConversationCount,
            (SELECT COUNT(*) FROM conversations WHERE isArchived = 1) AS archivedConversationCount,
            (SELECT COUNT(*) FROM messages) AS messageCount,
            (
                SELECT COUNT(*) FROM messages
                WHERE role = 'ASSISTANT'
                    AND status = 'GENERATING'
            ) AS activeGenerationCount
        """,
    )
    fun observeStoragePrivacyStats(): Flow<StoragePrivacyStatsEntity>

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
     * Inserts a branch and returns the generated id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversationBranch(branch: ConversationBranchEntity): Long

    /**
     * Inserts a message and returns the generated id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Inserts copied branch messages.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Updates active branch for one conversation.
     */
    @Query("UPDATE conversations SET activeBranchId = :branchId WHERE id = :conversationId")
    suspend fun updateActiveBranch(
        conversationId: Long,
        branchId: Long,
    ): Int

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
     * Updates model, timeout, and optional system prompt for one conversation.
     */
    @Query(
        """
        UPDATE conversations
        SET modelName = :modelName,
            generationTimeoutMillis = :generationTimeoutMillis,
            systemPrompt = :systemPrompt,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :conversationId
        """,
    )
    suspend fun updateConversationSettings(
        conversationId: Long,
        modelName: String,
        generationTimeoutMillis: Long,
        systemPrompt: String,
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
            AND branchId = (SELECT activeBranchId FROM conversations WHERE id = :conversationId)
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
            AND branchId IN (SELECT activeBranchId FROM conversations WHERE id IN (:conversationIds))
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
     * Resets a completed, failed, or canceled assistant message for a full regeneration.
     */
    @Query(
        """
        UPDATE messages
        SET content = '',
            status = 'GENERATING',
            errorMessage = NULL
        WHERE id = :messageId
            AND role = 'ASSISTANT'
            AND status != 'GENERATING'
        """,
    )
    suspend fun regenerateAssistantMessage(messageId: Long): Int

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
            AND branchId = (SELECT activeBranchId FROM conversations WHERE id = :conversationId)
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
            AND branchId = (SELECT activeBranchId FROM conversations WHERE id = :conversationId)
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
            AND branchId = (SELECT activeBranchId FROM conversations WHERE id = :conversationId)
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
     * Returns messages in the active branch up to and including a message.
     */
    @Query(
        """
        SELECT messages.* FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        WHERE messages.conversationId = :conversationId
            AND messages.branchId = conversations.activeBranchId
            AND (
                messages.createdAtMillis < :createdAtMillis
                OR (messages.createdAtMillis = :createdAtMillis AND messages.id <= :messageId)
            )
        ORDER BY messages.createdAtMillis ASC, messages.id ASC
        """,
    )
    suspend fun getActiveBranchMessagesThrough(
        conversationId: Long,
        createdAtMillis: Long,
        messageId: Long,
    ): List<MessageEntity>

    /**
     * Counts branches owned by one conversation.
     */
    @Query("SELECT COUNT(*) FROM conversation_branches WHERE conversationId = :conversationId")
    suspend fun getBranchCount(conversationId: Long): Int

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
