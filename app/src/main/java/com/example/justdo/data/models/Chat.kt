package com.example.justdo.data.models

data class Chat(
    val id: String = "",
    var name: String = "",
    val lastMessage: String = "",
    val timestamp: Any? = null,
    var avatarUrl: String? = null,
    var participants: List<String> = emptyList()
)