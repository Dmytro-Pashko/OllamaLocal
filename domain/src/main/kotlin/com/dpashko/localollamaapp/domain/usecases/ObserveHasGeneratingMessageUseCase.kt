package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHasGeneratingMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke(conversationId: Long): Flow<Boolean> =
        conversationRepository.observeHasGeneratingMessage(conversationId)
}
