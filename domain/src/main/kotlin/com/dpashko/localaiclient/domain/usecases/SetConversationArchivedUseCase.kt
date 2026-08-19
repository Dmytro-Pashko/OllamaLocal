package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Moves a local conversation between the active list and archive.
 */
class SetConversationArchivedUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Persists the archive state for [conversationId].
     */
    suspend operator fun invoke(
        conversationId: Long,
        isArchived: Boolean,
    ): AppResult<Unit> =
        conversationRepository.setConversationArchived(
            conversationId = conversationId,
            isArchived = isArchived,
        )
}
