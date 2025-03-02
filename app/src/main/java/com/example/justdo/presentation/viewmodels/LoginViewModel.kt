package com.example.justdo.presentation.viewmodels

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.example.justdo.utils.BiometricAuthHelper

/**
 * Данные страны для выбора кода телефона
 */
data class CountryInfo(
    val name: String,
    val code: String,
    val flagEmoji: String
)

/**
 * ViewModel для управления процессом авторизации
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val context: android.content.Context? = null
) : ViewModel() {

    // Помощник для биометрической аутентификации
    private val biometricAuthHelper = context?.let { BiometricAuthHelper(it) }

    // Состояние UI для экрана авторизации
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Для хранения ID верификации телефона
    private var verificationId: String? = null

    // Для хранения учетных данных телефона после верификации
    private var phoneCredential: PhoneAuthCredential? = null

    // Таймер для повторной отправки кода
    private var resendTimer: Job? = null

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

    // Инициализация авторизации через Google и проверка биометрии
    init {
        try {
            authRepository.initGoogleSignIn()

            // Проверка доступности биометрии
            val isBiometricAvailable = biometricAuthHelper?.isBiometricAvailable() ?: false

            _uiState.update {
                it.copy(
                    countryList = _countries,
                    selectedCountry = _countries[0],
                    isBiometricAvailable = isBiometricAvailable
                )
            }

            Log.d(TAG, "Биометрия доступна: $isBiometricAvailable")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google Sign-In", e)
        }
    }

    /**
     * Запуск процесса авторизации через Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            try {
                val result = authRepository.signInWithGoogle(idToken)
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

    /**
     * Получение Intent для входа через Google
     */
    fun getGoogleSignInIntent() = authRepository.getGoogleSignInIntent()

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

    // Запуск авторизации по телефону
    fun startPhoneAuth(
        phoneNumber: String,
        activity: Activity,
        onVerificationSent: () -> Unit,
        onError: (String) -> Unit
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
                    Log.e(TAG, "📱 Верификация завершена автоматически")
                    phoneCredential = credential
                    _uiState.update { it.copy(isLoading = false, isVerificationCompleted = true) }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "📱 Ошибка верификации: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Ошибка отправки кода"
                        )
                    }
                    onError(e.message ?: "Ошибка отправки кода")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.e(TAG, "📱 Код отправлен, verificationId: $verificationId")
                    this@LoginViewModel.verificationId = verificationId
                    _uiState.update { it.copy(isLoading = false, isCodeSent = true) }
                    onVerificationSent()
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
            Log.e(TAG, "📱 Исключение при запуске верификации: ${e.message}")
            e.printStackTrace()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Неизвестная ошибка"
                )
            }
            onError(e.message ?: "Неизвестная ошибка")
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

            val credential = authRepository.verifyPhoneCode(vid, code)
            if (credential != null) {
                // Сохраняем учетные данные и переходим к экрану ввода имени
                phoneCredential = credential
                viewModelScope.launch {
                    val result = authRepository.signInWithPhoneCredential(credential)
                    result.onSuccess { user ->
                        _uiState.update { it1 ->
                            it1.copy(
                                isLoading = false,
                                isVerificationCompleted = user.username.isEmpty(),
                                user = user.takeIf { it.username.isNotEmpty() },
                                username = user.username,
                                isAuthenticated = user.username.isNotEmpty()
                            )
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "Ошибка авторизации по телефону", e)
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

    /**
     * Сохранение имени пользователя и авторизация
     */
    fun saveUsernameAndSignIn(username: String) {
        Log.d(TAG, "saveUsernameAndSignIn вызван с именем: $username")

        // Проверяем наличие PhoneAuthCredential
        val credential = phoneCredential
        if (credential == null) {
            Log.e(TAG, "Ошибка верификации: отсутствуют учетные данные")
            _uiState.update {
                it.copy(
                    error = "Ошибка верификации: отсутствуют учетные данные"
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = "", username = username) }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Начинаем авторизацию с phoneCredential")

                // Аутентификация с использованием телефонного credential
                val result = authRepository.signInWithPhoneCredential(credential)

                result.onSuccess { user ->
                    // Обновляем пользователя с заданным именем
                    val finalUser = user.copy(username = username)

                    try {
                        // Явно сохраняем обновленное имя пользователя в Firestore
                        authRepository.updateUserProfile(finalUser)

                        // Обновляем состояние UI
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                user = finalUser
                            )
                        }

                        Log.d(TAG, "Авторизация успешна, пользователь: ${finalUser.id}, имя: $username")
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при обновлении профиля", e)

                        // Даже при ошибке обновления профиля считаем пользователя авторизованным
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                user = finalUser,
                                error = "Вы авторизованы, но возникла ошибка при обновлении имени"
                            )
                        }
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Ошибка авторизации по телефону", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Ошибка авторизации по телефону"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Неизвестная ошибка при авторизации по телефону", e)
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
     * Авторизация с использованием телефонного credential
     */
    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            try {
                val result = authRepository.signInWithPhoneCredential(credential)
                result.onSuccess { user ->
                    // Обновляем пользователя с заданным именем, если оно есть
                    val finalUser = if (_uiState.value.username.isNotEmpty()) {
                        user.copy(username = _uiState.value.username)
                    } else {
                        user
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = finalUser
                        )
                    }

                    // Сохраняем обновленное имя пользователя, если требуется
                    if (_uiState.value.username.isNotEmpty()) {
                        updateUserProfile(finalUser)
                    }
                }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Ошибка авторизации по телефону"
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
     * Обновление профиля пользователя
     */
    private fun updateUserProfile(user: User) {
        viewModelScope.launch {
            try {
                // Здесь можно добавить вызов репозитория для сохранения имени
                // authRepository.updateUserProfile(user)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении профиля пользователя", e)
            }
        }
    }

    /**
     * Таймер для ограничения повторной отправки кода
     */
    private fun startResendTimer() {
        resendTimer?.cancel()

        resendTimer = viewModelScope.launch {
            var timeLeft = 60 // 60 секунд до повторной отправки

            _uiState.update {
                it.copy(
                    canResendCode = false,
                    resendTimeLeft = timeLeft
                )
            }

            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                _uiState.update { it.copy(resendTimeLeft = timeLeft) }
            }

            _uiState.update { it.copy(canResendCode = true) }
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
     * Возврат к предыдущему экрану из экрана ввода имени
     */
    fun backToVerification() {
        _uiState.update { it.copy(isVerificationCompleted = false, isCodeSent = true) }
    }

    /**
     * Очистка ресурсов при уничтожении ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        resendTimer?.cancel()
    }

    // Добавить в класс LoginViewModel метод для установки ошибки:
    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    // Добавить метод для инициализации Google Sign-In:
    fun initGoogleSignIn() {
        try {
            authRepository.initGoogleSignIn()
        } catch (e: Exception) {
            setError("Ошибка инициализации Google Sign-In: ${e.message}")
        }
    }


    /**
     * Проверяет возможность использования биометрии сразу при запуске
     */
    private fun checkBiometricAvailability() {
        if (context != null) {
            val biometricHelper = BiometricAuthHelper(context)
            val isBiometricAvailable = biometricHelper.isBiometricAvailable()
            val isBiometricEnabled = biometricHelper.isBiometricEnabled()

            _uiState.update {
                it.copy(
                    isBiometricAvailable = isBiometricAvailable,
                    isBiometricEnabled = isBiometricEnabled
                )
            }

            // Если биометрия доступна и включена, автоматически запускаем
            if (isBiometricAvailable && isBiometricEnabled) {
                authenticateWithBiometric()
            }
        }
    }

    /**
     * Factory для создания ViewModel с зависимостями
     */
    /**
     * Запуск аутентификации по биометрии
     */
    fun authenticateWithBiometric() {
        _uiState.update { it.copy(showBiometricPrompt = true) }
    }

    /**
     * Успешная биометрическая аутентификация
     */
    /**
     * Обработка успешной биометрической аутентификации
     */
    fun onBiometricSuccess() {
        if (context == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showBiometricPrompt = false, error = "") }

            try {
                val biometricHelper = BiometricAuthHelper(context)
                val userId = biometricHelper.getBiometricUserId()

                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "ID пользователя не найден"
                        )
                    }
                    return@launch
                }

                // Получаем пользователя из Firestore по ID
                val userDoc = authRepository.getUserById(userId)

                if (userDoc == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Пользователь не найден"
                        )
                    }
                    return@launch
                }

                // Успешная авторизация
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = userDoc
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при биометрической аутентификации", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Сохраняет настройки биометрической аутентификации
     */
    private fun saveBiometricSettings(enable: Boolean, userId: String? = null) {
        if (context != null) {
            val biometricHelper = BiometricAuthHelper(context)

            biometricHelper.setBiometricEnabled(enable)
            if (enable && userId != null) {
                biometricHelper.saveBiometricUserId(userId)
            } else if (!enable) {
                biometricHelper.clearBiometricData()
            }

            _uiState.update {
                it.copy(isBiometricEnabled = enable)
            }
        }
    }

    /**
     * Расширение успешной авторизации для сохранения данных биометрии
     */
    private fun onSuccessfulAuthentication(user: User) {
        // Обновляем состояние
        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                user = user
            )
        }

        // Если биометрия доступна и пользователь согласился, сохраняем данные
        if (_uiState.value.isBiometricAvailable && _uiState.value.useBiometricForNextLogin) {
            saveBiometricSettings(true, user.id)
        }
    }

    /**
     * Сброс флага показа биометрического диалога
     */
    fun resetBiometricPrompt() {
        _uiState.update { it.copy(showBiometricPrompt = false) }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val context: android.content.Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(authRepository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
    }
}

/**
 * Состояние UI для экрана авторизации
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String = "",
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val showPhoneLogin: Boolean = false,
    val isCodeSent: Boolean = false,
    val isVerificationCompleted: Boolean = false,
    val canResendCode: Boolean = false,
    val resendTimeLeft: Int = 0,
    val countryList: List<CountryInfo> = emptyList(),
    val selectedCountry: CountryInfo? = null,
    val username: String = "",
    val formattedPhoneNumber: String = "",
    val isBiometricAvailable: Boolean = false,
    val showBiometricPrompt: Boolean = false,
    val useBiometricForNextLogin: Boolean = false,
    val isBiometricEnabled: Boolean = false
)