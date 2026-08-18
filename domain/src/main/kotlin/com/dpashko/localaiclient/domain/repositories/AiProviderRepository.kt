package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.ai.AiModel

interface AiProviderRepository {
    suspend fun checkConnection(config: ConnectionConfig): AppResult<Unit>

    suspend fun getModels(config: ConnectionConfig): AppResult<List<AiModel>>

    suspend fun sendChatMessage(
        config: ConnectionConfig,
        modelName: String,
        messages: List<Message>,
        generationTimeoutMillis: Long,
    ): AppResult<String>
}
