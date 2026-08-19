package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Retries a failed or canceled assistant generation for an existing placeholder.
 */
class RetryGenerationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
    /**
     * Marks [assistantMessageId] as generating and enqueues replacement background work.
     */
    suspend operator fun invoke(
        config: ConnectionConfig,
        conversationId: Long,
        modelName: String,
        assistantMessageId: Long,
    ): AppResult<Unit> {
        when (val retryResult = conversationRepository.retryAssistantMessage(assistantMessageId)) {
            is AppResult.Failure -> return retryResult
            is AppResult.Success -> Unit
        }

        val settings = when (val settingsResult = conversationRepository.getConversationSettings(conversationId)) {
            is AppResult.Failure -> return settingsResult
            is AppResult.Success -> settingsResult.data
        }

        return when (
            val scheduleResult = chatGenerationScheduler.enqueueGeneration(
                config = config,
                conversationId = conversationId,
                assistantMessageId = assistantMessageId,
                modelName = settings.modelName,
                generationTimeoutMillis = settings.generationTimeoutMillis,
                systemPrompt = settings.systemPrompt,
                replaceExisting = true,
            )
        ) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> {
                conversationRepository.failAssistantMessage(
                    messageId = assistantMessageId,
                    errorMessage = scheduleResult.error.toString(),
                )
                scheduleResult
            }
        }
    }
}
