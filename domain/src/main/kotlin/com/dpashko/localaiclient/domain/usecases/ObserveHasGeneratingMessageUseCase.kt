package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes whether a conversation has an active assistant generation.
 */
class ObserveHasGeneratingMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Emits true while [conversationId] contains a generating assistant placeholder.
     */
    operator fun invoke(conversationId: Long): Flow<Boolean> =
        conversationRepository.observeHasGeneratingMessage(conversationId)
}
