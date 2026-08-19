package com.dpashko.localaiclient.domain.models.connection

/**
 * Last known local capability snapshot for a configured provider endpoint.
 */
data class ProviderCapabilities(
    val provider: AiProvider,
    val host: String,
    val port: Int,
    val streaming: CapabilitySupport,
    val tools: CapabilitySupport,
    val embeddings: CapabilitySupport,
    val vision: CapabilitySupport,
    val lastCheckedAtMillis: Long,
    val lastError: String?,
)
