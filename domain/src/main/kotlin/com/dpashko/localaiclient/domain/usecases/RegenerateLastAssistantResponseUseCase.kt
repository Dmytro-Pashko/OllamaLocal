package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.conversation.MessageStatus
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Generates the latest assistant response again without requiring a failed state.
 */
class RegenerateLastAssistantResponseUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
    suspend operator fun invoke(
        config: ConnectionConfig,
        conversationId: Long,
    ): AppResult<Unit> {
        val latestAssistantMessage = when (
            val result = conversationRepository.getLatestAssistantMessage(conversationId)
        ) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data ?: return AppResult.Failure(
                AppError.Unknown("No assistant response to regenerate."),
            )
        }

        if (latestAssistantMessage.status == MessageStatus.GENERATING) {
            return AppResult.Failure(AppError.Unknown("Wait for the current response to finish."))
        }

        when (val updateResult = conversationRepository.regenerateAssistantMessage(latestAssistantMessage.id)) {
            is AppResult.Failure -> return updateResult
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
                assistantMessageId = latestAssistantMessage.id,
                modelName = settings.modelName,
                generationTimeoutMillis = settings.generationTimeoutMillis,
                systemPrompt = settings.systemPrompt,
                replaceExisting = true,
            )
        ) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> {
                conversationRepository.failAssistantMessage(
                    messageId = latestAssistantMessage.id,
                    errorMessage = scheduleResult.error.toString(),
                )
                scheduleResult
            }
        }
    }
}
