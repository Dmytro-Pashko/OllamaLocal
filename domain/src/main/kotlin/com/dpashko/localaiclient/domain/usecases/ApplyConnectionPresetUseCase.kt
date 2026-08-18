package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset
import com.dpashko.localaiclient.domain.models.error.AppError
import javax.inject.Inject

/**
 * Validates a saved preset before presentation applies it to connection input.
 */
class ApplyConnectionPresetUseCase @Inject constructor() {
    /**
     * Returns [preset] when it still contains usable connection values.
     */
    operator fun invoke(preset: ConnectionPreset): AppResult<ConnectionPreset> =
        if (preset.host.isBlank() || preset.port !in 1..65535) {
            AppResult.Failure(AppError.InvalidConnectionConfig)
        } else {
            AppResult.Success(preset)
        }
}
