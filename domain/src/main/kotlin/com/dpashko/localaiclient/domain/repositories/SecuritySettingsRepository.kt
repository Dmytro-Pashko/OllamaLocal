package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.settings.SecuritySettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for reading and writing local security settings.
 */
interface SecuritySettingsRepository {
    /**
     * Observes persisted security settings, emitting defaults when none were saved.
     */
    fun observeSecuritySettings(): Flow<SecuritySettings>

    /**
     * Persists [settings] for future app launches and lock checks.
     */
    suspend fun saveSecuritySettings(settings: SecuritySettings): AppResult<Unit>
}
