package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ProviderCapabilities
import kotlinx.coroutines.flow.Flow

/**
 * Stores locally detected provider capabilities.
 */
interface ProviderCapabilitiesRepository {
    fun observeProviderCapabilities(): Flow<ProviderCapabilities?>

    suspend fun saveProviderCapabilities(capabilities: ProviderCapabilities): AppResult<Unit>
}
