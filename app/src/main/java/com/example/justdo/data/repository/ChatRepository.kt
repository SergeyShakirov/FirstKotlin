package com.example.justdo.data.repository

import android.util.Log
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.Message
import com.example.justdo.data.models.User
import com.example.justdo.presentation.ChatListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ChatRepository"
    private var updatedUserData: User? = null

    // Подписка на сообщения в чате
    fun subscribeToMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        // Добавьте проверку входных данных
        if (chatId.isBlank()) {
            Log.e(TAG, "Chat ID не может быть пустым")
            close(IllegalArgumentException("Chat ID не может быть пустым"))
            return@callbackFlow
        }

        val messagesRef = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Ошибка при прослушивании сообщений", error)
                // Используйте close() для завершения потока с ошибкой
                close(error)
                return@addSnapshotListener
            }

            // Расширенная обработка снимка
            val messages = snapshot?.documents?.mapNotNull { doc ->
                try {
                    // Явное преобразование timestamp
                    val message = doc.toObject(Message::class.java)
                    message?.copy(
                        id = doc.id,
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при конвертации документа сообщения", e)
                    null
                }
            } ?: emptyList()

            // Безопасная отправка данных
            trySend(messages.sortedByDescending { it.timestamp as Comparable<Any> })
        }

        awaitClose {
            listener.remove()
            Log.d(TAG, "Прослушивание сообщений для чата $chatId остановлено")
        }
    }

    suspend fun sendMessage(chat: Chat, text: String, viewModel: ChatListViewModel) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("Пользователь не аутентифицирован")

            val timestamp = com.google.firebase.Timestamp.now()

            val message = hashMapOf(
                "senderId" to currentUser.uid,
                "text" to text,
                "timestamp" to timestamp,
                "isRead" to false
            )

            val otherUserId = if (chat.id.startsWith(currentUser.uid)) {
                chat.id.substringAfter("_")
            } else {
                chat.id.substringBefore("_")
            }

            val chatRef = firestore.collection("chats").document(chat.id)
            val currentUserRef = firestore.collection("users").document(currentUser.uid)
            val otherUserRef = firestore.collection("users").document(otherUserId)

            firestore.runTransaction { transaction ->
                // 1. Чтение всех необходимых документов
                val chatDoc = transaction.get(chatRef)
                val currentUserDoc = transaction.get(currentUserRef)
                val otherUserDoc = transaction.get(otherUserRef)

                // 2. Подготовка данных
                val chatExists = chatDoc.exists()
                val currentUserData = currentUserDoc.toObject(User::class.java)
                val otherUserData = otherUserDoc.toObject(User::class.java)

                // Создаем обновленный объект чата
                val updatedChat = Chat(
                    id = chat.id,
                    lastMessage = text,
                    lastMessageTimestamp = timestamp
                )

                // 3. Операции записи
                if (!chatExists) {
                    transaction.set(chatRef, hashMapOf(
                        "id" to chat.id,
                        "lastMessage" to text,
                        "lastMessageTimestamp" to timestamp,
                        "participants" to listOf(currentUser.uid, otherUserId)
                    ))

                    // Обновляем текущего пользователя
                    if (currentUserData != null) {
                        updatedChat.name = otherUserData?.username ?: ""
                        val currentUserChats = currentUserData.chats.filter { it.id != chat.id } + updatedChat
                        val updatedCurrentUser = currentUserData.copy(
                            chats = currentUserChats,
                            lastMessage = text
                        )
                        transaction.set(currentUserRef, updatedCurrentUser)
                        updatedUserData = updatedCurrentUser
                    }

                    // Обновляем другого пользователя
                    if (otherUserData != null) {
                        updatedChat.name = currentUserData?.username ?: ""
                        val otherUserChats = otherUserData.chats.filter { it.id != chat.id } + updatedChat
                        transaction.set(otherUserRef, otherUserData.copy(
                            chats = otherUserChats,
                            lastMessage = text  // Обновляем lastMessage
                        ))
                    }
                } else {
                    // Обновляем текущего пользователя
                    if (currentUserData != null) {
                        updatedChat.name = otherUserData?.username ?: ""
                        val currentUserChats = currentUserData.chats.map {
                            if (it.id == chat.id) updatedChat else it
                        }
                        val updatedCurrentUser = currentUserData.copy(
                            chats = currentUserChats,
                            lastMessage = text
                        )
                        transaction.set(currentUserRef, updatedCurrentUser)
                        updatedUserData = updatedCurrentUser
                    }

                    // Обновляем другого пользователя
                    if (otherUserData != null) {
                        updatedChat.name = currentUserData?.username ?: ""
                        val otherUserChats = otherUserData.chats.map {
                            if (it.id == chat.id) updatedChat else it
                        }
                        transaction.set(otherUserRef, otherUserData.copy(
                            chats = otherUserChats,
                            lastMessage = text  // Обновляем lastMessage
                        ))
                    }
                }

                // Добавляем сообщение
                val messageRef = chatRef.collection("messages").document()
                transaction.set(messageRef, message)

                // Обновляем последнее сообщение и timestamp в чате
                transaction.update(chatRef,
                    "lastMessage", text,
                    "lastMessageTimestamp", timestamp
                )
            }.await()

            // После транзакции обновляем ViewModel
            updatedUserData?.let { user ->
                viewModel.setCurrentUser(user)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отправке сообщения", e)
            throw e
        }
    }

    // Получение списка чатов пользователя
    fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        try {
            val listener = firestore.collection("users")
                .document(userId)
                .addSnapshotListener { userSnapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Ошибка при прослушивании чатов пользователя", error)
                        return@addSnapshotListener
                    }

                    val user = userSnapshot?.toObject(User::class.java)
                    val chats = user?.chats ?: emptyList()
                    trySend(chats)
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в getUserChats", e)
            trySend(emptyList())
            close(e)
        }
    }

    // Получение информации о конкретном чате
    suspend fun getUpdatedChats(chats: List<Chat>): List<Chat> {
        return try {
            // Создаем мапу имен из исходных чатов для быстрого доступа
            val chatNames = chats.associateBy({ it.id }, { it.name })

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
                            name = chatNames[doc.id] ?: chat.name // Используем имя из исходного чата или оставляем текущее
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

    // Обновление списка чатов пользователя
    private suspend fun updateUserChats(userId: String, chat: Chat) {
        val userRef = firestore.collection("users").document(userId)

        try {
            firestore.runTransaction { transaction ->
                val userDoc = transaction.get(userRef)
                val currentChats = userDoc.toObject(User::class.java)?.chats ?: emptyList()

                if (!currentChats.any { it.id == chat.id }) {
                    transaction.update(userRef, "chats", currentChats + chat)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении чатов пользователя $userId", e)
            throw e
        }
    }
}