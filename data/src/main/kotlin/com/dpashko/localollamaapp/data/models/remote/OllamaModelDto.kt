package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.Serializable

@Serializable
data class OllamaModelDto(
    val name: String,
    val model: String? = null,
    val size: Long? = null,
    val digest: String? = null,
    val details: OllamaModelDetailsDto? = null,
)
