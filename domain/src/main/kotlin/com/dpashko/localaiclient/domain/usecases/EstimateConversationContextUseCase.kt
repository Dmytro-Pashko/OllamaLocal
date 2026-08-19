package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.conversation.ContextEstimate
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

class EstimateConversationContextUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(conversationId: Long): AppResult<ContextEstimate> {
        val messages = when (val result = conversationRepository.getContextMessages(conversationId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val settings = when (val result = conversationRepository.getConversationSettings(conversationId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        return AppResult.Success(
            ContextEstimate.from(
                messages = messages,
                systemPrompt = settings.systemPrompt,
            ),
        )
    }
}
