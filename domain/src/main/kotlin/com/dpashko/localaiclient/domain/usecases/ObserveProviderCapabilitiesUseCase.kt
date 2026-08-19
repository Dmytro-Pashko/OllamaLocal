package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.repositories.ProviderCapabilitiesRepository
import javax.inject.Inject

class ObserveProviderCapabilitiesUseCase @Inject constructor(
    private val providerCapabilitiesRepository: ProviderCapabilitiesRepository,
) {
    operator fun invoke() = providerCapabilitiesRepository.observeProviderCapabilities()
}
