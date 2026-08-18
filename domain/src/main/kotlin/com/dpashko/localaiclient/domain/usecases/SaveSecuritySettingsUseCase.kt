package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.settings.SecuritySettings
import com.dpashko.localaiclient.domain.repositories.SecuritySettingsRepository
import javax.inject.Inject

/**
 * Persists user security settings.
 */
class SaveSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsRepository: SecuritySettingsRepository,
) {
    /**
     * Saves [settings] for app lock behavior.
     */
    suspend operator fun invoke(settings: SecuritySettings): AppResult<Unit> =
        securitySettingsRepository.saveSecuritySettings(settings)
}
