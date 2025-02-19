package com.example.justdo.data.models

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val text: String = "",
    val timestamp: Any? = null,
    val isRead: Boolean = false,
    val chatId: String = ""
)