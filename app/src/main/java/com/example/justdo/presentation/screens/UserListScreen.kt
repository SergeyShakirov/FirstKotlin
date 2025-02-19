package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.User
import com.example.justdo.presentation.ChatListViewModel
import com.example.justdo.presentation.components.UserListItem
import com.example.justdo.ui.theme.RedWhiteColorScheme

@Composable
fun UserList(
    viewModel: ChatListViewModel,
    onUserClicked: (User) -> Unit
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    MaterialTheme(
        colorScheme = RedWhiteColorScheme
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RedWhiteColorScheme.background)
        ) {
            // Анимированный фон в красно-белых тонах
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RedWhiteColorScheme.primary.copy(alpha = 0.1f),
                                RedWhiteColorScheme.secondary.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center),
                    color = RedWhiteColorScheme.primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(users) { user ->
                        UserListItem(
                            user = user,
                            onClick = { onUserClicked(user) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}