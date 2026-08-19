package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics
import com.dpashko.localaiclient.domain.models.connection.ProviderHealth
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import com.dpashko.localaiclient.domain.repositories.ProviderDiagnosticsRepository
import javax.inject.Inject

/**
 * Checks the current provider and stores a compact diagnostic snapshot.
 */
class RefreshProviderDiagnosticsUseCase @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
    private val providerDiagnosticsRepository: ProviderDiagnosticsRepository,
) {
    suspend operator fun invoke(config: ConnectionConfig): AppResult<ProviderDiagnostics> {
        val startedAtMillis = System.currentTimeMillis()
        val connectionResult = aiProviderRepository.checkConnection(config)
        val latencyMillis = System.currentTimeMillis() - startedAtMillis
        val diagnostics = when (connectionResult) {
            is AppResult.Failure -> ProviderDiagnostics(
                provider = config.provider,
                host = config.host,
                port = config.port,
                health = connectionResult.error.toProviderHealth(),
                lastCheckedAtMillis = System.currentTimeMillis(),
                latencyMillis = null,
                modelCount = null,
                lastError = connectionResult.error.toDiagnosticMessage(),
            )

            is AppResult.Success -> {
                when (val modelsResult = aiProviderRepository.getModels(config)) {
                    is AppResult.Failure -> ProviderDiagnostics(
                        provider = config.provider,
                        host = config.host,
                        port = config.port,
                        health = modelsResult.error.toProviderHealth(),
                        lastCheckedAtMillis = System.currentTimeMillis(),
                        latencyMillis = latencyMillis,
                        modelCount = null,
                        lastError = modelsResult.error.toDiagnosticMessage(),
                    )

                    is AppResult.Success -> ProviderDiagnostics(
                        provider = config.provider,
                        host = config.host,
                        port = config.port,
                        health = ProviderHealth.REACHABLE,
                        lastCheckedAtMillis = System.currentTimeMillis(),
                        latencyMillis = latencyMillis,
                        modelCount = modelsResult.data.size,
                        lastError = null,
                    )
                }
            }
        }

        return when (val saveResult = providerDiagnosticsRepository.saveProviderDiagnostics(diagnostics)) {
            is AppResult.Failure -> saveResult
            is AppResult.Success -> AppResult.Success(diagnostics)
        }
    }

    private fun AppError.toProviderHealth(): ProviderHealth =
        when (this) {
            AppError.Timeout -> ProviderHealth.TIMEOUT
            else -> ProviderHealth.OFFLINE
        }

    private fun AppError.toDiagnosticMessage(): String =
        when (this) {
            AppError.NetworkUnavailable -> "Provider is not reachable."
            AppError.Timeout -> "Provider request timed out."
            is AppError.Http -> "HTTP $code: ${message ?: "Request failed"}"
            is AppError.Server -> message
            is AppError.Unknown -> message ?: "Unknown error."
            else -> "Request failed."
        }
}
