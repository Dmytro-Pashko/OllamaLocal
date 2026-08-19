package com.dpashko.localaiclient.domain.models.storage

/**
 * Local-only storage and privacy counters shown on the dashboard.
 */
data class StoragePrivacyStats(
    val activeConversationCount: Int,
    val archivedConversationCount: Int,
    val messageCount: Int,
    val activeGenerationCount: Int,
    val databaseSizeBytes: Long,
)
