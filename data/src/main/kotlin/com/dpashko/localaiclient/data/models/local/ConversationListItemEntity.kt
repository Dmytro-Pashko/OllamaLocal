package com.dpashko.localaiclient.data.models.local

/**
 * Projection used for efficient conversation list queries.
 */
data class ConversationListItemEntity(
    /** Local conversation id. */
    val id: Long,
    /** Display title shown in the conversation list. */
    val title: String,
    /** True when the conversation should be shown above regular conversations. */
    val isPinned: Boolean,
    /** True when this conversation is hidden from the active list. */
    val isArchived: Boolean,
    /** Time when this conversation was archived, or null when active. */
    val archivedAtMillis: Long?,
    /** Model selected for this conversation. */
    val modelName: String,
    /** Creation timestamp in epoch milliseconds. */
    val createdAtMillis: Long,
    /** Last update timestamp in epoch milliseconds. */
    val updatedAtMillis: Long,
    /** True when a related assistant message is still generating. */
    val hasGeneratingMessage: Boolean,
)
