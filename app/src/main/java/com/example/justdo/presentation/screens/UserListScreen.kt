package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.justdo.data.repository.MessengerRepository
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.presentation.components.UserListContent
import kotlinx.coroutines.delay

@Composable
fun UserList(
    repository: AuthRepository,
    onUserClicked: (User) -> Unit
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                //users = repository.getUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
            delay(5000)
        }
    }

    Scaffold { paddingValues ->
        UserListContent(
            users = users,
            onUserClicked = onUserClicked,
            modifier = Modifier.padding(paddingValues)
        )
    }
}