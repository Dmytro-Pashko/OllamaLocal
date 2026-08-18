package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Rewrites a user message, removes newer context, and starts a replacement generation.
 */
class EditMessageAndRegenerateUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
    /**
     * Saves edited [content] for [messageId] and schedules a fresh assistant response.
     */
    suspend operator fun invoke(
        config: ConnectionConfig,
        conversationId: Long,
        modelName: String,
        messageId: Long,
        content: String,
    ): AppResult<Unit> {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            return AppResult.Failure(AppError.EmptyMessage)
        }

        when (val cancelResult = chatGenerationScheduler.cancelGeneration(conversationId)) {
            is AppResult.Failure -> return cancelResult
            is AppResult.Success -> Unit
        }

        when (
            val editResult = conversationRepository.editUserMessageAndDeleteNewer(
                conversationId = conversationId,
                messageId = messageId,
                content = trimmedContent,
            )
        ) {
            is AppResult.Failure -> return editResult
            is AppResult.Success -> Unit
        }

        val assistantMessageId = when (
            val placeholderResult = conversationRepository.addAssistantPlaceholder(conversationId)
        ) {
            is AppResult.Failure -> return placeholderResult
            is AppResult.Success -> placeholderResult.data
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
