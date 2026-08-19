package com.dpashko.localaiclient.domain.models.conversation

/**
 * Alternative message timeline inside one conversation.
 */
data class ConversationBranch(
    val id: Long,
    val conversationId: Long,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val isActive: Boolean,
)
