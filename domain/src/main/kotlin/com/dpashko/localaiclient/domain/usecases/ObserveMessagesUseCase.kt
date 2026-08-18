package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes all messages for one local conversation.
 */
class ObserveMessagesUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    /**
     * Emits message updates for [conversationId] in repository-defined display order.
     */
    operator fun invoke(conversationId: Long): Flow<List<Message>> =
        conversationRepository.observeMessages(conversationId)
}
