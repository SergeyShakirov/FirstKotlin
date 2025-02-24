package com.example.justdo.data.models

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    var lastMessage: String = "",
    var chats: List<Chat> = emptyList(),
    val avatarUrl: String? = null,
    val fcmToken: String? = null
)