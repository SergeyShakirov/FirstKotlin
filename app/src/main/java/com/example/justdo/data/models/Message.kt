package com.example.justdo.data.models

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val text: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val chatId: String = "",
    val notificationSent: Boolean = false
)