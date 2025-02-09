package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.justdo.data.MessengerRepository
import com.example.justdo.data.User
import com.example.justdo.presentation.components.LoginForm
import com.example.justdo.presentation.components.LoginHeader
import com.example.justdo.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: MessengerRepository,
    onLoginSuccess: (User) -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Проверяем сохраненные данные при запуске
    LaunchedEffect(Unit) {
        sessionManager.getCredentials()?.let { (savedUsername, savedPassword) ->
            try {
                val user = repository.login(savedUsername, savedPassword)
                onLoginSuccess(user)
            } catch (e: Exception) {
                // Если автологин не удался, очищаем сохраненные данные
                sessionManager.clearCredentials()
                error = "Ошибка автоматической авторизации"
            }
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
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            error = error,
            onLoginClick = {
                scope.launch {
                    try {
                        val user = repository.login(username, password)
                        sessionManager.saveCredentials(username, password)
                        onLoginSuccess(user)
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