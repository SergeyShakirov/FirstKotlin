package com.example.justdo.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.Message
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.data.repository.FirebaseStorageService
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.data.models.UploadState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storageService = FirebaseStorageService()
    // Состояние загрузки аватарки
    val uploadState: StateFlow<UploadState> = storageService.uploadState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = userRepository.currentUser

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    // Список URL аватарок пользователя
    private val _userAvatars = MutableStateFlow<List<String>>(emptyList())
    val userAvatars = _userAvatars.asStateFlow()

    // Состояние для обновления имени пользователя
    private val _isUpdatingUsername = MutableStateFlow(false)
    val isUpdatingUsername = _isUpdatingUsername.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                getCurrentUser()
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Ошибка при инициализации", e)
            }
        }
    }

    fun startMessageListener(chatId: String) {
        viewModelScope.launch {
            chatRepository.listenToMessages(chatId) { newMessages ->
                _messages.value = newMessages
            }
        }
    }

    fun markMessageAsRead(chatId: String, messageId: String) {
        viewModelScope.launch {
            chatRepository.markMessageAsRead(chatId, messageId)
        }
    }

    fun loadUserChats(currentUserId: String) {
        viewModelScope.launch {
            try {
                val userChats = getUserChats(currentUserId)
                _chats.value = userChats
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading user chats", e)
                _chats.value = emptyList()
            }
        }
    }

    fun onSendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val currentChat = _currentChat.value
                val currentUser = _currentUser.value

                if (currentChat != null && currentUser != null) {
                    chatRepository.sendMessageNew(currentChat.id, currentUser.id, text)
                    chatRepository.updateLastMessage(currentChat.id, text)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                // Обработка ошибки
            }
        }
    }

    private suspend fun getUserChats(currentUserId: String): List<Chat> {
        return try {
            val chatsSnapshot = firestore.collection("newChats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            chatsSnapshot.documents.mapNotNull { doc ->
                try {
                    val participants = doc.get("participants") as? List<String> ?: return@mapNotNull null

                    // Находим ID второго участника (не текущего пользователя)
                    val otherUserId = participants.find { it != currentUserId } ?: return@mapNotNull null

                    // Получаем данные второго участника
                    val otherUser = firestore.collection("users")
                        .document(otherUserId)
                        .get()
                        .await()

                    val timestamp = when (val timestampField = doc.get("timestamp")) {
                        is Long -> timestampField
                        is com.google.firebase.Timestamp -> timestampField.seconds * 1000 + timestampField.nanoseconds / 1000000
                        else -> System.currentTimeMillis() // Fallback
                    }

                    Chat(
                        id = doc.get("id") as String,
                        participants = participants,
                        lastMessage = doc.get("lastMessage") as String? ?: "",
                        timestamp = timestamp,
                        name = otherUser.getString("username") ?: "",
                        avatarUrl = otherUser.getString("avatarUrl") ?: null
                    )
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error mapping chat document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error getting user chats", e)
            emptyList()
        }
    }

    suspend fun getChat(userId: String): Chat? {
        return try {
            // Проверяем оба возможных варианта id чата
            val chatId1 = "${currentUser.value?.id}_${userId}"
            val chatId2 = "${userId}_${currentUser.value?.id}"

            // Пробуем найти чат по первому варианту id
            var doc = firestore.collection("newChats")
                .document(chatId1)
                .get()
                .await()

            // Если не нашли, пробуем второй вариант
            if (!doc.exists()) {
                doc = firestore.collection("newChats")
                    .document(chatId2)
                    .get()
                    .await()
            }

            if (doc.exists()) {
                Chat(
                    id = doc.get("id") as String,
                    participants = doc.get("participants") as List<String>,
                    lastMessage = doc.get("lastMessage") as String? ?: "",
                    timestamp = doc.get("timestamp") as Long
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error getting chat", e)
            null
        }
    }

    /**
     * Обновляет объект Chat, добавляя имя и аватарку пользователя из Firestore
     *
     * @param chat Объект чата, который нужно обновить
     * @param currentUserId ID текущего пользователя
     * @return Обновленный объект Chat с именем и аватаркой
     */
    suspend fun updateChatWithUserData(chat: Chat): Chat {
        return try {
            // Находим ID другого участника (не текущего пользователя)
            val otherUserId = chat.participants.firstOrNull { it != currentUser.value?.id } ?: return chat

            // Получаем данные другого участника из Firestore
            val otherUserDoc = firestore.collection("users")
                .document(otherUserId)
                .get()
                .await()

            if (otherUserDoc.exists()) {
                // Копируем объект чата с обновленными данными
                chat.copy(
                    name = otherUserDoc.getString("username") ?: "",
                    avatarUrl = otherUserDoc.getString("avatarUrl")
                )
            } else {
                // Если пользователь не найден, возвращаем исходный чат
                chat
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error updating chat with user data", e)
            // В случае ошибки возвращаем исходный чат
            chat
        }
    }

    suspend fun createChat(userId: String): Chat? {
        return try {
            val chatId = "${currentUser.value?.id}_${userId}"

            // Создаем новый чат
            val newChat = hashMapOf(
                "id" to chatId,
                "lastMessage" to "",
                "timestamp" to Timestamp.now(),
                "participants" to listOf(currentUser.value?.id, userId)
            )

            // Добавляем чат в коллекцию newChats
            firestore.collection("newChats")
                .document(chatId)
                .set(newChat)
                .await()

            Chat(
                id = chatId,
                participants = listOf(currentUser.value?.id, userId),
                lastMessage = "",
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error creating chat", e)
            null
        }
    }

    /**
     * Загружает все аватарки пользователя из Firebase Storage
     */
    fun loadUserAvatars(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val avatars = storageService.getUserAvatars(userId)
                _userAvatars.value = avatars
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Ошибка при загрузке аватарок", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getCurrentUser() {
        _currentUser.value = userRepository.getCurrentUser()
    }

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    fun setCurrentChat(chat: Chat) {
        _currentChat.value = chat
    }

    fun setIsLoading(value: Boolean) {
        _isLoading.value = value
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _currentUser.value?.id.let {
                _users.value = it?.let { it1 -> fetchUsers(it1) }!!
            }
            _isLoading.value = false
        }
    }

    private suspend fun fetchUsers(currentUserId: String): List<User> {
        return try {
            val usersSnapshot = firestore.collection("users")
                .get()
                .await()

            usersSnapshot.documents
                .mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }
                .filter { it.id != currentUserId } // Исключаем текущего пользователя
        } catch (e: Exception) {
            Log.e("ChatListViewModel", "Ошибка получения пользователей", e)
            emptyList()
        }
    }

    fun logout() {
        _currentUser.value = null
        _chats.value = emptyList()
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
                return ChatListViewModel(chatRepository, userRepository) as T
            }
            throw IllegalArgumentException("Неизвестный класс ViewModel")
        }
    }
}