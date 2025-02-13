package com.example.justdo.data.mapper

import com.example.justdo.data.database.entity.MessageEntity
import com.example.justdo.data.models.Message

fun Message.toEntity(chatId: String) = MessageEntity(
    id = id,
    content = content,
    senderId = "",
    chatId = chatId,
    timestamp = timestamp,
    isFromMe = isFromMe
)

fun MessageEntity.toMessage() = Message(
    id = id,
    content = content,
    timestamp = timestamp,
    isFromMe = isFromMe
)