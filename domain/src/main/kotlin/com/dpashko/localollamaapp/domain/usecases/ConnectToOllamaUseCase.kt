package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.OllamaRepository
import javax.inject.Inject

class ConnectToOllamaUseCase @Inject constructor(
    private val ollamaRepository: OllamaRepository,
) {
    suspend operator fun invoke(config: OllamaConnectionConfig): AppResult<Unit> =
        if (config.host.isBlank() || config.port !in 1..65535) {
            AppResult.Failure(AppError.InvalidConnectionConfig)
        } else {
            ollamaRepository.checkConnection(config)
        }
}
