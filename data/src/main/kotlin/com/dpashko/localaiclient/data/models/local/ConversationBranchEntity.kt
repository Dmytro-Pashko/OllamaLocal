package com.dpashko.localaiclient.data.models.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_branches",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conversationId"]),
    ],
)
data class ConversationBranchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val conversationId: Long,
    val title: String,
    val summary: String? = null,
    val summaryUntilMessageId: Long? = null,
    val summaryUpdatedAtMillis: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
