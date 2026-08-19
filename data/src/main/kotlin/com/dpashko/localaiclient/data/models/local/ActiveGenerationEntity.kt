package com.dpashko.localaiclient.data.models.local

/**
 * Projection for active assistant generation rows shown on the dashboard.
 */
data class ActiveGenerationEntity(
    val conversationId: Long,
    val title: String,
    val modelName: String,
    val isArchived: Boolean,
    val assistantMessageCreatedAtMillis: Long,
)
