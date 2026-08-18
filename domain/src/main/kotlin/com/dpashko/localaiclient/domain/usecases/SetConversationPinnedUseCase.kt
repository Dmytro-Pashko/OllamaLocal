package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Marks a conversation as pinned or regular in the local list.
 */
class SetConversationPinnedUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Updates the pin flag for [conversationId].
     */
    suspend operator fun invoke(
        conversationId: Long,
        isPinned: Boolean,
    ): AppResult<Unit> =
        conversationRepository.setConversationPinned(
            conversationId = conversationId,
            isPinned = isPinned,
        )
}
