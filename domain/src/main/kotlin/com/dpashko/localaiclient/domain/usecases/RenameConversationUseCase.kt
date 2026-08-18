package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Validates and saves a user-edited conversation title.
 */
class RenameConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Stores [title] when it is non-blank and short enough for list display.
     */
    suspend operator fun invoke(
        conversationId: Long,
        title: String,
    ): AppResult<Unit> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank() || trimmedTitle.length > MAX_TITLE_LENGTH) {
            return AppResult.Failure(AppError.InvalidConversationTitle)
        }

        return conversationRepository.renameConversation(
            conversationId = conversationId,
            title = trimmedTitle,
        )
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 48
    }
}
