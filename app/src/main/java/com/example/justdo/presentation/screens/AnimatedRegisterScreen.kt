package com.example.justdo.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.data.models.User
import com.example.justdo.presentation.components.RegisterForm
import com.example.justdo.presentation.components.RegisterHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedRegisterScreen(
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

    // Кастомная цветовая схема
    val redWhiteColorScheme = ColorScheme(
        primary = Color(0xFFD32F2F),           // Яркий красный
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD32F2F).copy(alpha = 0.2f),
        onPrimaryContainer = Color.White,
        inversePrimary = Color(0xFFD32F2F).copy(alpha = 0.5f),

        secondary = Color(0xFFF44336),         // Немного более светлый красный
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF44336).copy(alpha = 0.2f),
        onSecondaryContainer = Color.White,

        tertiary = Color(0xFFB71C1C),          // Темно-красный
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFB71C1C).copy(alpha = 0.2f),
        onTertiaryContainer = Color.White,

        background = Color.White,
        onBackground = Color.Black,

        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color.White.copy(alpha = 0.9f),
        onSurfaceVariant = Color.Black,
        surfaceTint = Color(0xFFD32F2F),

        inverseSurface = Color.Black,
        inverseOnSurface = Color.White,

        error = Color(0xFFB71C1C),             // Темно-красный для ошибок
        onError = Color.White,
        errorContainer = Color(0xFFB71C1C).copy(alpha = 0.2f),
        onErrorContainer = Color.White,

        outline = Color.Red.copy(alpha = 0.3f),
        outlineVariant = Color.Red.copy(alpha = 0.1f),
        scrim = Color.Black.copy(alpha = 0.5f),

        // Новые параметры в последней версии Material 3
        surfaceBright = Color.White,
        surfaceDim = Color.Gray.copy(alpha = 0.1f),
        surfaceContainer = Color.White.copy(alpha = 0.9f),
        surfaceContainerHigh = Color.White.copy(alpha = 0.8f),
        surfaceContainerHighest = Color.White.copy(alpha = 0.7f),
        surfaceContainerLow = Color.White.copy(alpha = 0.95f),
        surfaceContainerLowest = Color.White
    )

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