package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.repositories.ConnectionPresetRepository
import javax.inject.Inject

/**
 * Deletes a locally saved connection preset.
 */
class DeleteConnectionPresetUseCase @Inject constructor(
    private val connectionPresetRepository: ConnectionPresetRepository,
) {
    /**
     * Removes the preset identified by [presetId].
     */
    suspend operator fun invoke(presetId: String): AppResult<Unit> =
        connectionPresetRepository.deleteConnectionPreset(presetId)
}
