package com.dpashko.localaiclient.data.repositories

import android.content.Context
import com.dpashko.localaiclient.data.models.local.ProviderDiagnosticsLocalDto
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics
import com.dpashko.localaiclient.domain.models.connection.ProviderHealth
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ProviderDiagnosticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class ProviderDiagnosticsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) : ProviderDiagnosticsRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val diagnosticsState = MutableStateFlow(readDiagnostics())

    override fun observeProviderDiagnostics(): Flow<ProviderDiagnostics?> =
        diagnosticsState.asStateFlow()

    override suspend fun saveProviderDiagnostics(diagnostics: ProviderDiagnostics): AppResult<Unit> =
        try {
            preferences.edit()
                .putString(KEY_DIAGNOSTICS, json.encodeToString(diagnostics.toLocalDto()))
                .apply()
            diagnosticsState.value = diagnostics
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private fun readDiagnostics(): ProviderDiagnostics? {
        val rawDiagnostics = preferences.getString(KEY_DIAGNOSTICS, null) ?: return null
        return try {
            json.decodeFromString<ProviderDiagnosticsLocalDto>(rawDiagnostics).toDomain()
        } catch (exception: Exception) {
            null
        }
    }

    private fun ProviderDiagnosticsLocalDto.toDomain(): ProviderDiagnostics =
        ProviderDiagnostics(
            provider = AiProvider.fromRouteValue(provider),
            host = host,
            port = port,
            health = ProviderHealth.entries.firstOrNull { it.name == health } ?: ProviderHealth.NOT_CHECKED,
            lastCheckedAtMillis = lastCheckedAtMillis,
            latencyMillis = latencyMillis,
            modelCount = modelCount,
            lastError = lastError,
        )

    private fun ProviderDiagnostics.toLocalDto(): ProviderDiagnosticsLocalDto =
        ProviderDiagnosticsLocalDto(
            provider = provider.routeValue,
            host = host,
            port = port,
            health = health.name,
            lastCheckedAtMillis = lastCheckedAtMillis,
            latencyMillis = latencyMillis,
            modelCount = modelCount,
            lastError = lastError,
        )

    private companion object {
        const val PREFERENCES_NAME = "provider_diagnostics"
        const val KEY_DIAGNOSTICS = "diagnostics"
    }
}
