package com.example.justdo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.ArrowBack

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(item: Item?, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Прокрутка к последнему сообщению
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        SmallTopAppBar(
            title = { Text(text = item?.name ?: "") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatMessage(
                    message = message,
                    dateFormatter = dateFormatter
                )
            }
        }

        // Message Input
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
                        if (messageText.isNotBlank()) {
                            messages = messages + Message(
                                content = messageText,
                                isFromMe = true
                            )
                            messageText = ""
                            // Здесь можно добавить отправку сообщения на сервер
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
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