package com.dpashko.localaiclient.data.models.local

/**
 * Projection for editable per-conversation generation settings.
 */
data class ConversationSettingsEntity(
    /** Owning conversation id. */
    val id: Long,
    /** Model used when generating in this conversation. */
    val modelName: String,
    /** Maximum generation time for this conversation. */
    val generationTimeoutMillis: Long,
    /** Optional system instruction prepended to provider context. */
    val systemPrompt: String,
)
