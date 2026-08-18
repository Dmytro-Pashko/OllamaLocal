package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ConnectionPresetRepository
import javax.inject.Inject

/**
 * Validates and saves a local connection preset.
 */
class SaveConnectionPresetUseCase @Inject constructor(
    private val connectionPresetRepository: ConnectionPresetRepository,
) {
    /**
     * Saves [preset] when its connection fields are valid.
     */
    suspend operator fun invoke(preset: ConnectionPreset): AppResult<Unit> =
        if (preset.name.isBlank() || preset.host.isBlank() || preset.port !in 1..65535) {
            AppResult.Failure(AppError.InvalidConnectionConfig)
        } else {
            connectionPresetRepository.saveConnectionPreset(preset)
        }
}
