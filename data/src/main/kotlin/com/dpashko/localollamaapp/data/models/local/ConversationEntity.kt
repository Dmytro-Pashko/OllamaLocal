package com.dpashko.localollamaapp.data.models.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val modelName: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
