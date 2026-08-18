package com.dpashko.localaiclient.data.models.remote

import kotlinx.serialization.Serializable

/**
 * Model list response returned by LM Studio.
 */
@Serializable
data class LmStudioModelsResponseDto(
    /** Available model descriptors. */
    val data: List<LmStudioModelDto> = emptyList(),
)

/**
 * LM Studio model descriptor.
 */
@Serializable
data class LmStudioModelDto(
    /** Model id used in chat completion requests. */
    val id: String,
)
