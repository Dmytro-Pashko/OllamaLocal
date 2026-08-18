package com.dpashko.localollamaapp.domain.repositories

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig

interface ChatGenerationScheduler {
    suspend fun enqueueGeneration(
        config: ConnectionConfig,
        conversationId: Long,
        assistantMessageId: Long,
        modelName: String,
        replaceExisting: Boolean = false,
    ): AppResult<Unit>
}
