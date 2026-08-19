package com.dpashko.localaiclient.presentation.dashboard

import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ProviderDiagnostics

/**
 * Immutable dashboard state for connected local AI work.
 */
data class DashboardUiState(
    val provider: AiProvider = AiProvider.OLLAMA,
    val host: String = "",
    val port: Int = AiProvider.OLLAMA.defaultPort,
    val activeGenerations: List<ActiveGenerationUi> = emptyList(),
    val providerDiagnostics: ProviderDiagnostics? = null,
    val isStoppingAll: Boolean = false,
    val isRefreshingDiagnostics: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Active generation row shown in the dashboard.
 */
data class ActiveGenerationUi(
    val conversationId: Long,
    val title: String,
    val modelName: String,
    val isArchived: Boolean,
    val assistantMessageCreatedAtMillis: Long,
)
