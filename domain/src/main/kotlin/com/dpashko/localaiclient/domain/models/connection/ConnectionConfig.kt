package com.dpashko.localaiclient.domain.models.connection

/**
 * Network connection details for a locally reachable AI provider.
 */
data class ConnectionConfig(
    /** Local provider implementation that defines protocol and default port. */
    val provider: AiProvider = AiProvider.OLLAMA,
    /** Hostname or LAN IP address of the machine running the provider. */
    val host: String = DEFAULT_HOST,
    /** TCP port exposed by the selected provider. */
    val port: Int = provider.defaultPort,
) {
    /** HTTP base URL used by the data layer clients. */
    val baseUrl: String
        get() = "http://$host:$port"

    companion object {
        /** Default LAN host prefilled on the connection screen. */
        const val DEFAULT_HOST = "192.168.0.44"
    }
}

/**
 * Supported local AI provider families.
 */
enum class AiProvider(
    /** Stable route token used in Compose Navigation arguments. */
    val routeValue: String,
    /** Name shown to the user in provider selectors and headers. */
    val displayName: String,
    /** Provider's conventional local HTTP port. */
    val defaultPort: Int,
) {
    /** Ollama provider using the native Ollama HTTP API. */
    OLLAMA(
        routeValue = "ollama",
        displayName = "Ollama",
        defaultPort = 11434,
    ),
    /** LM Studio provider using the OpenAI-compatible local API. */
    LM_STUDIO(
        routeValue = "lm-studio",
        displayName = "LM Studio",
        defaultPort = 1234,
    );

    companion object {
        /** Converts a navigation route token back to a provider, defaulting to Ollama. */
        fun fromRouteValue(value: String?): AiProvider =
            entries.firstOrNull { it.routeValue == value } ?: OLLAMA
    }
}
