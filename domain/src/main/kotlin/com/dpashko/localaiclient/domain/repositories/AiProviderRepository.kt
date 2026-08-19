package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.ai.AiModel

/**
 * Provider-neutral contract for communicating with locally running AI servers.
 */
interface AiProviderRepository {
    /**
     * Verifies that the provider described by [config] is reachable and speaks the expected API.
     */
    suspend fun checkConnection(config: ConnectionConfig): AppResult<Unit>

    /**
     * Loads models that can be selected for chat generation from the configured provider.
     */
    suspend fun getModels(config: ConnectionConfig): AppResult<List<AiModel>>

    /**
     * Sends the eligible conversation [messages] to [modelName] and returns the generated text.
     */
    suspend fun sendChatMessage(
        config: ConnectionConfig,
        modelName: String,
        messages: List<Message>,
        generationTimeoutMillis: Long,
    ): AppResult<String>

    /**
     * Sends [messages] to [modelName], reports generated text chunks through [onDelta],
     * and returns the complete generated text after the provider stream finishes.
     */
    suspend fun streamChatMessage(
        config: ConnectionConfig,
        modelName: String,
        messages: List<Message>,
        generationTimeoutMillis: Long,
        onDelta: suspend (String) -> Unit,
    ): AppResult<String>
}
