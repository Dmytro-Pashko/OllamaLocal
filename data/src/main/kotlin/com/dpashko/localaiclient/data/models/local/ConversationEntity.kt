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
    /** True when the title was edited by the user and should not be auto-generated. */
    val isTitleManuallyEdited: Boolean = false,
    /** True when this conversation is hidden from the active list. */
    val isArchived: Boolean = false,
    /** Time when this conversation was archived, or null when active. */
    val archivedAtMillis: Long? = null,
    /** Model selected for this conversation. */
    val modelName: String,
    /** Creation timestamp in epoch milliseconds. */
    val createdAtMillis: Long,
    /** Last update timestamp in epoch milliseconds. */
    val updatedAtMillis: Long,
)
