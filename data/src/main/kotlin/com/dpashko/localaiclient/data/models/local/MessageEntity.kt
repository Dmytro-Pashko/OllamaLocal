package com.dpashko.localaiclient.data.models.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dpashko.localaiclient.domain.models.conversation.MessageRole
import com.dpashko.localaiclient.domain.models.conversation.MessageStatus

/**
 * Room row for a persisted chat message.
 */
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
        Index(value = ["branchId"]),
    ],
)
data class MessageEntity(
    /** Auto-generated local database id. */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** Parent conversation id. */
    val conversationId: Long,
    /** Parent branch id. */
    val branchId: Long = 0L,
    /** Sender role for UI rendering and provider request mapping. */
    val role: MessageRole,
    /** Message body stored on device. */
    val content: String,
    /** Current lifecycle state of this message. */
    val status: MessageStatus,
    /** Failure text for assistant messages that could not be generated. */
    val errorMessage: String?,
    /** Creation timestamp in epoch milliseconds. */
    val createdAtMillis: Long,
)
