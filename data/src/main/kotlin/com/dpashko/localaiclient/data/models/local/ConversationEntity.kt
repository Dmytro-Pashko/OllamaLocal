package com.dpashko.localaiclient.data.models.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a locally stored conversation.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    /** Auto-generated local database id. */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** Display title shown in the conversation list. */
    val title: String,
    /** True when the conversation should be shown above regular conversations. */
    val isPinned: Boolean = false,
    /** Model selected for this conversation. */
    val modelName: String,
    /** Creation timestamp in epoch milliseconds. */
    val createdAtMillis: Long,
    /** Last update timestamp in epoch milliseconds. */
    val updatedAtMillis: Long,
)
