package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.Chat
import com.example.justdo.presentation.ChatListViewModel
import com.example.justdo.presentation.components.ChatItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatList(
    chats: List<Chat>,
    onChatClicked: (Chat) -> Unit,
    onAddClicked: () -> Unit,
    viewModel: ChatListViewModel
) {
    var localChats by remember { mutableStateOf(chats) }
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(chats) {
        if (chats.isNotEmpty()) {
            localChats = chats
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadChats()
            delay(5000)
        }
    }

    Scaffold(
        floatingActionButton = {
            IconButton(
                onClick = { onAddClicked() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить",
                    tint = Color.White
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD32F2F).copy(alpha = 0.1f),
                            Color(0xFFF44336).copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            if (localChats.isEmpty() && isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                    color = Color(0xFFD32F2F)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = chats,
                        key = { chat -> chat.id }
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