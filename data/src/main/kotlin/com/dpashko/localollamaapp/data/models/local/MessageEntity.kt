package com.dpashko.localollamaapp.data.models.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dpashko.localollamaapp.domain.models.conversation.MessageRole
import com.dpashko.localollamaapp.domain.models.conversation.MessageStatus

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conversationId"]),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val status: MessageStatus,
    val errorMessage: String?,
    val createdAtMillis: Long,
)
