package com.example.justdo.data.models

data class Chat(
    val id: String = "",
    var name: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Any? = null
)