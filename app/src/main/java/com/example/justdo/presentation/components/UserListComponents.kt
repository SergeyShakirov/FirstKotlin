package com.example.justdo.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.justdo.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListTopBar(onLogout: () -> Unit) {
    TopAppBar(
        title = { Text("Пользователи") },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Выйти"
                )
            }
        }
    )
}

@Composable
fun UserListContent(
    users: List<User>,
    isLoading: Boolean,
    onUserClicked: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(users) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onUserClicked(user) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = user.name,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}