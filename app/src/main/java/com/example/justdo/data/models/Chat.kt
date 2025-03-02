package com.example.justdo.data.models

data class Chat(
    val id: String = "",
    var name: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    var avatarUrl: String? = null,
    var participants: List<String> = emptyList()
)