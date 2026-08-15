package com.dpashko.localollamaapp.domain.repositories

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig

interface ChatGenerationScheduler {
    suspend fun enqueueGeneration(
        config: OllamaConnectionConfig,
        conversationId: Long,
        assistantMessageId: Long,
        modelName: String,
    ): AppResult<Unit>
}
