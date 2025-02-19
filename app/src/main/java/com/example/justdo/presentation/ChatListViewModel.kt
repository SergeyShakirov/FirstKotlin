package com.example.justdo.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.services.MessageHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChatListViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Заменяем предыдущее объявление на это
    var messageHandler: MessageHandler? = null
        private set  // Делаем сеттер приватным

    // Теперь используем эту функцию для установки handler's
    fun updateMessageHandler(handler: MessageHandler) {
        messageHandler = handler
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users // Исправлено имя публичного свойства

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _currentUser.map { user ->
        user?.chats?.sortByLastMessage() ?: emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun markMessageAsRead(chatId: String, messageId: String) {
        viewModelScope.launch {
            messageHandler?.markMessageAsRead(chatId, messageId)
        }
    }

    suspend fun getCurrentUser() {
        try {
            val userDoc = auth.currentUser?.let {
                firestore.collection("users")
                    .document(it.uid)
                    .get()
                    .await()
            }

            if (userDoc != null) {
                _currentUser.value = userDoc.toObject(User::class.java)
            }
        } catch (e: Exception) {
            Log.e("getCurrentUser", "Ошибка при получении текущего пользователя", e)
        }
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

    // Тогда функция станет более чистой:
    fun loadChats() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Получаем ID текущего пользователя
                val userId = _currentUser.value?.id ?: return@launch

                // Получаем актуальные данные пользователя из Firestore
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java)?.copy(id = userId)
                if (user != null) {
                    // Обновляем текущего пользователя
                    _currentUser.value = user

                    if (user.chats.isNotEmpty()) {
                        // Получаем обновленные данные чатов
                        val loadedChats = chatRepository.getUpdatedChats(user.chats)

                        // Находим новые чаты, сравнивая с текущим списком
                        val currentChatIds = _chats.value.map { it.id }.toSet()
                        val newChats = loadedChats.filter { it.id !in currentChatIds }

                        if (newChats.isNotEmpty()) {
                            messageHandler?.startListeningNewChats(newChats)
                        }

                        // Обновляем пользователя с актуальными чатами
                        _currentUser.value = user.copy(chats = loadedChats)
                    } else {
                        Log.d("ChatListViewModel", "У пользователя нет чатов")
                    }
                } else {
                    Log.e("ChatListViewModel", "Пользователь не найден в базе")
                }
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Ошибка загрузки чатов", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun checkAndAddChatToUsers(chat: Chat, selectedUser: User) {
        try {
            val currentUser = _currentUser.value ?: return
            val userRefs = listOf(
                firestore.collection("users").document(currentUser.id),
                firestore.collection("users").document(selectedUser.id)
            )

            // Получаем данные обоих пользователей
            val userDocs = userRefs.map { it.get().await() }
            val users = userDocs.map { it.toObject(User::class.java) }

            // Проверяем наличие чата у обоих пользователей
            val needsUpdate = users.map { user ->
                user?.chats?.none { it.id == chat.id } ?: true
            }

            if (needsUpdate.any { it }) {
                // Обновляем данные обоих пользователей
                userRefs.zip(users).forEachIndexed { index, (ref, userData) ->
                    if (needsUpdate[index]) {
                        // Создаем копию чата с правильным именем для каждого пользователя
                        val chatWithUpdatedName = chat.copy(
                            name = if (index == 0) selectedUser.username else currentUser.username
                        )

                        val updatedUser = userData?.copy(
                            chats = (userData.chats ?: emptyList()) + chatWithUpdatedName
                        ) ?: User(
                            id = if (index == 0) currentUser.id else selectedUser.id,
                            username = if (index == 0) currentUser.username else selectedUser.username,
                            email = if (index == 0) currentUser.email else selectedUser.email,
                            chats = listOf(chat)
                        )
                        ref.set(updatedUser).await()

                        // Обновляем currentUser в ViewModel если это текущий пользователь
                        if (index == 0) {
                            _currentUser.value = updatedUser
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Ошибка при добавлении чата", e)
            throw e
        }
    }

    // Вспомогательная функция для получения ID чата
    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    // Для удобства можно вынести логику сортировки в отдельную extension-функцию:
    private fun List<Chat>.sortByLastMessage(): List<Chat> {
        return sortedByDescending { chat ->
            when (chat.lastMessageTimestamp) {
                is com.google.firebase.Timestamp -> (chat.lastMessageTimestamp as com.google.firebase.Timestamp).seconds
                is Long -> chat.lastMessageTimestamp
                is Date -> (chat.lastMessageTimestamp as Date).time
                else -> 0L
            }
        }
    }

    fun logout() {
        // Явный сброс всех состояний
        _currentUser.value = null
        _chats.value = emptyList()
    }

    class Factory(private val chatRepository: ChatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
                return ChatListViewModel(chatRepository) as T
            }
            throw IllegalArgumentException("Неизвестный класс ViewModel")
        }
    }
}