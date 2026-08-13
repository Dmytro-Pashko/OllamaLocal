package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.conversation.Conversation
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke(): Flow<List<Conversation>> =
        conversationRepository.observeConversations()
}
