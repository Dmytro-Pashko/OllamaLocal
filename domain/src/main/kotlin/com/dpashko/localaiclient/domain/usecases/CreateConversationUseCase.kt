package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Creates a new local conversation for the selected model.
 */
class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Persists a conversation and returns its id when [modelName] is not blank.
     */
    suspend operator fun invoke(modelName: String): AppResult<Long> =
        if (modelName.isBlank()) {
            AppResult.Failure(AppError.EmptyModels)
        } else {
            conversationRepository.createConversation(modelName)
        }
}
