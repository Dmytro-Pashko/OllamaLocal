package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

class SwitchConversationBranchUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(
        conversationId: Long,
        branchId: Long,
    ) = conversationRepository.switchConversationBranch(
        conversationId = conversationId,
        branchId = branchId,
    )
}
