package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics
import com.dpashko.localaiclient.domain.repositories.ProviderDiagnosticsRepository
import javax.inject.Inject

/**
 * Saves local provider diagnostics.
 */
class SaveProviderDiagnosticsUseCase @Inject constructor(
    private val providerDiagnosticsRepository: ProviderDiagnosticsRepository,
) {
    suspend operator fun invoke(diagnostics: ProviderDiagnostics) =
        providerDiagnosticsRepository.saveProviderDiagnostics(diagnostics)
}
