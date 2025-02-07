package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.justdo.data.MessengerRepository
import com.example.justdo.data.User

@Composable
fun MyApp(repository: MessengerRepository) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var selectedUser by remember { mutableStateOf<User?>(null) }

    Scaffold { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            when {
                !isAuthenticated && !isRegistering -> {
                    LoginScreen(
                        repository = repository,
                        onLoginSuccess = {
                            isAuthenticated = true
                        },
                        onRegisterClick = {
                            isRegistering = true
                        }
                    )
                }
                !isAuthenticated && isRegistering -> {
                    RegisterScreen(
                        repository = repository,
                        onRegisterSuccess = { user ->
                            currentUser = user
                            isAuthenticated = true
                            isRegistering = false
                        },
                        onBackToLogin = {
                            isRegistering = false
                        }
                    )
                }
                selectedUser == null -> {
                    UserList(
                        repository = repository,
                        onUserClicked = { user -> selectedUser = user },
                        onLogout = {
                            isAuthenticated = false
                            currentUser = null
                        }
                    )
                }
                else -> {
                    ChatScreen(
                        user = selectedUser,
                        onBack = { selectedUser = null }
                    )
                }
            }
        }
    }
}