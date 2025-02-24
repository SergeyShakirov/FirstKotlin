package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.User
import com.example.justdo.presentation.ChatListViewModel
import kotlinx.coroutines.delay
import androidx.compose.material.ripple.rememberRipple

@Composable
fun ChatList(
    currentUser: User,
    onChatClicked: (Chat) -> Unit,
    onAddClicked: () -> Unit,
    viewModel: ChatListViewModel
) {
    val chats by viewModel.chats.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadUserChats(currentUser.id)
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onChatClicked(chat) }
                    )  {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chat.avatarUrl != "") {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(chat.avatarUrl)
                                        .crossfade(true)
                                        .transformations(CircleCropTransformation())
                                        .build(),
                                    contentDescription = "Аватар пользователя ${chat.name}",
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD32F2F)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (chat.name.isNotEmpty()) chat.name.first().uppercase() else "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = chat.name.ifEmpty { "Новый чат" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Text(
                                    text = chat.lastMessage.ifEmpty { "Нет сообщений" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}