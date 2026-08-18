package com.dpashko.localaiclient.domain.models.settings

/**
 * User-configurable local access protection settings.
 */
data class SecuritySettings(
    /** True when app content should require Android device unlock before display. */
    val appLockEnabled: Boolean = false,
) {
    companion object {
        /** Default security posture: no extra app lock unless the user enables it. */
        val Default = SecuritySettings()
    }
}
