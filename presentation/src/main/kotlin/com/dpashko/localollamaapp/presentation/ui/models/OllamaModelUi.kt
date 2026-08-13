package com.dpashko.localollamaapp.presentation.ui.models

import com.dpashko.localollamaapp.domain.models.ollama.OllamaModel

data class OllamaModelUi(
    val name: String,
    val detailsText: String?,
)

fun OllamaModel.toUi(): OllamaModelUi =
    OllamaModelUi(
        name = name,
        detailsText = listOfNotNull(parameterSize, quantizationLevel)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " • "),
    )
