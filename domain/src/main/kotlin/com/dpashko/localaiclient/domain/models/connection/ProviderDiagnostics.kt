package com.dpashko.localaiclient.domain.models.connection

/**
 * Last known diagnostics for a local provider connection.
 */
data class ProviderDiagnostics(
    val provider: AiProvider,
    val host: String,
    val port: Int,
    val health: ProviderHealth,
    val lastCheckedAtMillis: Long,
    val latencyMillis: Long?,
    val modelCount: Int?,
    val lastError: String?,
) {
    val providerUrl: String
        get() = "http://$host:$port"
}
