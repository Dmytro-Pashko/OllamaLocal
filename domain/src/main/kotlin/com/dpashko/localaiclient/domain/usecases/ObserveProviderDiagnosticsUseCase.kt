package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ProviderDiagnosticsRepository
import javax.inject.Inject

/**
 * Observes the last known local provider diagnostics.
 */
class ObserveProviderDiagnosticsUseCase @Inject constructor(
    private val providerDiagnosticsRepository: ProviderDiagnosticsRepository,
) {
    operator fun invoke() = providerDiagnosticsRepository.observeProviderDiagnostics()
}
