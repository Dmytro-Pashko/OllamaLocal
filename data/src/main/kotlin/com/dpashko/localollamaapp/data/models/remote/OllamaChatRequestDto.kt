package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatRequestDto(
    val model: String,
    val messages: List<OllamaChatMessageDto>,
    val stream: Boolean,
)
