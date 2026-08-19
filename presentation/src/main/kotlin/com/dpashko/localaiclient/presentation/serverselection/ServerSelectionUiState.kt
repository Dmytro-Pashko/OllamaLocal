package com.dpashko.localaiclient.presentation.serverselection

import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset

/**
 * Immutable state for the saved server selection screen.
 */
data class ServerSelectionUiState(
    /** Locally saved server presets. */
    val presets: List<ConnectionPreset> = emptyList(),
    /** Preset currently being connected, if any. */
    val connectingPresetId: String? = null,
    /** Preset selected for deletion confirmation. */
    val deletingPresetCandidate: ConnectionPreset? = null,
    /** User-facing error text for preset actions. */
    val errorMessage: String? = null,
) {
    /** True while a saved server connection check is running. */
    val isConnecting: Boolean
        get() = connectingPresetId != null
}
