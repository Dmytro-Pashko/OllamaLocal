package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.Serializable

@Serializable
data class LmStudioModelsResponseDto(
    val data: List<LmStudioModelDto> = emptyList(),
)

@Serializable
data class LmStudioModelDto(
    val id: String,
)
