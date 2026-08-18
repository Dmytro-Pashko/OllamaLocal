package com.dpashko.localaiclient.presentation.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.usecases.ConnectToProviderUseCase
import com.dpashko.localaiclient.domain.usecases.GetAvailableModelsUseCase
import com.dpashko.localaiclient.presentation.common.toUserMessage
import com.dpashko.localaiclient.presentation.ui.models.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages provider connection input, local model discovery, and selected model state.
 */
@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectToProviderUseCase: ConnectToProviderUseCase,
    private val getAvailableModelsUseCase: GetAvailableModelsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    /**
     * Accepts only numeric port input and resets provider discovery state.
     */
    fun onPortChanged(port: String) {
        _uiState.update {
            it.copy(
                port = port.filter(Char::isDigit),
                isConnected = false,
                models = emptyList(),
                selectedModelName = null,
                errorMessage = null,
            )
        }
    }

    /**
     * Switches provider and pre-fills its conventional local port.
     */
    fun onProviderSelected(provider: AiProvider) {
        _uiState.update {
            it.copy(
                provider = provider,
                port = provider.defaultPort.toString(),
                isConnected = false,
                models = emptyList(),
                selectedModelName = null,
                errorMessage = null,
            )
        }
    }

    /**
     * Updates the provider host and clears any connection result tied to the old host.
     */
    fun onHostChanged(host: String) {
        _uiState.update {
            it.copy(
                host = host,
                isConnected = false,
                models = emptyList(),
                selectedModelName = null,
                errorMessage = null,
            )
        }
    }

    /**
     * Stores the model selected by the user after provider discovery.
     */
    fun onModelSelected(modelName: String) {
        _uiState.update { it.copy(selectedModelName = modelName) }
    }

    /**
     * Validates the current input, checks provider availability, and loads models.
     */
    fun connect() {
        val config = buildConfigOrNull()
        if (config == null) {
            _uiState.update {
                it.copy(errorMessage = AppError.InvalidConnectionConfig.toUserMessage())
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConnecting = true,
                    isConnected = false,
                    models = emptyList(),
                    selectedModelName = null,
                    errorMessage = null,
                )
            }

            when (val connectionResult = connectToProviderUseCase(config)) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = connectionResult.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> loadModels(config)
            }
        }
    }

    /**
     * Loads models after a successful provider connection.
     */
    private suspend fun loadModels(config: ConnectionConfig) {
        when (val modelsResult = getAvailableModelsUseCase(config)) {
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = modelsResult.error.toUserMessage(),
                    )
                }
            }

            is AppResult.Success -> {
                val models = modelsResult.data.map { it.toUi() }
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        models = models,
                        selectedModelName = models.firstOrNull()?.name,
                    )
                }
            }
        }
    }

    /**
     * Builds a domain connection config or returns null when the port is invalid.
     */
    private fun buildConfigOrNull(): ConnectionConfig? {
        val state = _uiState.value
        val port = state.port.toIntOrNull() ?: return null
        return ConnectionConfig(
            provider = state.provider,
            host = state.host,
            port = port,
        )
    }
}
