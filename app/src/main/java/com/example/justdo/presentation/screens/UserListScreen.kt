package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.justdo.data.MessengerRepository
import com.example.justdo.data.User
import com.example.justdo.presentation.components.UserListContent
import com.example.justdo.presentation.components.UserListTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserList(
    repository: MessengerRepository,
    onUserClicked: (User) -> Unit,
    onLogout: () -> Unit
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            try {
                users = repository.getUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            UserListTopBar(onLogout = onLogout)
        }
    ) { paddingValues ->
        UserListContent(
            users = users,
            isLoading = isLoading,
            onUserClicked = onUserClicked,
            modifier = Modifier.padding(paddingValues)
        )
    }
}