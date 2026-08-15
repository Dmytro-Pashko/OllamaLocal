package com.dpashko.localollamaapp.presentation.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.AiProvider
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.usecases.ConnectToProviderUseCase
import com.dpashko.localollamaapp.domain.usecases.GetAvailableModelsUseCase
import com.dpashko.localollamaapp.presentation.common.toUserMessage
import com.dpashko.localollamaapp.presentation.ui.models.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectToProviderUseCase: ConnectToProviderUseCase,
    private val getAvailableModelsUseCase: GetAvailableModelsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

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

    fun onModelSelected(modelName: String) {
        _uiState.update { it.copy(selectedModelName = modelName) }
    }

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
