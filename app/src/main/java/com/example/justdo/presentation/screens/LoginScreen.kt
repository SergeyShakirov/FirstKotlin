package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.justdo.data.MessengerRepository
import com.example.justdo.data.User
import com.example.justdo.presentation.components.LoginForm
import com.example.justdo.presentation.components.LoginHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    repository: MessengerRepository,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoginHeader()
        LoginForm(
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            error = error,
            onLoginClick = {
                scope.launch {
                    try {
                        val success = repository.login(username, password)
                        if (success) {
                            onLoginSuccess()
                        } else {
                            error = "Неверный логин или пароль"
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Ошибка авторизации"
                    }
                }
            }
        )
        TextButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Создать аккаунт")
        }
    }
}