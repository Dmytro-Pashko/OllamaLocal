package com.dpashko.localaiclient.presentation.connection

import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.presentation.ui.models.AiModelUi

/**
 * Immutable UI state for connecting to a provider on the local network.
 */
data class ConnectionUiState(
    /** Selected local AI provider. */
    val provider: AiProvider = AiProvider.OLLAMA,
    /** Provider host or LAN IP entered by the user. */
    val host: String = ConnectionConfig.DEFAULT_HOST,
    /** Port text kept as a string so invalid partial input can be represented. */
    val port: String = AiProvider.OLLAMA.defaultPort.toString(),
    /** True while connection and model discovery are running. */
    val isConnecting: Boolean = false,
    /** True after a successful connection and non-empty model list. */
    val isConnected: Boolean = false,
    /** Models returned by the connected provider. */
    val models: List<AiModelUi> = emptyList(),
    /** Currently selected model name, if a model is available. */
    val selectedModelName: String? = null,
    /** User-facing error text for connection or discovery failures. */
    val errorMessage: String? = null,
)
