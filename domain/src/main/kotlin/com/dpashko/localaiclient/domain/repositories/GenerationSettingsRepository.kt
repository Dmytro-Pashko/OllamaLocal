package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for reading and writing local generation settings.
 */
interface GenerationSettingsRepository {
    /**
     * Observes persisted generation settings, emitting defaults when none were saved.
     */
    fun observeGenerationSettings(): Flow<GenerationSettings>

    /**
     * Persists [settings] for future scheduled generation work.
     */
    suspend fun saveGenerationSettings(settings: GenerationSettings): AppResult<Unit>

    /**
     * Restores persisted settings to [GenerationSettings.Default].
     */
    suspend fun resetGenerationSettings(): AppResult<Unit>
}
