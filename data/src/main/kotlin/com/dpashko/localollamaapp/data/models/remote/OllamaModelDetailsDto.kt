package com.dpashko.localollamaapp.data.models.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaModelDetailsDto(
    @SerialName("parameter_size")
    val parameterSize: String? = null,
    @SerialName("quantization_level")
    val quantizationLevel: String? = null,
)
