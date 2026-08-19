package com.dpashko.localaiclient.data.models.local

import kotlinx.serialization.Serializable

/**
 * Serialized provider diagnostics stored in private preferences.
 */
@Serializable
data class ProviderDiagnosticsLocalDto(
    val provider: String,
    val host: String,
    val port: Int,
    val health: String,
    val lastCheckedAtMillis: Long,
    val latencyMillis: Long?,
    val modelCount: Int?,
    val lastError: String?,
)
