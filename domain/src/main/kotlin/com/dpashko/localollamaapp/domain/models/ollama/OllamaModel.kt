package com.dpashko.localollamaapp.domain.models.ollama

data class OllamaModel(
    val name: String,
    val parameterSize: String?,
    val quantizationLevel: String?,
)
