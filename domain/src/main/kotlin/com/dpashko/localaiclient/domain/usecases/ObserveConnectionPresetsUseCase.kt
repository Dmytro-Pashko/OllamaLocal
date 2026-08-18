package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ConnectionPresetRepository
import javax.inject.Inject

/**
 * Observes locally saved connection presets.
 */
class ObserveConnectionPresetsUseCase @Inject constructor(
    private val connectionPresetRepository: ConnectionPresetRepository,
) {
    /**
     * Emits all saved presets and future changes.
     */
    operator fun invoke() = connectionPresetRepository.observeConnectionPresets()
}
