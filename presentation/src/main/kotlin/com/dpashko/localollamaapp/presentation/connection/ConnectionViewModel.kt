package com.dpashko.localollamaapp.presentation.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.usecases.ConnectToOllamaUseCase
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
    private val connectToOllamaUseCase: ConnectToOllamaUseCase,
    private val getAvailableModelsUseCase: GetAvailableModelsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    fun onHostChanged(host: String) {
        _uiState.update {
            it.copy(
                host = host.trim(),
                isConnected = false,
                models = emptyList(),
                selectedModelName = null,
                errorMessage = null,
            )
        }
    }

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

            when (val connectionResult = connectToOllamaUseCase(config)) {
                is AppResult.Failure -> {
                    handleConnectionFailure(config, connectionResult.error)
                }

                is AppResult.Success -> loadModels(config)
            }
        }
    }

    private suspend fun loadModels(config: OllamaConnectionConfig) {
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

    private suspend fun handleConnectionFailure(
        config: OllamaConnectionConfig,
        error: AppError,
    ) {
        if (error == AppError.NetworkUnavailable && config.host.isLoopbackHost()) {
            val emulatorConfig = config.copy(host = ANDROID_EMULATOR_HOST)
            when (val fallbackResult = connectToOllamaUseCase(emulatorConfig)) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = loopbackErrorMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update { it.copy(host = emulatorConfig.host) }
                    loadModels(emulatorConfig)
                }
            }
            return
        }

        _uiState.update {
            it.copy(
                isConnecting = false,
                errorMessage = error.toUserMessage(),
            )
        }
    }

    private fun buildConfigOrNull(): OllamaConnectionConfig? {
        val state = _uiState.value
        val port = state.port.toIntOrNull() ?: return null
        return OllamaConnectionConfig(
            host = state.host,
            port = port,
        )
    }

    private fun String.isLoopbackHost(): Boolean =
        equals("127.0.0.1", ignoreCase = true) ||
            equals("localhost", ignoreCase = true) ||
            equals("::1", ignoreCase = true)

    private fun loopbackErrorMessage(): String =
        "127.0.0.1 points to this Android device. Use 10.0.2.2 on Android Emulator, or your computer LAN IP on a real device."

    private companion object {
        const val ANDROID_EMULATOR_HOST = "10.0.2.2"
    }
}
