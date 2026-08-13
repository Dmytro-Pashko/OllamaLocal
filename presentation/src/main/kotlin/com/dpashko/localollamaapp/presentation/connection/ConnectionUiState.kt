package com.dpashko.localollamaapp.presentation.connection

import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.presentation.ui.models.OllamaModelUi

data class ConnectionUiState(
    val host: String = OllamaConnectionConfig.DEFAULT_HOST,
    val port: String = OllamaConnectionConfig.DEFAULT_PORT.toString(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val models: List<OllamaModelUi> = emptyList(),
    val selectedModelName: String? = null,
    val errorMessage: String? = null,
)
