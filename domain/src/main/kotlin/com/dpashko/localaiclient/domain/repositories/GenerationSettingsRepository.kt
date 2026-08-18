package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings
import kotlinx.coroutines.flow.Flow

interface GenerationSettingsRepository {
    fun observeGenerationSettings(): Flow<GenerationSettings>

    suspend fun saveGenerationSettings(settings: GenerationSettings): AppResult<Unit>

    suspend fun resetGenerationSettings(): AppResult<Unit>
}
