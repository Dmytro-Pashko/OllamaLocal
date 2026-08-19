package com.dpashko.localaiclient.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.usecases.ObserveActiveGenerationsUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveProviderDiagnosticsUseCase
import com.dpashko.localaiclient.domain.usecases.RefreshProviderDiagnosticsUseCase
import com.dpashko.localaiclient.domain.usecases.StopAllGenerationsUseCase
import com.dpashko.localaiclient.domain.usecases.StopGenerationUseCase
import com.dpashko.localaiclient.presentation.Routes
import com.dpashko.localaiclient.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates connected dashboard state and global generation actions.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeActiveGenerationsUseCase: ObserveActiveGenerationsUseCase,
    observeProviderDiagnosticsUseCase: ObserveProviderDiagnosticsUseCase,
    private val refreshProviderDiagnosticsUseCase: RefreshProviderDiagnosticsUseCase,
    private val stopGenerationUseCase: StopGenerationUseCase,
    private val stopAllGenerationsUseCase: StopAllGenerationsUseCase,
) : ViewModel() {
    private val provider = AiProvider.fromRouteValue(savedStateHandle[Routes.ArgProvider])
    private val host = Uri.decode(savedStateHandle[Routes.ArgHost] ?: "")
    private val port = savedStateHandle[Routes.ArgPort] ?: provider.defaultPort

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            provider = provider,
            host = host,
            port = port,
        ),
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveGenerationsUseCase().collect { generations ->
                _uiState.update {
                    it.copy(
                        activeGenerations = generations.map { generation ->
                            ActiveGenerationUi(
                                conversationId = generation.conversationId,
                                title = generation.title,
                                modelName = generation.modelName,
                                isArchived = generation.isArchived,
                                assistantMessageCreatedAtMillis = generation.assistantMessageCreatedAtMillis,
                            )
                        },
                    )
                }
            }
        }

        viewModelScope.launch {
            observeProviderDiagnosticsUseCase().collect { diagnostics ->
                _uiState.update {
                    it.copy(providerDiagnostics = diagnostics)
                }
            }
        }
    }

    fun refreshProviderDiagnostics() {
        if (_uiState.value.isRefreshingDiagnostics) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshingDiagnostics = true,
                    errorMessage = null,
                )
            }
            when (
                val result = refreshProviderDiagnosticsUseCase(
                    ConnectionConfig(
                        provider = provider,
                        host = host,
                        port = port,
                    ),
                )
            ) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isRefreshingDiagnostics = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshingDiagnostics = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    fun stopGeneration(conversationId: Long) {
        viewModelScope.launch {
            when (val result = stopGenerationUseCase(conversationId)) {
                is AppResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }

                is AppResult.Success -> {
                    _uiState.update { it.copy(errorMessage = null) }
                }
            }
        }
    }

    fun stopAllGenerations() {
        if (_uiState.value.isStoppingAll) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStoppingAll = true,
                    errorMessage = null,
                )
            }
            when (val result = stopAllGenerationsUseCase()) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isStoppingAll = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isStoppingAll = false,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }
}
