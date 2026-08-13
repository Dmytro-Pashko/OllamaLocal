package com.dpashko.localollamaapp.presentation.ui.models

import com.dpashko.localollamaapp.domain.models.conversation.Conversation
import com.dpashko.localollamaapp.presentation.common.toConversationTimeText

data class ConversationUi(
    val id: Long,
    val title: String,
    val modelName: String,
    val updatedAtText: String,
)

fun Conversation.toUi(): ConversationUi =
    ConversationUi(
        id = id,
        title = title,
        modelName = modelName,
        updatedAtText = updatedAtMillis.toConversationTimeText(),
    )
