package com.dpashko.localaiclient.presentation.ui.models

import com.dpashko.localaiclient.domain.models.ai.AiModel

/**
 * Model option prepared for rendering in the connection screen.
 */
data class AiModelUi(
    /** Model name shown and passed back to generation requests. */
    val name: String,
    /** Optional compact details line such as parameter size and quantization. */
    val detailsText: String?,
)

/**
 * Maps a domain model to display text without leaking provider DTOs into UI code.
 */
fun AiModel.toUi(): AiModelUi =
    AiModelUi(
        name = name,
        detailsText = listOfNotNull(parameterSize, quantizationLevel)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " • "),
    )
