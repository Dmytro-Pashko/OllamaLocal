package com.dpashko.localaiclient.data.repositories

import android.content.Context
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings
import com.dpashko.localaiclient.domain.repositories.GenerationSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GenerationSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : GenerationSettingsRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val settingsState = MutableStateFlow(readSettings())

    override fun observeGenerationSettings(): Flow<GenerationSettings> =
        settingsState.asStateFlow()

    override suspend fun saveGenerationSettings(settings: GenerationSettings): AppResult<Unit> =
        safePreferencesCall {
            preferences.edit()
                .putLong(KEY_GENERATION_TIMEOUT_MILLIS, settings.generationTimeoutMillis)
                .apply()
            settingsState.value = settings
        }

    override suspend fun resetGenerationSettings(): AppResult<Unit> =
        saveGenerationSettings(GenerationSettings.Default)

    private fun readSettings(): GenerationSettings =
        GenerationSettings(
            generationTimeoutMillis = preferences.getLong(
                KEY_GENERATION_TIMEOUT_MILLIS,
                GenerationSettings.DEFAULT_GENERATION_TIMEOUT_MILLIS,
            ),
        )

    private suspend fun safePreferencesCall(block: suspend () -> Unit): AppResult<Unit> =
        try {
            block()
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private companion object {
        const val PREFERENCES_NAME = "generation_settings"
        const val KEY_GENERATION_TIMEOUT_MILLIS = "generation_timeout_millis"
    }
}
