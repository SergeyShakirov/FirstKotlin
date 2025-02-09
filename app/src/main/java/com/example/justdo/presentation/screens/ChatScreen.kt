package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.justdo.data.Message
import java.text.SimpleDateFormat
import java.util.*
import com.example.justdo.data.MessengerRepository
import com.example.justdo.data.User
import com.example.justdo.utils.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

sealed class ChatItem {
    data class MessageItem(val message: Message) : ChatItem()
    data class DateHeader(val date: String) : ChatItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(user: User?, onBack: () -> Unit) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }
    val notificationHelper = remember { NotificationHelper(context) }
    var lastProcessedMessageId by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Группировка сообщений по датам
    val chatItems = remember(messages) {
        messages
            .sortedBy { it.timestamp }
            .groupBy {
                Calendar.getInstance().apply {
                    timeInMillis = it.timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            .flatMap { (date, messagesForDate) ->
                listOf(ChatItem.DateHeader(dateFormatter.format(Date(date)))) +
                        messagesForDate.map { ChatItem.MessageItem(it) }
            }
    }

    // Загружаем сообщения каждые 5 секунд
    LaunchedEffect(user?.id) {
        val repository = MessengerRepository()
        while (true) {
            try {
                isLoading = messages.isEmpty()
                if (user != null) {
                    messages = repository.refreshMessages(user.id)
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = e.message ?: "Ошибка загрузки сообщений"
                )
            } finally {
                isLoading = false
            }
            delay(5000)
        }
    }

    // Прокрутка к последнему сообщению
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    // Обработка новых сообщений для уведомлений
    LaunchedEffect(messages) {
        for (message in messages) {
            if (!message.isFromMe &&
                message.id != lastProcessedMessageId &&
                message.timestamp > System.currentTimeMillis() - 10000
            ) {
                notificationHelper.showMessageNotification(message, user?.name ?: "")
                lastProcessedMessageId = message.id
            }
        }
    }

    // Очистка при уничтожении
    DisposableEffect(key1 = user?.id) {
        onDispose {
            notificationHelper.cancelAllNotifications()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = user?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading && messages.isEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatItems) { item ->
                    when (item) {
                        is ChatItem.DateHeader -> DateHeader(date = item.date)
                        is ChatItem.MessageItem -> ChatMessage(
                            message = item.message,
                            dateFormatter = timeFormatter
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Введите сообщение...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 3
                    )
                    IconButton(
                        onClick = {

                            if (messageText.isNotBlank() && !isSending) {
                                isSending = true

                                    val newMessage = Message(
                                        content = messageText,
                                        isFromMe = true
                                    )
                                    messages = messages + newMessage
                                    val messageToSend = messageText
                                    messageText = ""

                                    scope.launch {
                                        try {
                                            user?.id?.let { userId ->
                                                MessengerRepository().sendMessage(userId, messageToSend)
                                            }
                                        } catch (e: Exception) {
                                            // Удаляем сообщение в случае ошибки
                                            messages = messages.filterNot { it.id == newMessage.id }
                                            snackbarHostState.showSnackbar(
                                                message = e.message ?: "Ошибка отправки сообщения"
                                            )
                                        } finally {
                                            isSending = false
                                        }
                                    }


                            }


                        },
                        enabled = !isSending && messageText.isNotBlank()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = date,
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChatMessage(
    message: Message,
    dateFormatter: SimpleDateFormat
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                    )
                )
                .background(
                    if (message.isFromMe)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.isFromMe)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            text = dateFormatter.format(Date(message.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(4.dp)
        )
    }
}