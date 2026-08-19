package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Observes conversations filtered by local title, model, or message text.
 */
class ObserveFilteredConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Emits all conversations for blank [query], otherwise matching conversations only.
     */
    operator fun invoke(
        query: String,
        isArchived: Boolean = false,
    ) = conversationRepository.observeConversations(
        query = query,
        isArchived = isArchived,
    )
}
