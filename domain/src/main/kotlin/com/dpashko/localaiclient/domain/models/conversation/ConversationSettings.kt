package com.dpashko.localaiclient.domain.models.conversation

/**
 * Generation settings stored for one local conversation.
 */
data class ConversationSettings(
    /** Owning conversation id. */
    val conversationId: Long,
    /** Model used when generating in this conversation. */
    val modelName: String,
    /** Maximum generation time for this conversation. */
    val generationTimeoutMillis: Long,
    /** Optional system instruction prepended to provider context. */
    val systemPrompt: String,
) {
    /** Timeout value converted for minute-based UI controls. */
    val generationTimeoutMinutes: Int
        get() = (generationTimeoutMillis / MILLIS_PER_MINUTE).toInt()

    companion object {
        const val MAX_SYSTEM_PROMPT_LENGTH = 4_000
        const val MILLIS_PER_MINUTE = 60_000L

        fun fromMinutes(
            conversationId: Long,
            modelName: String,
            minutes: Int,
            systemPrompt: String,
        ): ConversationSettings =
            ConversationSettings(
                conversationId = conversationId,
                modelName = modelName,
                generationTimeoutMillis = minutes * MILLIS_PER_MINUTE,
                systemPrompt = systemPrompt,
            )
    }
}
