package com.example.justdo.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val senderId: String,
    val chatId: String,
    val timestamp: Long,
    val isFromMe: Boolean
)