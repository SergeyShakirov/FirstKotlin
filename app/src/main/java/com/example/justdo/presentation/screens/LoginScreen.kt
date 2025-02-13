package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.presentation.components.LoginForm
import com.example.justdo.presentation.components.LoginHeader
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: AuthRepository,
    onLoginSuccess: (User) -> Unit,
    onRegisterClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Проверяем сохраненные данные при запуске
    LaunchedEffect(Unit) {
        repository.getCurrentUser()?.let { user ->
            onLoginSuccess(user)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoginHeader()
        LoginForm(
            email = email,
            onEmailChange = { email = it },
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            error = error,
            isLoading = isLoading,
            onLoginClick = {
                scope.launch {
                    isLoading = true
                    val result = repository.loginWithEmail(email, password)
                    result.fold(
                        onSuccess = { user ->
                            onLoginSuccess(user)
                        },
                        onFailure = { exception ->
                            error = exception.message ?: "Ошибка входа"
                        }
                    )
                    isLoading = false
                }
            }
        )

        TextButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Создать аккаунт")
        }
    }
}