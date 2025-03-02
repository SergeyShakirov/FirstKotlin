package com.example.justdo.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Класс для хранения состояния авторизации в виде одного объекта
 * вместо отдельных полей isAuthorized и currentUser
 */
sealed class AuthState {
    /** Состояние загрузки/проверки авторизации */
    data object Loading : AuthState()

    /** Состояние, когда пользователь не авторизован */
    data object Unauthorized : AuthState()

    /** Состояние, когда пользователь авторизован */
    data class Authorized(val user: User) : AuthState()
}

/**
 * ViewModel для управления состоянием авторизации на уровне приложения
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val TAG = "AuthViewModel"

    // Единое состояние для авторизации (вместо отдельных isAuthorized и currentUser)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Вспомогательные свойства для совместимости с существующим кодом
    val isAuthorized: StateFlow<Boolean> = authState.map { state ->
        state is AuthState.Authorized
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentUser: StateFlow<User?> = authState.map { state ->
        if (state is AuthState.Authorized) state.user else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoading: StateFlow<Boolean> = authState.map { state ->
        state is AuthState.Loading
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        // Инициализируем Google Sign-In при создании ViewModel
        try {
            authRepository.initGoogleSignIn()
            Log.d(TAG, "Google Sign-In инициализирован успешно")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации Google Sign-In", e)
        }
    }

    /**
     * Проверяет текущий статус авторизации пользователя
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting authorization status check...")
                _authState.value = AuthState.Loading

                // Check for user through repository
                val user = authRepository.getCurrentUser()

                _authState.value = if (user != null) {
                    Log.d(TAG, "User found: ${user.id}, name: ${user.username}")
                    AuthState.Authorized(user)
                } else {
                    Log.d(TAG, "User not found, authorization required")
                    AuthState.Unauthorized
                }

                Log.d(TAG, "Authorization status: ${_authState.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking authorization status", e)
                // Consider user unauthorized in case of error
                _authState.value = AuthState.Unauthorized
            }
        }
    }

    /**
     * Выход из системы
     */
    suspend fun logout() {
        try {
            Log.d(TAG, "Начинаем процесс выхода из системы...")
            _authState.value = AuthState.Loading

            // Выходим из системы через репозиторий
            val success = authRepository.logout()

            if (success) {
                _authState.value = AuthState.Unauthorized
                Log.d(TAG, "Успешный выход из системы")
            } else {
                Log.e(TAG, "Ошибка при выходе из системы")
                // Возвращаем предыдущее состояние, если выход не удался
                // Но только если оно не Loading
                if (_authState.value is AuthState.Loading) {
                    _authState.value = AuthState.Unauthorized
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при выходе из системы", e)
            // В случае ошибки сбрасываем на неавторизованное состояние
            _authState.value = AuthState.Unauthorized
        }
    }

    /**
     * Установка авторизованного состояния с пользователем
     */
    fun setAuthorized(user: User) {
        _authState.value = AuthState.Authorized(user)
        Log.d(TAG, "Установлен авторизованный статус для пользователя: ${user.id}, имя: ${user.username}")
    }

    /**
     * Сброс авторизации
     */
    fun setUnauthorized() {
        _authState.value = AuthState.Unauthorized
    }

    /**
     * Factory для создания ViewModel
     */
    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}