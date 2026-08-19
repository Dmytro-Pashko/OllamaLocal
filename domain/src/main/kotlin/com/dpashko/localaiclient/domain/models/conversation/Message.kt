package com.dpashko.localaiclient.domain.models.conversation

/**
 * Single chat message persisted on device.
 */
data class Message(
    /** Stable local database identifier. */
    val id: Long,
    /** Owning conversation identifier. */
    val conversationId: Long,
    /** Owning branch identifier inside the conversation. */
    val branchId: Long,
    /** Sender role used for rendering and provider request mapping. */
    val role: MessageRole,
    /** Message body stored locally and sent as context when eligible. */
    val content: String,
    /** Lifecycle state for sent, generating, failed, or canceled messages. */
    val status: MessageStatus,
    /** Provider or scheduling error text for failed assistant messages. */
    val errorMessage: String?,
    /** Creation timestamp in epoch milliseconds. */
    val createdAtMillis: Long,
)
