package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Permanently removes all conversation session data after stopping active generation.
 */
class DeleteAllSessionDataUseCase @Inject constructor(
    private val stopAllGenerationsUseCase: StopAllGenerationsUseCase,
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Stops all active generations, then deletes conversations and messages from local storage.
     */
    suspend operator fun invoke(): AppResult<Unit> {
        when (val stopResult = stopAllGenerationsUseCase()) {
            is AppResult.Failure -> return stopResult
            is AppResult.Success -> Unit
        }

        return conversationRepository.deleteAllSessionData()
    }
}
