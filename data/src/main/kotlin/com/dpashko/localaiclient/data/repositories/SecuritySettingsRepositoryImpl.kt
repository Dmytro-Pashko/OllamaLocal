package com.dpashko.localaiclient.data.repositories

import android.content.Context
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.settings.SecuritySettings
import com.dpashko.localaiclient.domain.repositories.SecuritySettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecuritySettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : SecuritySettingsRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val settingsState = MutableStateFlow(readSettings())

    override fun observeSecuritySettings(): Flow<SecuritySettings> =
        settingsState.asStateFlow()

    override suspend fun saveSecuritySettings(settings: SecuritySettings): AppResult<Unit> =
        safePreferencesCall {
            preferences.edit()
                .putBoolean(KEY_APP_LOCK_ENABLED, settings.appLockEnabled)
                .apply()
            settingsState.value = settings
        }

    private fun readSettings(): SecuritySettings =
        SecuritySettings(
            appLockEnabled = preferences.getBoolean(
                KEY_APP_LOCK_ENABLED,
                SecuritySettings.Default.appLockEnabled,
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
        const val PREFERENCES_NAME = "security_settings"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}
