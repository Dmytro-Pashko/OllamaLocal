package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.LastConnection
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.LastConnectionRepository
import javax.inject.Inject

/**
 * Validates and saves the last successful provider connection.
 */
class SaveLastConnectionUseCase @Inject constructor(
    private val lastConnectionRepository: LastConnectionRepository,
) {
    /**
     * Saves [connection] when all connection fields are usable.
     */
    suspend operator fun invoke(connection: LastConnection): AppResult<Unit> =
        if (connection.host.isBlank() || connection.port !in 1..65535 || connection.modelName.isBlank()) {
            AppResult.Failure(AppError.InvalidConnectionConfig)
        } else {
            lastConnectionRepository.saveLastConnection(connection)
        }
}
