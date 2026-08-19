package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Observes active assistant generation work across all conversations.
 */
class ObserveActiveGenerationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke() = conversationRepository.observeActiveGenerations()
}
