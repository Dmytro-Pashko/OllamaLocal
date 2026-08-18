package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.domain.repositories.ChatGenerationScheduler
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import javax.inject.Inject

class RetryGenerationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
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

        return when (
            val scheduleResult = chatGenerationScheduler.enqueueGeneration(
                config = config,
                conversationId = conversationId,
                assistantMessageId = assistantMessageId,
                modelName = modelName,
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
