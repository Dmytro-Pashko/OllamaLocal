package com.dpashko.localollamaapp.domain.models.connection

data class OllamaConnectionConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
) {
    val baseUrl: String
        get() = "http://$host:$port"

    companion object {
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 11434
    }
}
