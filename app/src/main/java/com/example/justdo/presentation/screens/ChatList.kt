package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.Chat
import com.example.justdo.presentation.ChatListViewModel
import com.example.justdo.presentation.components.ChatItem
import kotlinx.coroutines.delay

@Composable
fun ChatList(
    chats: List<Chat>,
    onChatClicked: (Chat) -> Unit,
    onAddClicked: () -> Unit,
    viewModel: ChatListViewModel
) {
    // Используем remember для хранения локальной копии списка
    var localChats by remember { mutableStateOf(chats) }
    val isLoading by viewModel.isLoading.collectAsState()

    // Обновляем локальную копию только если список изменился
    LaunchedEffect(chats) {
        if (chats.isNotEmpty()) {
            localChats = chats
        }
    }

    // Запускаем фоновое обновление
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadChats()
            delay(5000)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddClicked() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            if (localChats.isEmpty() && isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(
                        items = chats,
                        key = { chat -> chat.id } // Добавляем key для оптимизации обновлений
                    ) { chat ->
                        ChatItem(
                            chat = chat,
                            onClick = { onChatClicked(chat) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

