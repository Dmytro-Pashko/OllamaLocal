package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.CapabilitySupport
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.connection.ProviderCapabilities
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import com.dpashko.localaiclient.domain.repositories.ProviderCapabilitiesRepository
import javax.inject.Inject

/**
 * Detects provider capability hints without sending prompt or conversation data.
 */
class DetectProviderCapabilitiesUseCase @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
    private val providerCapabilitiesRepository: ProviderCapabilitiesRepository,
) {
    suspend operator fun invoke(config: ConnectionConfig): AppResult<ProviderCapabilities> {
        val connectionResult = aiProviderRepository.checkConnection(config)
        val capabilities = when (connectionResult) {
            is AppResult.Success -> ProviderCapabilities(
                provider = config.provider,
                host = config.host,
                port = config.port,
                streaming = CapabilitySupport.SUPPORTED,
                tools = CapabilitySupport.UNKNOWN,
                embeddings = CapabilitySupport.UNKNOWN,
                vision = CapabilitySupport.UNKNOWN,
                lastCheckedAtMillis = System.currentTimeMillis(),
                lastError = null,
            )

            is AppResult.Failure -> ProviderCapabilities(
                provider = config.provider,
                host = config.host,
                port = config.port,
                streaming = CapabilitySupport.UNKNOWN,
                tools = CapabilitySupport.UNKNOWN,
                embeddings = CapabilitySupport.UNKNOWN,
                vision = CapabilitySupport.UNKNOWN,
                lastCheckedAtMillis = System.currentTimeMillis(),
                lastError = connectionResult.error.toCapabilityMessage(),
            )
        }

        return when (val saveResult = providerCapabilitiesRepository.saveProviderCapabilities(capabilities)) {
            is AppResult.Failure -> saveResult
            is AppResult.Success -> AppResult.Success(capabilities)
        }
    }

    private fun AppError.toCapabilityMessage(): String =
        when (this) {
            AppError.NetworkUnavailable -> "Provider is not reachable."
            AppError.Timeout -> "Provider request timed out."
            is AppError.Http -> "HTTP $code: ${message ?: "Request failed"}"
            is AppError.Server -> message
            is AppError.Unknown -> message ?: "Unknown error."
            else -> "Request failed."
        }
}
