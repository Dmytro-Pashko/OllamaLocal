package com.dpashko.localollamaapp.domain.models.connection

data class ConnectionConfig(
    val provider: AiProvider = AiProvider.OLLAMA,
    val host: String = DEFAULT_HOST,
    val port: Int = provider.defaultPort,
) {
    val baseUrl: String
        get() = "http://$host:$port"

    companion object {
        const val DEFAULT_HOST = "192.168.0.44"
    }
}

enum class AiProvider(
    val routeValue: String,
    val displayName: String,
    val defaultPort: Int,
) {
    OLLAMA(
        routeValue = "ollama",
        displayName = "Ollama",
        defaultPort = 11434,
    ),
    LM_STUDIO(
        routeValue = "lm-studio",
        displayName = "LM Studio",
        defaultPort = 1234,
    );

    companion object {
        fun fromRouteValue(value: String?): AiProvider =
            entries.firstOrNull { it.routeValue == value } ?: OLLAMA
    }
}
