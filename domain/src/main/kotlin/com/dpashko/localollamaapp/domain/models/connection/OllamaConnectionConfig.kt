package com.dpashko.localollamaapp.domain.models.connection

data class OllamaConnectionConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
) {
    val baseUrl: String
        get() = "http://$host:$port"

    companion object {
        const val DEFAULT_HOST = "192.168.0.44"
        const val DEFAULT_PORT = 11434
    }
}
