package com.dpashko.localaiclient.data.models.local

import kotlinx.serialization.Serializable

@Serializable
data class ProviderCapabilitiesLocalDto(
    val provider: String,
    val host: String,
    val port: Int,
    val streaming: String,
    val tools: String,
    val embeddings: String,
    val vision: String,
    val lastCheckedAtMillis: Long,
    val lastError: String?,
)
