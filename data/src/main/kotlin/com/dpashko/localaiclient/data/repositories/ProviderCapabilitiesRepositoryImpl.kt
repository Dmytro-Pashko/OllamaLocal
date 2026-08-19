package com.dpashko.localaiclient.data.repositories

import android.content.Context
import com.dpashko.localaiclient.data.models.local.ProviderCapabilitiesLocalDto
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.CapabilitySupport
import com.dpashko.localaiclient.domain.models.connection.ProviderCapabilities
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ProviderCapabilitiesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class ProviderCapabilitiesRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) : ProviderCapabilitiesRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val capabilitiesState = MutableStateFlow(readCapabilities())

    override fun observeProviderCapabilities(): Flow<ProviderCapabilities?> =
        capabilitiesState.asStateFlow()

    override suspend fun saveProviderCapabilities(capabilities: ProviderCapabilities): AppResult<Unit> =
        try {
            preferences.edit()
                .putString(KEY_CAPABILITIES, json.encodeToString(capabilities.toLocalDto()))
                .apply()
            capabilitiesState.value = capabilities
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private fun readCapabilities(): ProviderCapabilities? {
        val rawCapabilities = preferences.getString(KEY_CAPABILITIES, null) ?: return null
        return try {
            json.decodeFromString<ProviderCapabilitiesLocalDto>(rawCapabilities).toDomain()
        } catch (exception: Exception) {
            null
        }
    }

    private fun ProviderCapabilitiesLocalDto.toDomain(): ProviderCapabilities =
        ProviderCapabilities(
            provider = AiProvider.fromRouteValue(provider),
            host = host,
            port = port,
            streaming = streaming.toCapabilitySupport(),
            tools = tools.toCapabilitySupport(),
            embeddings = embeddings.toCapabilitySupport(),
            vision = vision.toCapabilitySupport(),
            lastCheckedAtMillis = lastCheckedAtMillis,
            lastError = lastError,
        )

    private fun ProviderCapabilities.toLocalDto(): ProviderCapabilitiesLocalDto =
        ProviderCapabilitiesLocalDto(
            provider = provider.routeValue,
            host = host,
            port = port,
            streaming = streaming.name,
            tools = tools.name,
            embeddings = embeddings.name,
            vision = vision.name,
            lastCheckedAtMillis = lastCheckedAtMillis,
            lastError = lastError,
        )

    private fun String.toCapabilitySupport(): CapabilitySupport =
        CapabilitySupport.entries.firstOrNull { it.name == this } ?: CapabilitySupport.UNKNOWN

    private companion object {
        const val PREFERENCES_NAME = "provider_capabilities"
        const val KEY_CAPABILITIES = "capabilities"
    }
}
