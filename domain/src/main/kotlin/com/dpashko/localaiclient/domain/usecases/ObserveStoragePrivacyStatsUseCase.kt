package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Observes local-only storage and privacy dashboard stats.
 */
class ObserveStoragePrivacyStatsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke() = conversationRepository.observeStoragePrivacyStats()
}
