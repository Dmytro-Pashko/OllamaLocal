package com.dpashko.localollamaapp.presentation.ui.models

import com.dpashko.localollamaapp.domain.models.ai.AiModel

data class AiModelUi(
    val name: String,
    val detailsText: String?,
)

fun AiModel.toUi(): AiModelUi =
    AiModelUi(
        name = name,
        detailsText = listOfNotNull(parameterSize, quantizationLevel)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " • "),
    )
