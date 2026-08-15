package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.AiProviderRepository
import javax.inject.Inject

class ConnectToProviderUseCase @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
) {
    suspend operator fun invoke(config: ConnectionConfig): AppResult<Unit> =
        if (config.host.isBlank() || config.port !in 1..65535) {
            AppResult.Failure(AppError.InvalidConnectionConfig)
        } else {
            aiProviderRepository.checkConnection(config)
        }
}
