package com.example.justdo.presentation.screens

import com.example.justdo.ui.theme.RedWhiteColorScheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.presentation.components.AnimatedBackground
import com.example.justdo.presentation.components.AnimatedButton
import com.example.justdo.presentation.components.AnimatedLogo
import com.example.justdo.presentation.components.ExpandableLoginForm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(isLoading: Boolean,
    repository: AuthRepository,
    onLoginSuccess: (User) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    //var isLoading by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }

    val redWhiteColorScheme = RedWhiteColorScheme

    // Анимация для ошибки
    var showError by remember { mutableStateOf(false) }
    LaunchedEffect(error) {
        if (error != null) {
            showError = true
            delay(500)
            showError = false
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    MaterialTheme(
        colorScheme = redWhiteColorScheme
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(redWhiteColorScheme.background)
        ) {
            AnimatedBackground()

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(initialAlpha = 0f),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .graphicsLayer {
                            if (showError) {
                                translationX = (Math.random() * 20 - 10).toFloat()
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedLogo()

                    Spacer(modifier = Modifier.height(32.dp))

                    AnimatedContent(
                        targetState = isRegisterMode,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }, label = ""
                    ) { registerMode ->
                        if (!registerMode) {
                            // Форма входа
                            ExpandableLoginForm(
                                email = email,
                                onEmailChange = { email = it },
                                password = password,
                                onPasswordChange = { password = it },
                                error = error,
                                isLoading = isLoading,
                                onLoginClick = {
                                    scope.launch {
                                        //isLoading = true
                                        val result = repository.loginWithEmail(email, password)
                                        result.fold(
                                            onSuccess = { user ->
                                                onLoginSuccess(user)
                                            },
                                            onFailure = { exception ->
                                                error = exception.message ?: "Ошибка входа"
                                            }
                                        )
                                        //isLoading = false
                                    }
                                }
                            )
                        } else {
                            // Форма регистрации
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Имя пользователя") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Пароль") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Подтвердите пароль") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    singleLine = true
                                )

                                error?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedButton(
                        onClick = {
                            if (!isRegisterMode) {
                                // Переход в режим регистрации
                                isRegisterMode = true
                            } else {
                                // Логика регистрации
                                scope.launch {
                                    if (password != confirmPassword) {
                                        error = "Пароли не совпадают"
                                        return@launch
                                    }

                                    //isLoading = true
                                    val result = repository.registerWithEmail(email, password, username)
                                    result.fold(
                                        onSuccess = { user ->
                                            onLoginSuccess(user)
                                        },
                                        onFailure = { exception ->
                                            error = exception.message ?: "Ошибка регистрации"
                                        }
                                    )
                                    //isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), // Добавлен modifier для полной ширины
                        enabled = !isLoading
                    ) {
                        Text(
                            if (!isRegisterMode) "Создать аккаунт" else "Зарегистрироваться",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            modifier = Modifier.fillMaxWidth(), // Добавлен modifier для полной ширины
                            onClick = {
                                isRegisterMode = false
                                error = null
                            }
                        ) {
                            Text("Войти")
                        }
                    }
                }
            }

            // Loader с красным акцентом
            AnimatedVisibility(
                visible = isLoading,
                enter = scaleIn(initialScale = 0.8f) + fadeIn(initialAlpha = 0f),
                exit = scaleOut(targetScale = 0.8f) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(redWhiteColorScheme.background.copy(alpha = 0.8f))
                        .blur(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = redWhiteColorScheme.surface.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(50.dp)
                                .padding(12.dp),
                            color = redWhiteColorScheme.primary
                        )
                    }
                }
            }
        }
    }
}