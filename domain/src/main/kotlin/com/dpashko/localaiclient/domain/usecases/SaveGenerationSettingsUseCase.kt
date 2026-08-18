package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings
import com.dpashko.localaiclient.domain.repositories.GenerationSettingsRepository
import javax.inject.Inject

/**
 * Validates and persists user generation settings.
 */
class SaveGenerationSettingsUseCase @Inject constructor(
    private val generationSettingsRepository: GenerationSettingsRepository,
) {
    /**
     * Saves [settings] when the timeout is inside the supported range.
     */
    suspend operator fun invoke(settings: GenerationSettings): AppResult<Unit> {
        if (!GenerationSettings.isValidMinutes(settings.generationTimeoutMinutes)) {
            return AppResult.Failure(AppError.InvalidGenerationSettings)
        }

        return generationSettingsRepository.saveGenerationSettings(settings)
    }
}
