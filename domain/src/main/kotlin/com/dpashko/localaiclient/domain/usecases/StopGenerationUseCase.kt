package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Stops generation for one conversation.
 */
class StopGenerationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
    /**
     * Cancels scheduler work and marks generating assistant messages in [conversationId] as canceled.
     */
    suspend operator fun invoke(conversationId: Long): AppResult<Unit> {
        when (val cancelResult = chatGenerationScheduler.cancelGeneration(conversationId)) {
            is AppResult.Failure -> return cancelResult
            is AppResult.Success -> Unit
        }

        return conversationRepository.cancelGeneratingAssistantMessages(conversationId)
    }
}
