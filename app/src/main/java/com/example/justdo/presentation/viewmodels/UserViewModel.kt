package com.example.justdo.presentation.viewmodels

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.AuthState
import com.example.justdo.data.models.CountryInfo
import com.example.justdo.data.models.LoginUiState
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.UserRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ViewModel для управления состоянием авторизации на уровне приложения
 */
class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "UserViewModel"

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

    // Состояние UI для экрана авторизации
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Список стран с кодами для выбора
    private val _countries = listOf(
        CountryInfo("Грузия", "+995", "🇬🇪"),
        CountryInfo("Россия", "+7", "🇷🇺"),
        CountryInfo("Казахстан", "+7", "🇰🇿"),
        CountryInfo("Украина", "+380", "🇺🇦"),
        CountryInfo("Беларусь", "+375", "🇧🇾"),
        CountryInfo("США", "+1", "🇺🇸"),
        CountryInfo("Германия", "+49", "🇩🇪"),
        CountryInfo("Великобритания", "+44", "🇬🇧"),
        CountryInfo("Франция", "+33", "🇫🇷"),
        CountryInfo("Италия", "+39", "🇮🇹"),
        CountryInfo("Испания", "+34", "🇪🇸"),
        CountryInfo("Китай", "+86", "🇨🇳"),
        CountryInfo("Япония", "+81", "🇯🇵"),
        CountryInfo("Южная Корея", "+82", "🇰🇷"),
        CountryInfo("Индия", "+91", "🇮🇳"),
        CountryInfo("Бразилия", "+55", "🇧🇷")
    )

    // Таймер для повторной отправки кода
    private var resendTimer: Job? = null

    // Для хранения ID верификации телефона
    private var verificationId: String? = null

    // Для хранения учетных данных телефона после верификации
    private var phoneCredential: PhoneAuthCredential? = null

    init {
        userRepository.initGoogleSignIn()
        checkAuthStatus()
        _uiState.update {
            it.copy(
                countryList = _countries,
                selectedCountry = _countries[0]
            )
        }
    }

    /**
     * Проверяет текущий статус авторизации пользователя
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val user = userRepository.getCurrentUser()

                if (user != null) {
                    _authState.value = AuthState.Authorized(user)
                    userRepository.setCurrentUser(user)
                } else {
                    _authState.value = AuthState.Unauthorized
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthorized
            }
        }
    }

    /**
     * Получение Intent для входа через Google
     */
    fun getGoogleSignInIntent() = userRepository.getGoogleSignInIntent()

    /**
     * Установка флага для показа экрана ввода телефона
     */
    fun showPhoneLogin() {
        _uiState.update { it.copy(showPhoneLogin = true) }
    }

    /**
     * Выбор страны и кода телефона
     */
    fun selectCountry(country: CountryInfo) {
        _uiState.update { it.copy(selectedCountry = country) }
    }

    /**
     * Возврат к предыдущему экрану из экрана ввода имени
     */
    fun backToVerification() {
        _uiState.update { it.copy(isVerificationCompleted = false, isCodeSent = true) }
    }

    /**
     * Сохранение имени пользователя и авторизация
     */
    fun saveUsernameAndSignIn(username: String) {
        viewModelScope.launch {
            try {
                val finalUser = _uiState.value.user!!.copy(username = username)
                try {
                    // Явно сохраняем обновленное имя пользователя в Firestore
                    userRepository.updateUserProfile(finalUser)
                    // Обновляем состояние UI
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = finalUser
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = finalUser,
                            error = "Вы авторизованы, но возникла ошибка при обновлении имени"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Неизвестная ошибка при авторизации по телефону"
                    )
                }
            }
        }
    }

    /**
     * Проверка введенного кода подтверждения
     */
    fun verifyPhoneCode(code: String) {
        _uiState.update { it.copy(isLoading = true, error = "") }
        try {
            val vid = verificationId
            if (vid == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка верификации: отсутствует ID"
                    )
                }
                return
            }

            val credential = userRepository.verifyPhoneCode(vid, code)
            if (credential != null) {
                // Сохраняем учетные данные и переходим к экрану ввода имени
                phoneCredential = credential
                viewModelScope.launch {
                    val result = userRepository.signInWithPhoneCredential(credential)
                    result.onSuccess { user ->
                        _uiState.update { it1 ->
                            it1.copy(
                                isLoading = false,
                                isVerificationCompleted = user.username.isEmpty(),
                                user = user,
                                username = user.username,
                                isAuthenticated = user.username.isNotEmpty()
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Ошибка авторизации по телефону"
                            )
                        }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Неверный код подтверждения"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка при проверке кода"
                )
            }
        }

    }

    // Запуск авторизации по телефону
    fun startPhoneAuth(
        phoneNumber: String,
        activity: Activity,
    ) {
        // Форматируем номер с кодом страны
        val country = _uiState.value.selectedCountry
        var formattedNumber = phoneNumber

        // Удаляем все нецифровые символы, включая пробелы и знаки +
        formattedNumber = formattedNumber.replace(Regex("[^0-9]"), "")

        // Удаляем код страны, если он уже присутствует в начале номера
        val countryCodeDigits = country?.code?.replace(Regex("[^0-9]"), "")
        if (countryCodeDigits?.let { formattedNumber.startsWith(it) } == true) {
            formattedNumber = formattedNumber.substring(countryCodeDigits.length)
        }

        // Добавляем код страны в правильном формате E.164
        if (country != null) {
            formattedNumber = "${country.code}${formattedNumber}"
        }

        _uiState.update { it.copy(isLoading = true, error = "", formattedPhoneNumber = formattedNumber) }

        try {
            val auth = FirebaseAuth.getInstance()

            // Создаем callback-объект
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    phoneCredential = credential
                    _uiState.update { it.copy(isLoading = false, isVerificationCompleted = true) }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Ошибка отправки кода"
                        )
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@UserViewModel.verificationId = verificationId
                    _uiState.update { it.copy(isLoading = false, isCodeSent = true) }
                }
            }

            // Настройка параметров верификации
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            // Начинаем верификацию
            PhoneAuthProvider.verifyPhoneNumber(options)

        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Неизвестная ошибка"
                )
            }
        }
    }

    /**
     * Выход из системы
     */
    suspend fun logout() {
        try {
            _authState.value = AuthState.Loading
            val success = userRepository.logout()

            if (success) {
                _authState.value = AuthState.Unauthorized
            } else {
                if (_authState.value is AuthState.Loading) {
                    _authState.value = AuthState.Unauthorized
                }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthorized
        }
    }

    /**
     * Сброс состояния авторизации
     */
    fun resetState() {
        _uiState.update { LoginUiState(countryList = _countries, selectedCountry = _countries[0]) }
        resendTimer?.cancel()
        verificationId = null
        phoneCredential = null
    }

    /**
     * Запуск процесса авторизации через Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            try {
                val result = userRepository.signInWithGoogle(idToken)
                result.onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = user
                        )
                    }
                }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Ошибка авторизации через Google"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Неизвестная ошибка при авторизации через Google"
                    )
                }
            }
        }
    }

    // Добавить в класс LoginViewModel метод для установки ошибки:
    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    /**
     * Установка авторизованного состояния с пользователем
     */
    fun setAuthorized(user: User) {
        _authState.value = AuthState.Authorized(user)
        userRepository.setCurrentUser(user)
    }

    /**
     * Сброс авторизации
     */
    fun setUnauthorized() {
        _authState.value = AuthState.Unauthorized
        userRepository.setCurrentUser(null)
    }

    /**
     * Factory для создания ViewModel
     */
    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                return UserViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}