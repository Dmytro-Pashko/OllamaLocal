package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig

/**
 * Domain boundary for scheduling and canceling background assistant generation.
 */
interface ChatGenerationScheduler {
    /**
     * Enqueues generation for [assistantMessageId] in [conversationId] using the selected model.
     */
    suspend fun enqueueGeneration(
        config: ConnectionConfig,
        conversationId: Long,
        assistantMessageId: Long,
        modelName: String,
        generationTimeoutMillis: Long,
        systemPrompt: String,
        replaceExisting: Boolean = false,
    ): AppResult<Unit>

    /**
     * Cancels any scheduled or running generation work for one conversation.
     */
    suspend fun cancelGeneration(conversationId: Long): AppResult<Unit>

    /**
     * Cancels scheduled or running generation work for multiple conversations.
     */
    suspend fun cancelGenerations(conversationIds: List<Long>): AppResult<Unit>
}
