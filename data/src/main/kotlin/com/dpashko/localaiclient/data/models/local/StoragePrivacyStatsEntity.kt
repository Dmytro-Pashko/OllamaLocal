package com.dpashko.localaiclient.data.models.local

/**
 * Aggregate local storage counters loaded from Room.
 */
data class StoragePrivacyStatsEntity(
    val activeConversationCount: Int,
    val archivedConversationCount: Int,
    val messageCount: Int,
    val activeGenerationCount: Int,
)
