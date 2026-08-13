package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(modelName: String): AppResult<Long> =
        if (modelName.isBlank()) {
            AppResult.Failure(AppError.EmptyModels)
        } else {
            conversationRepository.createConversation(modelName)
        }
}
