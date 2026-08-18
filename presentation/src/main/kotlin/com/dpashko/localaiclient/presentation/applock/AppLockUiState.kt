package com.dpashko.localaiclient.presentation.applock

/**
 * UI state for the app-level content lock gate.
 */
data class AppLockUiState(
    /** True until security settings are loaded from local storage. */
    val isLoading: Boolean = true,
    /** True when the user enabled app lock in settings. */
    val isLockRequired: Boolean = false,
    /** True after the current process has passed Android device unlock. */
    val isUnlocked: Boolean = false,
    /** Monotonic request id used by UI to launch the system unlock prompt. */
    val unlockRequestNonce: Int = 0,
    /** Optional error shown when unlock cannot complete. */
    val errorMessage: String? = null,
) {
    /** True when private app content can be rendered. */
    val canShowContent: Boolean
        get() = !isLoading && (!isLockRequired || isUnlocked)
}
