package com.dpashko.localollamaapp.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val messageTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val conversationTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

fun Long.toMessageTimeText(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(messageTimeFormatter)

fun Long.toConversationTimeText(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(conversationTimeFormatter)
