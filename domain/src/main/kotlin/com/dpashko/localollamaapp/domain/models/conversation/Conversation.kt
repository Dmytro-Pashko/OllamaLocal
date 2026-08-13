package com.dpashko.localollamaapp.domain.models.conversation

data class Conversation(
    val id: Long,
    val title: String,
    val modelName: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
