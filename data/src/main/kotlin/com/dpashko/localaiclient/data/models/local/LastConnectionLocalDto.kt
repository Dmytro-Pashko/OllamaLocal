package com.dpashko.localaiclient.data.models.local

import kotlinx.serialization.Serializable

/**
 * Serialized last successful connection stored in private preferences.
 */
@Serializable
data class LastConnectionLocalDto(
    val provider: String,
    val host: String,
    val port: Int,
    val modelName: String,
    val updatedAtMillis: Long,
)
