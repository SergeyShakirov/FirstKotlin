package com.example.justdo.presentation.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.data.repository.FirebaseStorageService
import com.example.justdo.data.models.UploadState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

class ProfileViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val storageService = FirebaseStorageService()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _userAvatars = MutableStateFlow<List<String>>(emptyList())
    val userAvatars = _userAvatars.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = userRepository.currentUser

    init {
        // Наблюдаем за состоянием загрузки из StorageService
        viewModelScope.launch {
            _currentUser.value = userRepository.getCurrentUser()

            // Загружаем аватарки при инициализации
            loadUserAvatars()

            storageService.uploadState.collect { state ->
                _uploadState.value = state

                // Если загрузка успешно завершена, обновляем список аватарок
                if (state is UploadState.Success) {
                    loadUserAvatars()
                }
            }
        }
    }

    fun deleteAvatar(userId: String, avatarUrl: String) {
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Начинаем удаление аватарки: $avatarUrl")

                // Сохраняем текущий список аватарок до удаления
                val currentAvatars = _userAvatars.value.toList()

                // Начинаем с локального обновления UI для мгновенной реакции
                // Удаляем аватарку из локального списка
                _userAvatars.value = currentAvatars.filter { it != avatarUrl }

                // Если удаляемая аватарка - текущая аватарка пользователя
                val isCurrentAvatar = _currentUser.value?.avatarUrl == avatarUrl
                if (isCurrentAvatar) {
                    // Находим следующую доступную аватарку
                    val nextAvatar = currentAvatars.firstOrNull { it != avatarUrl } ?: ""

                    // Обновляем локального пользователя для мгновенной реакции UI
                    _currentUser.value = _currentUser.value?.copy(avatarUrl = nextAvatar)

                    // Обновляем в Firestore асинхронно
                    updateUserAvatar(userId, nextAvatar)
                }

                // Затем выполняем фактическое удаление в Storage
                val deleted = storageService.deleteAvatarByUrl(avatarUrl)

                // После удаления обновляем список аватарок из Storage
                if (deleted) {
                    Log.d("ProfileViewModel", "Аватарка успешно удалена, обновляем данные")

                    // Загружаем актуальный список аватарок
                    loadUserAvatars()
                } else {
                    Log.e("ProfileViewModel", "Не удалось удалить аватарку")

                    // Если не удалось удалить, восстанавливаем исходный список
                    _userAvatars.value = currentAvatars

                    // И восстанавливаем исходную аватарку пользователя, если пытались её изменить
                    if (isCurrentAvatar) {
                        _currentUser.value = _currentUser.value?.copy(avatarUrl = avatarUrl)
                        updateUserAvatar(userId, avatarUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Ошибка при удалении аватарки: ${e.message}", e)
                // Загружаем список аватарок заново на случай, если что-то пошло не так
                loadUserAvatars()
            }
        }
    }

    suspend fun updateUserAvatar(userId: String, avatarUrl: String): Boolean {
        return try {
            val result = usersCollection.document(userId)
                .update("avatarUrl", avatarUrl)
                .await()

            // Обновляем локального пользователя
            _currentUser.value = _currentUser.value?.copy(avatarUrl = avatarUrl)

            true
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Ошибка при обновлении аватарки: ${e.message}", e)
            false
        }
    }

    /**
     * Загружает все аватарки пользователя из Firebase Storage
     */
    fun loadUserAvatars() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = _currentUser.value?.id ?: return@launch

                // Получаем аватарки из Storage
                val avatars = storageService.getUserAvatars(userId)

                // Важное изменение: формируем комбинированный список, включая текущую аватарку
                val combinedList = mutableListOf<String>()

                // Добавляем текущую аватарку, если она не пустая и её нет в списке аватарок
                _currentUser.value?.avatarUrl?.takeIf { it.isNotEmpty() }?.let { currentAvatar ->
                    if (!avatars.contains(currentAvatar)) {
                        combinedList.add(currentAvatar)
                    }
                }

                // Добавляем все остальные аватарки
                combinedList.addAll(avatars.filter { it.isNotEmpty() && !combinedList.contains(it) })

                // Устанавливаем итоговый список
                _userAvatars.value = combinedList

                Log.d("ProfileViewModel", "Загружено ${combinedList.size} аватарок")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Ошибка при загрузке аватарок", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Загружает аватарку пользователя
     */
    fun uploadAvatar(uri: Uri, context: Context) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                // Получаем ContentResolver из контекста, переданного как параметр
                val contentResolver = context.contentResolver

                // Загружаем изображение в Firebase Storage
                val avatarUrl = storageService.uploadImage(uri, contentResolver, user.id)

                // Обновляем URL аватарки в Firestore
                avatarUrl?.let {
                    if (userRepository.updateUserAvatar(user.id, it)) {
                        // Обновляем локального пользователя
                        _currentUser.value = user.copy(avatarUrl = it)

                        // Важное изменение: обновляем список аватарок после успешной загрузки
                        loadUserAvatars()
                    }
                }
            } catch (e: IOException) {
                Log.e("ProfileViewModel", "Ошибка при загрузке аватарки", e)
            }
        }
    }

    fun updateUsername(userId: String, newUsername: String) {
        viewModelScope.launch {
            userRepository.updateUsername(userId, newUsername)
        }
    }

    class Factory(
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Неизвестный класс ViewModel")
        }
    }
}