package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.models.conversation.MessageRole
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import com.dpashko.localollamaapp.domain.repositories.OllamaRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val ollamaRepository: OllamaRepository,
) {
    suspend operator fun invoke(
        config: OllamaConnectionConfig,
        conversationId: Long,
        modelName: String,
        content: String,
    ): AppResult<Unit> {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) {
            return AppResult.Failure(AppError.EmptyMessage)
        }

        val userMessageResult = conversationRepository.addMessage(
            conversationId = conversationId,
            role = MessageRole.USER,
            content = trimmedContent,
        )
        if (userMessageResult is AppResult.Failure) {
            return userMessageResult
        }

        val messages = when (val result = conversationRepository.getMessages(conversationId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }

        val assistantContent = when (
            val result = ollamaRepository.sendChatMessage(
                config = config,
                modelName = modelName,
                messages = messages,
            )
        ) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }

        return when (
            val result = conversationRepository.addMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = assistantContent,
            )
        ) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(Unit)
        }
    }
}
