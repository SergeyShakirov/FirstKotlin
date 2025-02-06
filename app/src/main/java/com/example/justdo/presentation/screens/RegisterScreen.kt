package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.justdo.data.MessengerRepository
import com.example.justdo.data.User
import com.example.justdo.presentation.components.RegisterForm
import com.example.justdo.presentation.components.RegisterHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    repository: MessengerRepository,
    onRegisterSuccess: (User) -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RegisterHeader()
        RegisterForm(
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            error = error,
            onRegisterClick = {
                scope.launch {
                    try {
                        if (password != confirmPassword) {
                            error = "Пароли не совпадают"
                            return@launch
                        }
                        val user = repository.register(username, password)
                        onRegisterSuccess(user)
                    } catch (e: Exception) {
                        error = e.message ?: "Ошибка регистрации"
                    }
                }
            },
            onBackToLoginClick = onBackToLogin
        )
    }
}