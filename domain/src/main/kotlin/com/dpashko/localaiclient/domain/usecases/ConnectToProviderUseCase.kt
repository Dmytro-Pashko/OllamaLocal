package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import javax.inject.Inject

/**
 * Validates local network connection details and checks provider availability.
 */
class ConnectToProviderUseCase @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
) {
    /**
     * Returns success only when [config] is structurally valid and the provider responds.
     */
    suspend operator fun invoke(config: ConnectionConfig): AppResult<Unit> =
        if (config.host.isBlank() || config.port !in 1..65535) {
            AppResult.Failure(AppError.InvalidConnectionConfig)
        } else {
            aiProviderRepository.checkConnection(config)
        }
}
