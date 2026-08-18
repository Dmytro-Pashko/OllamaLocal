package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.conversation.Conversation
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes conversation list metadata from local storage.
 */
class ObserveConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Emits updates whenever conversations or their generation state changes.
     */
    operator fun invoke(): Flow<List<Conversation>> =
        conversationRepository.observeConversations()
}
