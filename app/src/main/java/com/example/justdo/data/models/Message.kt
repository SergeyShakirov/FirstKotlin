package com.example.justdo.data.models

data class Message(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean
)