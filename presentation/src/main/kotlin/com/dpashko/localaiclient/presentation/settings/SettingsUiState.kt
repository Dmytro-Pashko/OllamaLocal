package com.dpashko.localaiclient.presentation.settings

/**
 * Immutable UI state for generation settings.
 */
data class SettingsUiState(
    /** Draft timeout value shown in the minutes input. */
    val timeoutMinutesText: String = "",
    /** Draft app-lock toggle shown in security settings. */
    val appLockEnabled: Boolean = false,
    /** True while settings are being persisted. */
    val isApplying: Boolean = false,
    /** User-facing validation or persistence error. */
    val errorMessage: String? = null,
)
