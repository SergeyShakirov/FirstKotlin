package com.example.justdo.data

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}