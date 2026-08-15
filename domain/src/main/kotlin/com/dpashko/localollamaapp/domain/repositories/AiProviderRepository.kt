package com.dpashko.localollamaapp.domain.repositories

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.ai.AiModel

interface AiProviderRepository {
    suspend fun checkConnection(config: ConnectionConfig): AppResult<Unit>

    suspend fun getModels(config: ConnectionConfig): AppResult<List<AiModel>>

    suspend fun sendChatMessage(
        config: ConnectionConfig,
        modelName: String,
        messages: List<Message>,
    ): AppResult<String>
}
