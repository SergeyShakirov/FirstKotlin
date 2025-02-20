package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.Message
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.presentation.ChatListViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatListViewModel,
    chat: Chat,
    onBack: () -> Unit,
    chatRepository: ChatRepository = ChatRepository()
    ) {
        var messageText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val snackbarHostState = remember { SnackbarHostState() }
        var isLoading by remember { mutableStateOf(true) }
        val chatRepository = remember { ChatRepository() }

        // Предотвращаем множественные подписки
        var hasSubscribed by remember { mutableStateOf(false) }

        // Получаем сообщения
        var messages by remember { mutableStateOf<List<Message>>(emptyList()) }

        LaunchedEffect(chat.id) {
            viewModel.messageHandler?.stopListeningChat(chat.id)
            if (!hasSubscribed) {
                hasSubscribed = true
                isLoading = true
                try {
                    chatRepository.subscribeToMessages(chat.id).collect { newMessages ->
                        messages = newMessages
                        isLoading = false
                    }
                } catch (e: Exception) {
                    isLoading = false
                    snackbarHostState.showSnackbar("Ошибка загрузки сообщений")
                }
            }
        }

    DisposableEffect(chat.id) {
        onDispose {
            // При выходе - возобновляем прослушку
            viewModel.messageHandler?.startListeningChat(chat)
        }
    }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            chat.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            // Redesigned message input section with red-white style and borderless design
            bottomBar = {
                TelegramStyleChatInput(
                    value = messageText,
                    onValueChange = { messageText = it },
                    onSendMessage = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                try {
                                    chatRepository.sendMessage(chat, messageText, viewModel)
                                    messageText = ""
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Ошибка отправки сообщения")
                                }
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFD32F2F).copy(alpha = 0.1f),
                                Color(0xFFF44336).copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        state = listState,
                        reverseLayout = true
                    ) {
                        items(messages) { message ->
                            MessageItem(
                                viewModel = viewModel,
                                chatId = chat.id,
                                message = message,
                                isCurrentUser = message.senderId == currentUserId,
                            )
                            //Spacer(modifier = Modifier.height(8.dp))
                        }

                    }
                }
            }
        }

        // Очистка при уничтожении
        DisposableEffect(chat.id) {
            onDispose {
                hasSubscribed = false
            }
        }

    }

@Composable
fun MessageItem(viewModel: ChatListViewModel, chatId: String, message: Message, isCurrentUser: Boolean) {

    LaunchedEffect(Unit) {
        viewModel.markMessageAsRead(chatId, message.id)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = if (isCurrentUser) 16.dp else 4.dp,
                topEnd = if (isCurrentUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
