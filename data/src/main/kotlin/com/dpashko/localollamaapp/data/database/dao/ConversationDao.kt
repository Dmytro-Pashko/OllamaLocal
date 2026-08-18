package com.dpashko.localollamaapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dpashko.localollamaapp.data.models.local.ConversationEntity
import com.dpashko.localollamaapp.data.models.local.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAtMillis DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC, id ASC")
    suspend fun getMessages(conversationId: Long): List<MessageEntity>

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

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int

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

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId)")
    suspend fun messageExists(messageId: Long): Boolean

    @Query("SELECT conversationId FROM messages WHERE id = :messageId")
    suspend fun getConversationIdForMessage(messageId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE conversations SET title = :title, updatedAtMillis = :updatedAtMillis WHERE id = :conversationId")
    suspend fun updateConversationTitleAndTimestamp(
        conversationId: Long,
        title: String,
        updatedAtMillis: Long,
    )

    @Query("UPDATE conversations SET updatedAtMillis = :updatedAtMillis WHERE id = :conversationId")
    suspend fun updateConversationTimestamp(
        conversationId: Long,
        updatedAtMillis: Long,
    )

    @Query(
        """
        UPDATE messages
        SET content = :content,
            status = 'SENT',
            errorMessage = NULL
        WHERE id = :messageId
        """,
    )
    suspend fun completeAssistantMessage(
        messageId: Long,
        content: String,
    )

    @Query(
        """
        UPDATE messages
        SET status = 'FAILED',
            errorMessage = :errorMessage
        WHERE id = :messageId
        """,
    )
    suspend fun failAssistantMessage(
        messageId: Long,
        errorMessage: String,
    )

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

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)
}
