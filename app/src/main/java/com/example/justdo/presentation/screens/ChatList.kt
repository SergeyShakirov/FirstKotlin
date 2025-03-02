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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.User
import com.example.justdo.presentation.viewmodels.ChatListViewModel
import com.example.justdo.ui.components.TelegramAvatar
import com.example.justdo.ui.components.TelegramTopBar
import com.example.justdo.ui.theme.TelegramColors
import kotlinx.coroutines.delay

@Composable
fun ChatList(
    currentUser: User,
    onChatClicked: (Chat) -> Unit,
    onAddClicked: () -> Unit,
    viewModel: ChatListViewModel,
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
            FloatingActionButton(
                onClick = { onAddClicked() },
                containerColor = TelegramColors.Primary,
                contentColor = TelegramColors.TextPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить",
                    tint = TelegramColors.TextPrimary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = TelegramColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = chats,
                key = { chat -> chat.id }
            ) { chat ->
                ChatListItem(chat, onChatClicked)
            }
        }
    }
}

@Composable
fun ChatListItem(chat: Chat, onChatClicked: (Chat) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onChatClicked(chat) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар
        TelegramAvatar(
            name = chat.name,
            avatarUrl = chat.avatarUrl,
            size = 50.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Информация о чате
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Имя
            Text(
                text = chat.name.ifEmpty { "Новый чат" },
                color = TelegramColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Последнее сообщение
            Text(
                text = chat.lastMessage.ifEmpty { "Нет сообщений" },
                color = TelegramColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Тут можно добавить время последнего сообщения или непрочитанные
        // Пример для времени последнего сообщения
        if (chat.timestamp > 0) {
            Text(
                text = formatChatTime(chat.timestamp),
                color = TelegramColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// Вспомогательная функция для форматирования времени в стиле Telegram
fun formatChatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val date = java.util.Date(timestamp)
    val calendar = java.util.Calendar.getInstance()
    calendar.time = date

    val today = java.util.Calendar.getInstance()
    val yesterday = java.util.Calendar.getInstance()
    yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1)

    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

    return when {
        calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) -> {
            sdf.format(date)
        }
        calendar.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) -> {
            "Вчера"
        }
        calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) -> {
            java.text.SimpleDateFormat("d MMM", java.util.Locale("ru")).format(date)
        }
        else -> {
            java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale("ru")).format(date)
        }
    }
}