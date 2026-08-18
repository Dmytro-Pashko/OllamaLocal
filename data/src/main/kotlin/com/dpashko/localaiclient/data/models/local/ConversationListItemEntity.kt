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
    /** Model selected for this conversation. */
    val modelName: String,
    /** Creation timestamp in epoch milliseconds. */
    val createdAtMillis: Long,
    /** Last update timestamp in epoch milliseconds. */
    val updatedAtMillis: Long,
    /** True when a related assistant message is still generating. */
    val hasGeneratingMessage: Boolean,
)
