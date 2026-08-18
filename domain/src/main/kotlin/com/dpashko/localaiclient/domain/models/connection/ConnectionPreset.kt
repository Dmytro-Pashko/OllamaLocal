package com.dpashko.localaiclient.domain.models.connection

/**
 * Saved local provider profile used to quickly restore connection input.
 */
data class ConnectionPreset(
    /** Stable local identifier for this preset. */
    val id: String,
    /** User-facing preset name. */
    val name: String,
    /** Local provider selected for this preset. */
    val provider: AiProvider,
    /** LAN host or IP address. */
    val host: String,
    /** TCP port for the provider. */
    val port: Int,
    /** Optional model name selected when the preset was saved. */
    val modelName: String?,
    /** Last time this preset was created or updated. */
    val updatedAtMillis: Long,
)
