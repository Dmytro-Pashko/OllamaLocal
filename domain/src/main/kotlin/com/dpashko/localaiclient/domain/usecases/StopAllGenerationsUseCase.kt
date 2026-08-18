package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Stops all currently running assistant generations.
 */
class StopAllGenerationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
    /**
     * Cancels scheduler work and marks all generating assistant messages as canceled.
     */
    suspend operator fun invoke(): AppResult<Unit> {
        val conversationIds = when (val result = conversationRepository.getGeneratingConversationIds()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }

        if (conversationIds.isEmpty()) {
            return AppResult.Success(Unit)
        }

        when (val cancelResult = chatGenerationScheduler.cancelGenerations(conversationIds)) {
            is AppResult.Failure -> return cancelResult
            is AppResult.Success -> Unit
        }

        return conversationRepository.cancelGeneratingAssistantMessages(conversationIds)
    }
}
