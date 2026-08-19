package com.dpashko.localaiclient.presentation.serverselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpashko.localaiclient.domain.usecases.ObserveConnectionPresetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads saved local server presets for the entry screen.
 */
@HiltViewModel
class ServerSelectionViewModel @Inject constructor(
    observeConnectionPresetsUseCase: ObserveConnectionPresetsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServerSelectionUiState())
    val uiState: StateFlow<ServerSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeConnectionPresetsUseCase().collect { presets ->
                _uiState.update { it.copy(presets = presets) }
            }
        }
    }
}
