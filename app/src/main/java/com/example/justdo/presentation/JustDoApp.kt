package com.example.justdo.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.justdo.data.models.AuthState
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.presentation.screens.login.LoginScreen
import com.example.justdo.presentation.screens.MyApp
import com.example.justdo.presentation.viewmodels.UserViewModel
import com.example.justdo.ui.theme.JustDoTheme
import kotlinx.coroutines.launch

@Composable
fun JustDoApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Создаем репозитории
    val userRepository = remember { UserRepository(context) }
    // Создаем UserViewModel для управления состоянием авторизации
    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModel.Factory(userRepository)
    )
    // Получаем текущее состояние авторизации
    val authState by userViewModel.authState.collectAsState()

    JustDoTheme(forceTelegramStyle = true) {
        when (authState) {
            is AuthState.Loading -> {
                LoadingScreen()
            }

            is AuthState.Authorized -> {
                MyApp(
                    userRepository = userRepository,
                    onLogout = {
                        scope.launch {
                            userViewModel.logout()
                        }
                    }
                )
            }

            is AuthState.Unauthorized -> {
                LoginScreen(
                    userViewModel = userViewModel,
                    onLoginSuccess = { user ->
                        userViewModel.setAuthorized(user)
                    }
                )
            }
        }
    }
}