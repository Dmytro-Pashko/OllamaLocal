package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Observes generation settings for one conversation.
 */
class ObserveConversationSettingsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke(conversationId: Long) =
        conversationRepository.observeConversationSettings(conversationId)
}
