package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatResponseDto(
    val model: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val message: OllamaChatMessageDto? = null,
    val done: Boolean = true,
    @SerialName("done_reason")
    val doneReason: String? = null,
)
