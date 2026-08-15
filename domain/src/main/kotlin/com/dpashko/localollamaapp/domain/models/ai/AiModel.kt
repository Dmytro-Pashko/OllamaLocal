package com.dpashko.localollamaapp.domain.models.ai

data class AiModel(
    val name: String,
    val parameterSize: String?,
    val quantizationLevel: String?,
)
