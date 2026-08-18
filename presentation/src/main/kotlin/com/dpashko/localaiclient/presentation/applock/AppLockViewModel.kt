package com.dpashko.localaiclient.presentation.applock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.usecases.ObserveSecuritySettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates the optional process-level app lock state.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    observeSecuritySettingsUseCase: ObserveSecuritySettingsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSecuritySettingsUseCase().collect { settings ->
                _uiState.update { state ->
                    val shouldRequestUnlock = settings.appLockEnabled && !state.isLockRequired
                    state.copy(
                        isLoading = false,
                        isLockRequired = settings.appLockEnabled,
                        isUnlocked = if (settings.appLockEnabled) state.isUnlocked else true,
                        unlockRequestNonce = if (shouldRequestUnlock) {
                            state.unlockRequestNonce + 1
                        } else {
                            state.unlockRequestNonce
                        },
                        errorMessage = null,
                    )
                }
            }
        }
    }

    /**
     * Requests Android device unlock from the UI.
     */
    fun requestUnlock() {
        _uiState.update {
            it.copy(
                unlockRequestNonce = it.unlockRequestNonce + 1,
                errorMessage = null,
            )
        }
    }

    /**
     * Marks the current process as unlocked.
     */
    fun onUnlockSucceeded() {
        _uiState.update {
            it.copy(
                isUnlocked = true,
                errorMessage = null,
            )
        }
    }

    /**
     * Keeps private content hidden after cancellation or failed authentication.
     */
    fun onUnlockFailed(message: String) {
        _uiState.update {
            it.copy(
                isUnlocked = false,
                errorMessage = message,
            )
        }
    }
}
