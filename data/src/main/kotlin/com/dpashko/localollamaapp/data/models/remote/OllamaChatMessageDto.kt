package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatMessageDto(
    val role: String,
    val content: String,
)
