package com.dpashko.localollamaapp.presentation.ui.models

import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.conversation.MessageRole
import com.dpashko.localollamaapp.presentation.common.toMessageTimeText

data class MessageUi(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val createdAtText: String,
)

fun Message.toUi(): MessageUi =
    MessageUi(
        id = id,
        role = role,
        content = content,
        createdAtText = createdAtMillis.toMessageTimeText(),
    )
