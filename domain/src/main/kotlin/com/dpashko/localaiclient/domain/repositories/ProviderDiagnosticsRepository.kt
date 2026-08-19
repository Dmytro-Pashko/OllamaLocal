package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics
import kotlinx.coroutines.flow.Flow

/**
 * Stores last known provider diagnostics locally.
 */
interface ProviderDiagnosticsRepository {
    fun observeProviderDiagnostics(): Flow<ProviderDiagnostics?>

    suspend fun saveProviderDiagnostics(diagnostics: ProviderDiagnostics): AppResult<Unit>
}
