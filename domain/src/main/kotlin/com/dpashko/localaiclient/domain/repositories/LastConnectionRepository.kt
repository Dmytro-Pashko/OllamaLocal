package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.LastConnection
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for the last successful local provider connection.
 */
interface LastConnectionRepository {
    /**
     * Observes the last successful connection, or null when none exists.
     */
    fun observeLastConnection(): Flow<LastConnection?>

    /**
     * Persists [connection] after provider reachability and model loading succeed.
     */
    suspend fun saveLastConnection(connection: LastConnection): AppResult<Unit>
}
