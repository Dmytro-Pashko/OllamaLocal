package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

class ObserveConversationBranchesUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke(conversationId: Long) =
        conversationRepository.observeConversationBranches(conversationId)
}
