package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.ChatGenerationScheduler
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
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

        return when (
            val scheduleResult = chatGenerationScheduler.enqueueGeneration(
                config = config,
                conversationId = conversationId,
                assistantMessageId = assistantMessageId,
                modelName = modelName,
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
