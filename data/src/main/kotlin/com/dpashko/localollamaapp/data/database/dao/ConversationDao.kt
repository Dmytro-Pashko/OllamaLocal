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

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: Long): Int

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

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)
}
