package com.dpashko.localaiclient.presentation.serverselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset
import com.dpashko.localaiclient.domain.models.connection.LastConnection
import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics
import com.dpashko.localaiclient.domain.models.connection.ProviderHealth
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.usecases.ConnectToProviderUseCase
import com.dpashko.localaiclient.domain.usecases.DeleteConnectionPresetUseCase
import com.dpashko.localaiclient.domain.usecases.GetAvailableModelsUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveConnectionPresetsUseCase
import com.dpashko.localaiclient.domain.usecases.SaveLastConnectionUseCase
import com.dpashko.localaiclient.domain.usecases.SaveProviderDiagnosticsUseCase
import com.dpashko.localaiclient.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads saved local server presets for the entry screen.
 */
@HiltViewModel
class ServerSelectionViewModel @Inject constructor(
    private val connectToProviderUseCase: ConnectToProviderUseCase,
    private val deleteConnectionPresetUseCase: DeleteConnectionPresetUseCase,
    private val getAvailableModelsUseCase: GetAvailableModelsUseCase,
    private val saveLastConnectionUseCase: SaveLastConnectionUseCase,
    private val saveProviderDiagnosticsUseCase: SaveProviderDiagnosticsUseCase,
    observeConnectionPresetsUseCase: ObserveConnectionPresetsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServerSelectionUiState())
    val uiState: StateFlow<ServerSelectionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ServerSelectionEvent>()
    val events: SharedFlow<ServerSelectionEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeConnectionPresetsUseCase().collect { presets ->
                _uiState.update { it.copy(presets = presets) }
            }
        }
    }

    fun connectPreset(presetId: String) {
        if (_uiState.value.isConnecting) return
        val preset = _uiState.value.presets.firstOrNull { it.id == presetId } ?: return
        val config = ConnectionConfig(
            provider = preset.provider,
            host = preset.host,
            port = preset.port,
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectingPresetId = presetId,
                    errorMessage = null,
                )
            }

            when (val connectionResult = connectToProviderUseCase(config)) {
                is AppResult.Failure -> handleConnectionFailure(config, connectionResult.error)
                is AppResult.Success -> loadPresetModels(preset, config)
            }
        }
    }

    fun requestDeletePreset(presetId: String) {
        val preset = _uiState.value.presets.firstOrNull { it.id == presetId } ?: return
        _uiState.update { it.copy(deletingPresetCandidate = preset) }
    }

    fun dismissDeletePreset() {
        _uiState.update { it.copy(deletingPresetCandidate = null) }
    }

    fun confirmDeletePreset() {
        val preset = _uiState.value.deletingPresetCandidate ?: return
        viewModelScope.launch {
            when (val result = deleteConnectionPresetUseCase(preset.id)) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            deletingPresetCandidate = null,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            deletingPresetCandidate = null,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadPresetModels(
        preset: ConnectionPreset,
        config: ConnectionConfig,
    ) {
        when (val modelsResult = getAvailableModelsUseCase(config)) {
            is AppResult.Failure -> handleConnectionFailure(config, modelsResult.error)
            is AppResult.Success -> {
                val selectedModelName = preset.modelName
                    ?.takeIf { savedModelName ->
                        modelsResult.data.any { it.name == savedModelName }
                    }
                    ?: modelsResult.data.first().name

                saveDiagnostics(
                    config = config,
                    health = ProviderHealth.REACHABLE,
                    modelCount = modelsResult.data.size,
                    lastError = null,
                )
                saveLastConnectionUseCase(
                    LastConnection(
                        provider = config.provider,
                        host = config.host,
                        port = config.port,
                        modelName = selectedModelName,
                        updatedAtMillis = System.currentTimeMillis(),
                    ),
                )
                _uiState.update {
                    it.copy(
                        connectingPresetId = null,
                        errorMessage = null,
                    )
                }
                _events.emit(
                    ServerSelectionEvent.OpenConnected(
                        provider = config.provider,
                        host = config.host,
                        port = config.port,
                        modelName = selectedModelName,
                    ),
                )
            }
        }
    }

    private suspend fun handleConnectionFailure(
        config: ConnectionConfig,
        error: AppError,
    ) {
        val health = error.toProviderHealth()
        val message = error.toUserMessage()
        saveDiagnostics(
            config = config,
            health = health,
            modelCount = null,
            lastError = message,
        )
        _uiState.update {
            it.copy(
                connectingPresetId = null,
                errorMessage = message,
            )
        }
    }

    private fun AppError.toProviderHealth(): ProviderHealth =
        when (this) {
            AppError.Timeout -> ProviderHealth.TIMEOUT
            else -> ProviderHealth.OFFLINE
        }

    private suspend fun saveDiagnostics(
        config: ConnectionConfig,
        health: ProviderHealth,
        modelCount: Int?,
        lastError: String?,
    ) {
        saveProviderDiagnosticsUseCase(
            ProviderDiagnostics(
                provider = config.provider,
                host = config.host,
                port = config.port,
                health = health,
                lastCheckedAtMillis = System.currentTimeMillis(),
                latencyMillis = null,
                modelCount = modelCount,
                lastError = lastError,
            ),
        )
    }
}

sealed interface ServerSelectionEvent {
    data class OpenConnected(
        val provider: AiProvider,
        val host: String,
        val port: Int,
        val modelName: String,
    ) : ServerSelectionEvent
}
