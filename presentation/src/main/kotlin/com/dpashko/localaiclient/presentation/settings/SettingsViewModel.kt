package com.dpashko.localaiclient.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings
import com.dpashko.localaiclient.domain.models.settings.SecuritySettings
import com.dpashko.localaiclient.domain.usecases.ObserveGenerationSettingsUseCase
import com.dpashko.localaiclient.domain.usecases.ObserveSecuritySettingsUseCase
import com.dpashko.localaiclient.domain.usecases.SaveGenerationSettingsUseCase
import com.dpashko.localaiclient.domain.usecases.SaveSecuritySettingsUseCase
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
 * Manages editable generation timeout settings.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeGenerationSettingsUseCase: ObserveGenerationSettingsUseCase,
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
    private val saveGenerationSettingsUseCase: SaveGenerationSettingsUseCase,
    private val saveSecuritySettingsUseCase: SaveSecuritySettingsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        // Keep the draft synchronized with persisted settings when the screen opens or updates.
        viewModelScope.launch {
            observeGenerationSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        timeoutMinutesText = settings.generationTimeoutMinutes.toString(),
                        errorMessage = null,
                    )
                }
            }
        }

        viewModelScope.launch {
            observeSecuritySettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        appLockEnabled = settings.appLockEnabled,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    /**
     * Updates the timeout draft while preserving numeric-only input.
     */
    fun onTimeoutMinutesChanged(value: String) {
        if (value.all { it.isDigit() }) {
            _uiState.update {
                it.copy(
                    timeoutMinutesText = value,
                    errorMessage = null,
                )
            }
        }
    }

    /**
     * Updates the app-lock draft without applying the lock gate immediately.
     */
    fun onAppLockEnabledChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                appLockEnabled = enabled,
                errorMessage = null,
            )
        }
    }

    /**
     * Replaces the draft timeout with the default value without saving yet.
     */
    fun resetDraftToDefault() {
        _uiState.update {
            it.copy(
                timeoutMinutesText = GenerationSettings.Default.generationTimeoutMinutes.toString(),
                appLockEnabled = SecuritySettings.Default.appLockEnabled,
                errorMessage = null,
            )
        }
    }

    /**
     * Validates and persists the current timeout draft.
     */
    fun apply() {
        val minutes = _uiState.value.timeoutMinutesText.toIntOrNull()
        if (minutes == null || !GenerationSettings.isValidMinutes(minutes)) {
            _uiState.update {
                it.copy(errorMessage = "Enter a timeout from 1 to 1440 minutes.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isApplying = true,
                    errorMessage = null,
                )
            }

            when (val result = saveGenerationSettingsUseCase(GenerationSettings.fromMinutes(minutes))) {
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            errorMessage = result.error.toUserMessage(),
                        )
                    }
                }

                is AppResult.Success -> saveSecuritySettings()
            }
        }
    }

    private suspend fun saveSecuritySettings() {
        when (
            val result = saveSecuritySettingsUseCase(
                SecuritySettings(appLockEnabled = _uiState.value.appLockEnabled),
            )
        ) {
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }

            is AppResult.Success -> {
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        errorMessage = null,
                    )
                }
                _events.emit(SettingsEvent.Applied)
            }
        }
    }
}

/**
 * One-shot settings events that should not be replayed as persistent UI state.
 */
sealed interface SettingsEvent {
    /** Emitted after settings are successfully saved. */
    data object Applied : SettingsEvent
}
