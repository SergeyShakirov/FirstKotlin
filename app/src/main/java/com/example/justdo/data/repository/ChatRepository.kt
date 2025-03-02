package com.example.justdo.data.repository

import android.util.Log
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.Message
import com.example.justdo.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ChatRepository"
    private var updatedUserData: User? = null

    fun listenToMessages(chatId: String, onNewMessages: (List<Message>) -> Unit) {
        try {
            firestore.collection("messages")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ChatViewModel", "Error listening to messages", e)
                        return@addSnapshotListener
                    }

                    val messages = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            Message(
                                id = doc.id,
                                senderId = doc.getString("senderId") ?: return@mapNotNull null,
                                text = doc.getString("text") ?: "",
                                // Конвертируем Timestamp в Long миллисекунды
                                timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: return@mapNotNull null,
                                isRead = doc.getBoolean("isRead") ?: false
                            )
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Error mapping message", e)
                            null
                        }
                    } ?: emptyList()

                    onNewMessages(messages)
                }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error setting up message listener", e)
        }
    }

    suspend fun markMessageAsRead(chatId: String, messageId: String) {
        try {
            firestore.collection("messages")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Ошибка при отметке сообщения как прочитанного", e)
        }
    }

    suspend fun updateLastMessage(chatId: String, text: String) {
        try {
            firestore.collection("newChats")
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to text,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error updating last message", e)
            throw e
        }
    }

    suspend fun sendMessageNew(chatId: String, senderId: String, text: String) {
        try {
            // Создаем новое сообщение
            val message = mapOf(
                "senderId" to senderId,
                "text" to text,
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to false,
                "notificationSent" to false
            )

            // Добавляем сообщение в подколлекцию messages
            firestore.collection("messages")
                .document(chatId)
                .collection("messages")
                .add(message)
                .await()

        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error sending message", e)
            throw e
        }
    }

    suspend fun getUpdatedChats(chats: List<Chat>): List<Chat> {
        return try {
            // Создаем мапу имен из исходных чатов для быстрого доступа
            val chatNames = chats.associateBy({ it.id }, { it.name })
            val chatAvatars = chats.associateBy({ it.id }, { it.avatarUrl })

            val chatIds = chats.map { it.id }
            val batches = chatIds.chunked(10)

            val updatedChats = batches.flatMap { batchIds ->
                val batchQuery = firestore.collection("chats")
                    .whereIn(FieldPath.documentId(), batchIds)
                    .get()
                    .await()

                // При преобразовании добавляем имя из исходного чата
                batchQuery.documents.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.let { chat ->
                        chat.copy(
                            id = doc.id,
                            name = chatNames[doc.id] ?: chat.name, // Используем имя из исходного чата или оставляем текущее
                            avatarUrl = chatAvatars[doc.id] ?: ""
                        )
                    }
                }
            }

            val updatedChatIds = updatedChats.map { it.id }.toSet()
            val notFoundChats = chats.filter { it.id !in updatedChatIds }

            updatedChats + notFoundChats

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при пакетном получении чатов", e)
            chats
        }
    }
}