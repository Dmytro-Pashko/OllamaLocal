package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.SecuritySettingsRepository
import javax.inject.Inject

/**
 * Observes persisted security settings for app lock UI and gate checks.
 */
class ObserveSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsRepository: SecuritySettingsRepository,
) {
    /**
     * Emits the current security settings and all future updates.
     */
    operator fun invoke() = securitySettingsRepository.observeSecuritySettings()
}
