package com.example.justdo.presentation.viewmodels

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.Message
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.domain.models.UploadState
import com.example.justdo.network.api.ImgurApi
import com.example.justdo.network.constants.ImgurConfig
import com.example.justdo.services.MessageHandler
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import okhttp3.MultipartBody
import okio.IOException

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Состояние загрузки аватара
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

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

    suspend fun getChat(userId: String, currentUserId: String): Chat? {
        return try {
            // Проверяем оба возможных варианта id чата
            val chatId1 = "${currentUserId}_${userId}"
            val chatId2 = "${userId}_${currentUserId}"

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

    suspend fun createChat(userId: String, currentUserId: String): Chat? {
        return try {
            val chatId = "${currentUserId}_${userId}"

            // Создаем новый чат
            val newChat = hashMapOf(
                "id" to chatId,
                "lastMessage" to "",
                "timestamp" to Timestamp.now(),
                "participants" to listOf(currentUserId, userId)
            )

            // Добавляем чат в коллекцию newChats
            firestore.collection("newChats")
                .document(chatId)
                .set(newChat)
                .await()

            Chat(
                id = chatId,
                participants = listOf(currentUserId, userId),
                lastMessage = "",
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error creating chat", e)
            null
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading

            try {
                val context = auth.app.applicationContext

                // Проверяем существование файла
                if (!context.contentResolver.openInputStream(uri)?.use { true }!! ?: false) {
                    throw IOException("Файл не существует")
                }

                // Проверяем размер файла
                val fileSize = context.contentResolver.openInputStream(uri)?.use { it.available() } ?: 0
                if (fileSize > 10 * 1024 * 1024) { // 10MB лимит
                    throw IllegalArgumentException("Файл слишком большой (максимум 10MB)")
                }

                // Читаем файл с обработкой ошибок
                val byteArray = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.buffered().readBytes()
                } ?: throw IOException("Не удалось прочитать файл")

                // Проверяем, что файл не пустой
                if (byteArray.isEmpty()) {
                    throw IOException("Файл пустой")
                }

                // Создаем RequestBody с явным указанием типа контента
                val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = okhttp3.RequestBody.create(
                    contentType.toMediaType(),
                    byteArray
                )

                // Создаем MultipartBody.Part с уникальным именем файла
                val fileName = "avatar_${System.currentTimeMillis()}.${
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType) ?: "jpg"
                }"

                val body = MultipartBody.Part.createFormData(
                    "image",
                    fileName,
                    requestBody
                )

                // Добавляем retry механизм
                var retryCount = 0
                var lastException: Exception? = null

                while (retryCount < 3) {
                    try {
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
                            getCurrentUser()
                            return@launch
                        } else {
                            throw IOException("Ошибка загрузки: ${response.code()} - ${response.message()}")
                        }
                    } catch (e: Exception) {
                        lastException = e
                        retryCount++
                        if (retryCount < 3) {
                            delay(1000L * retryCount) // Экспоненциальная задержка
                            continue
                        }
                        throw e
                    }
                }

                throw lastException ?: IOException("Неизвестная ошибка при загрузке")

            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    when (e) {
                        is FirebaseAuthException -> "Ошибка аутентификации: ${e.message}"
                        is SecurityException -> "Ошибка доступа к файлу: ${e.message}"
                        is IllegalArgumentException -> "Некорректный формат файла: ${e.message}"
                        is IOException -> "Ошибка чтения файла: ${e.message}"
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