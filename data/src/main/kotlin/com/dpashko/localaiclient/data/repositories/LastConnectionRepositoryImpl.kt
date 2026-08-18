package com.dpashko.localaiclient.data.repositories

import android.content.Context
import com.dpashko.localaiclient.data.models.local.LastConnectionLocalDto
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.LastConnection
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.LastConnectionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class LastConnectionRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) : LastConnectionRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val connectionState = MutableStateFlow(readLastConnection())

    override fun observeLastConnection(): Flow<LastConnection?> =
        connectionState.asStateFlow()

    override suspend fun saveLastConnection(connection: LastConnection): AppResult<Unit> =
        safePreferencesCall {
            val savedConnection = connection.copy(host = connection.host.trim())
            preferences.edit()
                .putString(KEY_LAST_CONNECTION, json.encodeToString(savedConnection.toLocalDto()))
                .apply()
            connectionState.value = savedConnection
        }

    private fun readLastConnection(): LastConnection? {
        val rawConnection = preferences.getString(KEY_LAST_CONNECTION, null) ?: return null
        return try {
            json.decodeFromString<LastConnectionLocalDto>(rawConnection).toDomain()
        } catch (exception: Exception) {
            null
        }
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

    private fun LastConnectionLocalDto.toDomain(): LastConnection =
        LastConnection(
            provider = AiProvider.fromRouteValue(provider),
            host = host,
            port = port,
            modelName = modelName,
            updatedAtMillis = updatedAtMillis,
        )

    private fun LastConnection.toLocalDto(): LastConnectionLocalDto =
        LastConnectionLocalDto(
            provider = provider.routeValue,
            host = host,
            port = port,
            modelName = modelName,
            updatedAtMillis = updatedAtMillis,
        )

    private companion object {
        const val PREFERENCES_NAME = "last_connection"
        const val KEY_LAST_CONNECTION = "connection"
    }
}
