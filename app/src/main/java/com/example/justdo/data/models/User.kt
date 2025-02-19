package com.example.justdo.data.models

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    var lastMessage: String = "",
    var chats: List<Chat> = emptyList()
)