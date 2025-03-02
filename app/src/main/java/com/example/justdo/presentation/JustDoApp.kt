package com.example.justdo.presentation

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.presentation.screens.LoginScreen
import com.example.justdo.presentation.screens.MyApp
import com.example.justdo.presentation.viewmodels.AuthState
import com.example.justdo.presentation.viewmodels.AuthViewModel
import com.example.justdo.presentation.viewmodels.LoginViewModel
import com.example.justdo.ui.theme.JustDoTheme
import kotlinx.coroutines.launch

@Composable
fun JustDoApp() {
    val TAG = "JustDoApp"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Создаем репозитории
    val authRepository = remember { AuthRepository(context) }
    val chatRepository = remember { ChatRepository() }
    val userRepository = remember { UserRepository() }

    // Создаем AuthViewModel для управления состоянием авторизации
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(authRepository)
    )
    // Получаем текущее состояние авторизации
    val authState by authViewModel.authState.collectAsState()

    // Проверяем статус авторизации при запуске
    LaunchedEffect(Unit) {
        authViewModel.checkAuthStatus()
    }

    // Применяем тему JustDo
    JustDoTheme(forceTelegramStyle = true) {
        when (authState) {
            is AuthState.Loading -> {
                LoadingScreen()
            }

            is AuthState.Authorized -> {
                MyApp(
                    repository = authRepository,
                    chatRepository = chatRepository,
                    userRepository = userRepository,
                    initialUser = (authState as AuthState.Authorized).user,
                    onLogout = {
                        authViewModel.setUnauthorized()
                        scope.launch {
                            authRepository.logout()
                        }
                    }
                )
            }

            is AuthState.Unauthorized -> {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModel.Factory(authRepository, context)
                )
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        Log.d(TAG, "Вызван onLoginSuccess")
                        val user = loginViewModel.uiState.value.user
                        if (user != null) {
                            scope.launch {
                                authViewModel.setAuthorized(user)
                                loginViewModel.resetState()
                            }
                        }
                    }
                )
            }
        }
    }
}