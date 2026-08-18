package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.GenerationSettingsRepository
import javax.inject.Inject

/**
 * Observes persisted generation settings for settings screens and schedulers.
 */
class ObserveGenerationSettingsUseCase @Inject constructor(
    private val generationSettingsRepository: GenerationSettingsRepository,
) {
    /**
     * Emits the current generation settings and all future updates.
     */
    operator fun invoke() = generationSettingsRepository.observeGenerationSettings()
}
