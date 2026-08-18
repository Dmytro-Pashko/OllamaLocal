package com.dpashko.localaiclient.domain.models.connection

/**
 * Last connection that successfully reached a provider and loaded models.
 */
data class LastConnection(
    /** Local provider used for the successful connection. */
    val provider: AiProvider,
    /** LAN host or IP address. */
    val host: String,
    /** TCP port for the provider. */
    val port: Int,
    /** Selected model name from the successful model list. */
    val modelName: String,
    /** Time when this connection was stored. */
    val updatedAtMillis: Long,
)
