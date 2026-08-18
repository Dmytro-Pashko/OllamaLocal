package com.dpashko.localaiclient.presentation.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset
import com.dpashko.localaiclient.domain.models.connection.LastConnection
import com.dpashko.localaiclient.domain.models.connection.ProviderHealth
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.usecases.ApplyConnectionPresetUseCase
import com.dpashko.localaiclient.domain.usecases.ConnectToProviderUseCase
import com.dpashko.localaiclient.domain.usecases.DeleteConnectionPresetUseCase
import com.dpashko.localaiclient.domain.usecases.GetAvailableModelsUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveConnectionPresetsUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveLastConnectionUseCase
import com.dpashko.localaiclient.domain.usecases.SaveConnectionPresetUseCase
import com.dpashko.localaiclient.domain.usecases.SaveLastConnectionUseCase
import com.dpashko.localaiclient.presentation.common.toUserMessage
import com.dpashko.localaiclient.presentation.ui.models.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val observeConnectionPresetsUseCase: ObserveConnectionPresetsUseCase,
    private val observeLastConnectionUseCase: ObserveLastConnectionUseCase,
    private val saveConnectionPresetUseCase: SaveConnectionPresetUseCase,
    private val saveLastConnectionUseCase: SaveLastConnectionUseCase,
    private val deleteConnectionPresetUseCase: DeleteConnectionPresetUseCase,
    private val applyConnectionPresetUseCase: ApplyConnectionPresetUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeConnectionPresetsUseCase().collect { presets ->
                _uiState.update { it.copy(presets = presets) }
            }
        }

        viewModelScope.launch {
            observeLastConnectionUseCase().first()?.let { connection ->
                _uiState.update {
                    it.copy(
                        provider = connection.provider,
                        host = connection.host,
                        port = connection.port.toString(),
                        selectedModelName = connection.modelName,
                        selectedPresetId = null,
                        isConnected = false,
                        isRefreshingModels = false,
                        models = emptyList(),
                        errorMessage = null,
                    )
                }
            }
        }
    }

    /**
     * Accepts only numeric port input and resets provider discovery state.
     */
    fun onPortChanged(port: String) {
        _uiState.update {
            it.copy(
                port = port.filter(Char::isDigit),
                selectedPresetId = null,
                isConnected = false,
                isRefreshingModels = false,
                providerHealth = ProviderHealth.NOT_CHECKED,
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
                selectedPresetId = null,
                isConnected = false,
                isRefreshingModels = false,
                providerHealth = ProviderHealth.NOT_CHECKED,
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
                selectedPresetId = null,
                isConnected = false,
                isRefreshingModels = false,
                providerHealth = ProviderHealth.NOT_CHECKED,
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
     * Applies a saved preset to the editable connection fields.
     */
    fun applyPreset(presetId: String) {
        val preset = _uiState.value.presets.firstOrNull { it.id == presetId } ?: return
        when (val result = applyConnectionPresetUseCase(preset)) {
            is AppResult.Failure -> {
                _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
            }

            is AppResult.Success -> {
                val appliedPreset = result.data
                _uiState.update {
                    it.copy(
                        provider = appliedPreset.provider,
                        host = appliedPreset.host,
                        port = appliedPreset.port.toString(),
                        selectedModelName = appliedPreset.modelName,
                        selectedPresetId = appliedPreset.id,
                        isConnected = false,
                        isRefreshingModels = false,
                        providerHealth = ProviderHealth.NOT_CHECKED,
                        models = emptyList(),
                        errorMessage = null,
                    )
                }
            }
        }
    }

    /**
     * Saves current connection input as a local preset.
     */
    fun saveCurrentAsPreset(name: String) {
        val state = _uiState.value
        val port = state.port.toIntOrNull()
        if (name.isBlank() || state.host.isBlank() || port == null) {
            _uiState.update { it.copy(errorMessage = AppError.InvalidConnectionConfig.toUserMessage()) }
            return
        }

        val presetId = state.selectedPresetId ?: "preset-${System.currentTimeMillis()}"
        val preset = ConnectionPreset(
            id = presetId,
            name = name.trim(),
            provider = state.provider,
            host = state.host.trim(),
            port = port,
            modelName = state.selectedModelName,
            updatedAtMillis = System.currentTimeMillis(),
        )

        viewModelScope.launch {
            when (val result = saveConnectionPresetUseCase(preset)) {
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPresetId = preset.id,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * Deletes a saved preset from local storage.
     */
    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            when (val result = deleteConnectionPresetUseCase(presetId)) {
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPresetId = it.selectedPresetId?.takeIf { id -> id != presetId },
                            errorMessage = null,
                        )
                    }
                }
            }
        }
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
                    isRefreshingModels = false,
                    providerHealth = ProviderHealth.CHECKING,
                    isConnected = false,
                    models = emptyList(),
                    errorMessage = null,
                )
            }

            when (val connectionResult = connectToProviderUseCase(config)) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            providerHealth = connectionResult.error.toProviderHealth(),
                            errorMessage = connectionResult.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> loadModels(
                    config = config,
                    preserveExistingModelsOnFailure = false,
                )
            }
        }
    }

    /**
     * Refreshes models without requiring a full reconnect or clearing the previous model list on failure.
     */
    fun refreshModels() {
        val config = buildConfigOrNull()
        if (config == null) {
            _uiState.update {
                it.copy(errorMessage = AppError.InvalidConnectionConfig.toUserMessage())
            }
            return
        }

        if (_uiState.value.isBusy || !_uiState.value.isConnected) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshingModels = true,
                    providerHealth = ProviderHealth.CHECKING,
                    errorMessage = null,
                )
            }

            loadModels(
                config = config,
                preserveExistingModelsOnFailure = true,
            )
        }
    }

    /**
     * Loads models after a successful provider connection.
     */
    private suspend fun loadModels(
        config: ConnectionConfig,
        preserveExistingModelsOnFailure: Boolean,
    ) {
        val preferredModelName = _uiState.value.selectedModelName
        when (val modelsResult = getAvailableModelsUseCase(config)) {
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isRefreshingModels = false,
                        isConnected = if (preserveExistingModelsOnFailure) it.isConnected else false,
                        providerHealth = modelsResult.error.toProviderHealth(),
                        errorMessage = modelsResult.error.toUserMessage(),
                    )
                }
            }

            is AppResult.Success -> {
                val models = modelsResult.data.map { it.toUi() }
                val selectedModelName = preferredModelName
                    ?.takeIf { modelName -> models.any { it.name == modelName } }
                    ?: models.firstOrNull()?.name
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isRefreshingModels = false,
                        isConnected = true,
                        providerHealth = ProviderHealth.REACHABLE,
                        models = models,
                        selectedModelName = selectedModelName,
                    )
                }
                selectedModelName?.let { modelName ->
                    saveLastConnectionUseCase(
                        LastConnection(
                            provider = config.provider,
                            host = config.host,
                            port = config.port,
                            modelName = modelName,
                            updatedAtMillis = System.currentTimeMillis(),
                        ),
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

    private fun AppError.toProviderHealth(): ProviderHealth =
        when (this) {
            AppError.Timeout -> ProviderHealth.TIMEOUT
            else -> ProviderHealth.OFFLINE
        }
}
