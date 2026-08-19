package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Persists a user message and schedules a local model response.
 */
class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
    /**
     * Adds trimmed [content], creates an assistant placeholder, and enqueues generation.
     */
    suspend operator fun invoke(
        config: ConnectionConfig,
        conversationId: Long,
        modelName: String,
        content: String,
    ): AppResult<Unit> {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            return AppResult.Failure(AppError.EmptyMessage)
        }

        when (
            val userMessageResult = conversationRepository.addUserMessage(
                conversationId = conversationId,
                content = trimmedContent,
            )
        ) {
            is AppResult.Failure -> return userMessageResult
            is AppResult.Success -> Unit
        }

        val assistantMessageId = when (
            val placeholderResult = conversationRepository.addAssistantPlaceholder(conversationId)
        ) {
            is AppResult.Failure -> return placeholderResult
            is AppResult.Success -> placeholderResult.data
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
