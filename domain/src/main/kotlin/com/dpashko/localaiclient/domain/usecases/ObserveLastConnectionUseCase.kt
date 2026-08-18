package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.LastConnectionRepository
import javax.inject.Inject

/**
 * Observes the last successful local provider connection.
 */
class ObserveLastConnectionUseCase @Inject constructor(
    private val lastConnectionRepository: LastConnectionRepository,
) {
    /**
     * Emits the saved connection or null when the app has not connected successfully yet.
     */
    operator fun invoke() = lastConnectionRepository.observeLastConnection()
}
