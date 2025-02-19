package com.example.justdo.services

import android.content.Context
import android.util.Log
import com.example.justdo.data.models.Chat
import com.example.justdo.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MessageHandler(
    private val context: Context,
    private val chats: List<Chat>,
    private val currentUserId: String
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationHelper = NotificationHelper(context)
    private var messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateChats(newChats: List<Chat>) {
        startListening() // Обновит слушатели для новых чатов
    }

    fun startListening(chatId: String = "") {
        // Устанавливаем слушатели только для переданных чатов
        chats.forEach { chat ->
            if (!messageListeners.containsKey(chat.id) && chat.id != chatId) {
                setupChatListener(chat.id, chat.name)
            }
        }
        Log.d(TAG, "Установлены слушатели для чатов: ${chats.map { it.id }}")
    }

    private fun setupChatListener(chatId: String, name: String) {
        //val chat = chats.find { it.id == chatId } ?: return // Находим чат из списка

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Ошибка при прослушивании сообщений чата $chatId", error)
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                val messageDoc = snapshots.documents.firstOrNull() ?: return@addSnapshotListener

                val text = messageDoc.getString("text")
                val senderId = messageDoc.getString("senderId")
                val isRead = messageDoc.getBoolean("isRead") ?: true

                if (text != null &&
                    senderId != null &&
                    senderId != currentUserId &&
                    !isRead
                ) {
                    // Используем имя из локального чата
                    notificationHelper.showMessageNotification(
                        senderId = senderId,
                        senderName = name, // Берем имя прямо из чата
                        message = text,
                        chatId = chatId
                    )
                }
            }

        messageListeners[chatId] = listener
        Log.d(TAG, "Установлен слушатель для чата: $chatId")
    }

    fun startListeningNewChats(newChats: List<Chat>) {
        newChats.forEach { chat ->
            if (!messageListeners.containsKey(chat.id)) {
                setupChatListener(chat.id, chat.name)
            }
        }
    }

    // Функция для отметки сообщения как прочитанного
    suspend fun markMessageAsRead(chatId: String, messageId: String) {
        try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отметке сообщения как прочитанного", e)
        }
    }

    // Функция для массовой отметки сообщений как прочитанных
    suspend fun markAllMessagesAsRead(chatId: String) {
        try {
            val unreadMessages = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", currentUserId)
                .get()
                .await()

            unreadMessages.documents.forEach { doc ->
                markMessageAsRead(chatId, doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отметке всех сообщений как прочитанных", e)
        }
    }

    // Функция для обновления последнего сообщения в чате
    suspend fun updateLastMessage(chatId: String, message: String, senderId: String) {
        try {
            firestore.collection("chats")
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessageSenderId" to senderId,
                        "lastMessageTimestamp" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении последнего сообщения", e)
        }
    }

    // Функция для получения непрочитанных сообщений
    suspend fun getUnreadMessagesCount(chatId: String): Int {
        return try {
            val unreadMessages = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", currentUserId)
                .get()
                .await()

            unreadMessages.size()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении количества непрочитанных сообщений", e)
            0
        }
    }

    // Функция для отслеживания состояния онлайн другого пользователя
    fun listenToUserOnlineStatus(userId: String, onStatusChanged: (Boolean) -> Unit) {
        val listener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Ошибка при отслеживании статуса пользователя", error)
                    return@addSnapshotListener
                }

                val isOnline = snapshot?.getBoolean("isOnline") ?: false
                onStatusChanged(isOnline)
            }

        messageListeners["user_$userId"] = listener
    }

    // Функция для обновления своего статуса онлайн
    suspend fun updateOnlineStatus(isOnline: Boolean) {
        try {
            firestore.collection("users")
                .document(currentUserId)
                .update("isOnline", isOnline)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении статуса онлайн", e)
        }
    }

    // Функция для получения времени последнего сообщения
    suspend fun getLastMessageTime(chatId: String): com.google.firebase.Timestamp? {
        return try {
            val chat = firestore.collection("chats")
                .document(chatId)
                .get()
                .await()

            chat.getTimestamp("lastMessageTimestamp")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении времени последнего сообщения", e)
            null
        }
    }

    // Переопределяем stopListening для корректной очистки всех ресурсов
    fun stopListening() {
        scope.launch {
            try {
                // Обновляем статус оффлайн перед выходом
                updateOnlineStatus(false)

                // Отписываемся от всех слушателей
                messageListeners.values.forEach { it.remove() }
                messageListeners.clear()

                // Отменяем все корутины
                scope.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при остановке слушателей", e)
            }
        }
    }

    // В MessageHandler добавляем методы
    fun stopListeningChat(chatId: String) {
        messageListeners[chatId]?.let { listener ->
            listener.remove()
            messageListeners.remove(chatId)
            Log.d(TAG, "Остановлена прослушка чата: $chatId")
        }
    }

    fun startListeningChat(chat: Chat) {
        if (!messageListeners.containsKey(chat.id)) {
            setupChatListener(chat.id, chat.name)
            Log.d(TAG, "Запущена прослушка чата: ${chat.id}")
        }
    }

    companion object {
        private const val TAG = "MessageHandler"
    }
}