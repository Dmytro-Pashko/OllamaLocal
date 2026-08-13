package com.dpashko.localollamaapp.domain.repositories

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.ollama.OllamaModel

interface OllamaRepository {
    suspend fun checkConnection(config: OllamaConnectionConfig): AppResult<Unit>

    suspend fun getModels(config: OllamaConnectionConfig): AppResult<List<OllamaModel>>

    suspend fun sendChatMessage(
        config: OllamaConnectionConfig,
        modelName: String,
        messages: List<Message>,
    ): AppResult<String>
}
