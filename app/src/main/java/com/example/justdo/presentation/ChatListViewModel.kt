package com.example.justdo.presentation

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.domain.models.UploadState
import com.example.justdo.network.api.ImgurApi
import com.example.justdo.network.constants.ImgurConfig
import com.example.justdo.services.MessageHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedInputStream
import java.io.InputStream

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Состояние загрузки аватара
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    // Заменяем предыдущее объявление на это
    var messageHandler: MessageHandler? = null
        private set  // Делаем сеттер приватным

    // Добавляем Imgur API
    private val imgurApi: ImgurApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Accept", "application/json")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.imgur.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImgurApi::class.java)
    }

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

    // Обновляем функцию uploadAvatar для использования Imgur
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading

            try {
                // Получаем context из Firebase Auth
                val context = auth.app.applicationContext

                // Читаем файл и конвертируем в ByteArray
                val byteArray = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: throw IOException("Не удалось открыть файл")

                // Создаем RequestBody из ByteArray
                val requestBody = okhttp3.RequestBody.create(
                    "image/*".toMediaType(),
                    byteArray
                )

                // Создаем MultipartBody.Part
                val body = MultipartBody.Part.createFormData(
                    "image",
                    "photo.jpg",
                    requestBody
                )

                // Загружаем на Imgur
                val response = imgurApi.uploadImage(
                    "Client-ID ${ImgurConfig.CLIENT_ID}",
                    body
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val imageUrl = response.body()?.data?.link
                        ?: throw IOException("Нет ссылки на изображение")

                    // Обновляем URL аватара в Firebase
                    auth.currentUser?.uid?.let { userId ->
                        firestore.collection("users")
                            .document(userId)
                            .update("avatarUrl", imageUrl)
                            .await()
                    }

                    _uploadState.value = UploadState.Success(imageUrl)
                    // Обновляем информацию о пользователе
                    getCurrentUser()
                } else {
                    throw IOException("Ошибка загрузки: ${response.code()}")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    when (e) {
                        is FirebaseAuthException -> "Ошибка аутентификации: ${e.message}"
                        is SecurityException -> "Ошибка доступа к файлу: ${e.message}"
                        is IllegalArgumentException -> "Некорректный формат файла: ${e.message}"
                        else -> "Произошла ошибка: ${e.message}"
                    }
                )
            }
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
                            name = if (index == 0) selectedUser.username else currentUser.username,
                            avatarUrl = if (index == 0) selectedUser.avatarUrl else currentUser.avatarUrl
                        )

                        val updatedUser = userData?.copy(
                            chats = (userData.chats ?: emptyList()) + chatWithUpdatedName
                        ) ?: User(
                            id = if (index == 0) currentUser.id else selectedUser.id,
                            username = if (index == 0) currentUser.username else selectedUser.username,
                            email = if (index == 0) currentUser.email else selectedUser.email,
                            avatarUrl = "",
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