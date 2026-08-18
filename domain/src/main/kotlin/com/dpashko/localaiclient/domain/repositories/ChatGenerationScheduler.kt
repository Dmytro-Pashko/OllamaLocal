package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig

interface ChatGenerationScheduler {
    suspend fun enqueueGeneration(
        config: ConnectionConfig,
        conversationId: Long,
        assistantMessageId: Long,
        modelName: String,
        replaceExisting: Boolean = false,
    ): AppResult<Unit>

    suspend fun cancelGeneration(conversationId: Long): AppResult<Unit>

    suspend fun cancelGenerations(conversationIds: List<Long>): AppResult<Unit>
}
