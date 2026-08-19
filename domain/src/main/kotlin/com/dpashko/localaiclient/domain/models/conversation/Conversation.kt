package com.dpashko.localaiclient.domain.models.conversation

/**
 * Conversation metadata shown in the conversation list.
 */
data class Conversation(
    /** Stable local database identifier. */
    val id: Long,
    /** User-facing title, currently derived from the conversation content or model. */
    val title: String,
    /** Whether this conversation should be shown above regular conversations. */
    val isPinned: Boolean,
    /** Whether this conversation is hidden from the active conversation list. */
    val isArchived: Boolean,
    /** Time when this conversation was archived, or null when active. */
    val archivedAtMillis: Long?,
    /** Model selected when the conversation was created. */
    val modelName: String,
    /** Creation timestamp in epoch milliseconds. */
    val createdAtMillis: Long,
    /** Last activity timestamp in epoch milliseconds. */
    val updatedAtMillis: Long,
    /** Whether this conversation currently has an assistant response in progress. */
    val hasGeneratingMessage: Boolean,
)
