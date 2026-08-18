package com.dpashko.localaiclient.domain.models.settings

/**
 * User-configurable limits for local model generation.
 */
data class GenerationSettings(
    /** Maximum time a background generation may run before being treated as timed out. */
    val generationTimeoutMillis: Long = DEFAULT_GENERATION_TIMEOUT_MILLIS,
) {
    /** Timeout value converted for editing in minute-based UI controls. */
    val generationTimeoutMinutes: Int
        get() = (generationTimeoutMillis / MILLIS_PER_MINUTE).toInt()

    companion object {
        /** Default timeout for long-running local generation work. */
        const val DEFAULT_GENERATION_TIMEOUT_MILLIS = 3_600_000L
        /** Smallest accepted timeout value in minutes. */
        const val MIN_TIMEOUT_MINUTES = 1
        /** Largest accepted timeout value in minutes. */
        const val MAX_TIMEOUT_MINUTES = 1_440
        /** Number of milliseconds in one minute. */
        const val MILLIS_PER_MINUTE = 60_000L

        /** Default settings instance used by repositories and reset actions. */
        val Default = GenerationSettings()

        /** Creates settings from the minute value entered in the UI. */
        fun fromMinutes(minutes: Int): GenerationSettings =
            GenerationSettings(generationTimeoutMillis = minutes * MILLIS_PER_MINUTE)

        /** Returns whether [minutes] is inside the supported generation timeout range. */
        fun isValidMinutes(minutes: Int): Boolean =
            minutes in MIN_TIMEOUT_MINUTES..MAX_TIMEOUT_MINUTES
    }
}
