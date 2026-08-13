package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.Serializable

@Serializable
data class OllamaTagsResponseDto(
    val models: List<OllamaModelDto> = emptyList(),
)
