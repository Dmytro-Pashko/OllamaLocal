package com.dpashko.localaiclient.data.models.local

import kotlinx.serialization.Serializable

/**
 * Serialized local connection preset stored in private preferences.
 */
@Serializable
data class ConnectionPresetLocalDto(
    val id: String,
    val name: String,
    val provider: String,
    val host: String,
    val port: Int,
    val modelName: String?,
    val updatedAtMillis: Long,
)
