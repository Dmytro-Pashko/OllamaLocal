package com.dpashko.localollamaapp.presentation.connection

import com.dpashko.localollamaapp.domain.models.connection.AiProvider
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.presentation.ui.models.AiModelUi

data class ConnectionUiState(
    val provider: AiProvider = AiProvider.OLLAMA,
    val host: String = ConnectionConfig.DEFAULT_HOST,
    val port: String = AiProvider.OLLAMA.defaultPort.toString(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val models: List<AiModelUi> = emptyList(),
    val selectedModelName: String? = null,
    val errorMessage: String? = null,
)
