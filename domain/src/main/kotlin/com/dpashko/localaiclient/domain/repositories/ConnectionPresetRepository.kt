package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionPreset
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for local connection presets.
 */
interface ConnectionPresetRepository {
    /**
     * Observes saved presets ordered for display.
     */
    fun observeConnectionPresets(): Flow<List<ConnectionPreset>>

    /**
     * Saves or updates [preset] in local private storage.
     */
    suspend fun saveConnectionPreset(preset: ConnectionPreset): AppResult<Unit>

    /**
     * Deletes a local preset by id.
     */
    suspend fun deleteConnectionPreset(presetId: String): AppResult<Unit>
}
