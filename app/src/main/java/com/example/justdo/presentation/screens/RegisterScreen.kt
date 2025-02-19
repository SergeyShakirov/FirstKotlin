package com.example.justdo.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.data.models.User
import com.example.justdo.presentation.components.RegisterForm
import com.example.justdo.presentation.components.RegisterHeader
import com.example.justdo.ui.theme.RedWhiteColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    repository: AuthRepository,
    onRegisterSuccess: (User) -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    val redWhiteColorScheme = RedWhiteColorScheme

    // Анимация появления экрана
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    MaterialTheme(
        colorScheme = redWhiteColorScheme
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(initialAlpha = 0f) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RegisterHeader()

                RegisterForm(
                    email = email,
                    onEmailChange = { email = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    error = error,
                    isLoading = isLoading,
                    onRegisterClick = {
                        scope.launch {
                            isLoading = true
                            if (password != confirmPassword) {
                                error = "Пароли не совпадают"
                                isLoading = false
                                return@launch
                            }

                            val result = repository.registerWithEmail(email, password, username)
                            result.fold(
                                onSuccess = { user ->
                                    onRegisterSuccess(user)
                                },
                                onFailure = { exception ->
                                    error = exception.message ?: "Ошибка регистрации"
                                }
                            )
                            isLoading = false
                        }
                    },
                    onBackToLoginClick = onBackToLogin
                )
            }
        }
    }
}