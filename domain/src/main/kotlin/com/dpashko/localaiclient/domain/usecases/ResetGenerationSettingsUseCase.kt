package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.GenerationSettingsRepository
import javax.inject.Inject

/**
 * Restores generation settings to the app defaults.
 */
class ResetGenerationSettingsUseCase @Inject constructor(
    private val generationSettingsRepository: GenerationSettingsRepository,
) {
    /**
     * Persists default generation settings through the settings repository.
     */
    suspend operator fun invoke() = generationSettingsRepository.resetGenerationSettings()
}
