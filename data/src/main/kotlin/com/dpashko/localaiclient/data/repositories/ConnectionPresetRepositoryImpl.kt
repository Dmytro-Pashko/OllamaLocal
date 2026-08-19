package com.dpashko.localaiclient.data.repositories

import android.content.Context
import com.dpashko.localaiclient.data.models.local.ConnectionPresetLocalDto
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ConnectionPresetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ConnectionPresetRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) : ConnectionPresetRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val serializer = ListSerializer(ConnectionPresetLocalDto.serializer())
    private val presetsState = MutableStateFlow(readPresets())

    override fun observeConnectionPresets(): Flow<List<ConnectionPreset>> =
        presetsState.asStateFlow()

    override suspend fun getConnectionPresets(): AppResult<List<ConnectionPreset>> =
        try {
            AppResult.Success(presetsState.value)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    override suspend fun saveConnectionPreset(preset: ConnectionPreset): AppResult<Unit> =
        safePreferencesCall {
            val existingPresets = presetsState.value
            val updatedPreset = preset.copy(
                name = preset.name.trim(),
                host = preset.host.trim(),
            )
            val updatedPresets = (
                existingPresets.filterNot { it.id == updatedPreset.id } + updatedPreset
                )
                .sortedByDescending { it.updatedAtMillis }
                .take(MAX_PRESETS)
            writePresets(updatedPresets)
        }

    override suspend fun deleteConnectionPreset(presetId: String): AppResult<Unit> =
        safePreferencesCall {
            writePresets(presetsState.value.filterNot { it.id == presetId })
        }

    private fun readPresets(): List<ConnectionPreset> {
        val rawPresets = preferences.getString(KEY_PRESETS, null) ?: return emptyList()
        return try {
            json.decodeFromString(serializer, rawPresets)
                .map { it.toDomain() }
                .sortedByDescending { it.updatedAtMillis }
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun writePresets(presets: List<ConnectionPreset>) {
        preferences.edit()
            .putString(KEY_PRESETS, json.encodeToString(serializer, presets.map { it.toLocalDto() }))
            .apply()
        presetsState.value = presets
    }

    private suspend fun safePreferencesCall(block: suspend () -> Unit): AppResult<Unit> =
        try {
            block()
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private fun ConnectionPresetLocalDto.toDomain(): ConnectionPreset =
        ConnectionPreset(
            id = id,
            name = name,
            provider = AiProvider.fromRouteValue(provider),
            host = host,
            port = port,
            modelName = modelName,
            updatedAtMillis = updatedAtMillis,
        )

    private fun ConnectionPreset.toLocalDto(): ConnectionPresetLocalDto =
        ConnectionPresetLocalDto(
            id = id,
            name = name,
            provider = provider.routeValue,
            host = host,
            port = port,
            modelName = modelName,
            updatedAtMillis = updatedAtMillis,
        )

    private companion object {
        const val PREFERENCES_NAME = "connection_presets"
        const val KEY_PRESETS = "presets"
        const val MAX_PRESETS = 20
    }
}
