package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiErrorDto(
    val error: OpenAiErrorBodyDto? = null,
)

@Serializable
data class OpenAiErrorBodyDto(
    val message: String? = null,
)
