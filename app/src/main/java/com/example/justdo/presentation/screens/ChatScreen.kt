package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.justdo.GogoApplication
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.Message
import com.example.justdo.presentation.components.MessageItem
import com.example.justdo.presentation.components.TelegramStyleChatInput
import com.example.justdo.presentation.viewmodels.ChatListViewModel
import com.example.justdo.ui.components.TelegramAvatar
import com.example.justdo.ui.components.TelegramTopBar
import com.example.justdo.ui.theme.TelegramColors
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    viewModel: ChatListViewModel,
    chat: Chat,
    onBack: () -> Unit,
) {
    // Получаем ChatManager через Application
    val chatManager = (LocalContext.current.applicationContext as GogoApplication).chatManager

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val messages by viewModel.messages.collectAsState()

    var hasSubscribed by remember { mutableStateOf(false) }

    LaunchedEffect(chat.id) {
        chatManager.setActiveChat(chat.id)
        viewModel.startMessageListener(chat.id)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Чат становится неактивным
                    chatManager.setActiveChat(null)
                }

                Lifecycle.Event.ON_RESUME -> {
                    // Чат снова активен
                    chatManager.setActiveChat(chat.id)
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            chatManager.setActiveChat(null)
        }
    }

    Scaffold(
        topBar = {
            TelegramTopBar(
                title = chat.name,
                subtitle = "был(а) в сети недавно", // Можно добавить статус пользователя
                showBackButton = true,
                onBackClick = onBack,
                avatar = {
                    TelegramAvatar(
                        name = chat.name,
                        avatarUrl = chat.avatarUrl,
                        size = 36.dp
                    )
                }
            )
        },
        bottomBar = {
            TelegramStyleChatInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        viewModel.onSendMessage(messageText)
                        messageText = ""
                    }
                }
            )
        },
        containerColor = TelegramColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Основной контент чата
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                    )
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
fun TelegramMessageItem(
    viewModel: ChatListViewModel,
    chatId: String,
    message: Message,
    isCurrentUser: Boolean
) {
    LaunchedEffect(Unit) {
        viewModel.markMessageAsRead(chatId, message.id)
    }

    val formattedTime = remember(message.timestamp) {
        if (message.timestamp != null) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(message.timestamp))
        } else {
            ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Сообщение
            Surface(
                color = if (isCurrentUser) TelegramColors.MyMessage else TelegramColors.OtherMessage,
                shape = RoundedCornerShape(
                    topStart = if (isCurrentUser) 16.dp else 4.dp,
                    topEnd = if (isCurrentUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 4.dp
                    )
                ) {
                    // Текст сообщения
                    Text(
                        text = message.text,
                        color = TelegramColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Время отправки
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = formattedTime,
                            color = TelegramColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}