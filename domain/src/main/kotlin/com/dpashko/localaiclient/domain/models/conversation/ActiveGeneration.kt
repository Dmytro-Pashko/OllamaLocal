package com.dpashko.localaiclient.domain.models.conversation

/**
 * Compact metadata for one active assistant generation.
 */
data class ActiveGeneration(
    val conversationId: Long,
    val title: String,
    val modelName: String,
    val isArchived: Boolean,
    val assistantMessageCreatedAtMillis: Long,
)
