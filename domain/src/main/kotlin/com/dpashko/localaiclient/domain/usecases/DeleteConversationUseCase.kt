package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Deletes a conversation after canceling any active generation tied to it.
 */
class DeleteConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val chatGenerationScheduler: ChatGenerationScheduler,
) {
    /**
     * Cancels background work for [conversationId] and then removes its stored messages.
     */
    suspend operator fun invoke(conversationId: Long): AppResult<Unit> {
        when (val cancelResult = chatGenerationScheduler.cancelGeneration(conversationId)) {
            is AppResult.Failure -> return cancelResult
            is AppResult.Success -> Unit
        }

        return conversationRepository.deleteConversation(conversationId)
    }
}
