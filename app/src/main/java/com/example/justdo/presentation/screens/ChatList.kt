package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.User
import com.example.justdo.presentation.viewmodels.ChatListViewModel
import com.example.justdo.ui.components.TelegramAvatar
import com.example.justdo.ui.theme.TelegramColors
import kotlinx.coroutines.delay

@Composable
fun ChatList(
    currentUser: User,
    onChatClicked: (Chat) -> Unit,
    onAddClicked: () -> Unit,
    onProfileClicked: () -> Unit,
    viewModel: ChatListViewModel,
) {
    val chats by viewModel.chats.collectAsState()
    var showProfilePopup by remember { mutableStateOf(false) }
    var showFullScreenAvatar by remember { mutableStateOf(false) }
    var userForFullscreenAvatar by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadUserChats(currentUser.id)
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            // Верхняя панель с профилем пользователя
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TelegramColors.TopBar,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Аватарка пользователя (кликабельная)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                userForFullscreenAvatar = currentUser
                                showProfilePopup = true
                            }
                    ) {
                        TelegramAvatar(
                            name = currentUser.username,
                            avatarUrl = currentUser.avatarUrl,
                            size = 40.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Имя пользователя (также кликабельное)
                    Text(
                        text = currentUser.username.ifEmpty { "Пользователь" },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                userForFullscreenAvatar = currentUser
                                showProfilePopup = true
                            },
                        color = TelegramColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
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

    // Всплывающее окно профиля
    if (showProfilePopup && userForFullscreenAvatar != null) {
        ProfilePopup(
            user = userForFullscreenAvatar!!,
            onDismiss = { showProfilePopup = false },
            onProfileClicked = {
                showProfilePopup = false
                onProfileClicked()
            },
            onFullscreenAvatarRequest = { user ->
                // Здесь обрабатываем запрос на просмотр аватарки в полный экран
                showProfilePopup = false
                showFullScreenAvatar = true
            }
        )
    }

    // Полноэкранный просмотр аватарки
    if (showFullScreenAvatar && userForFullscreenAvatar != null) {
        FullscreenAvatarView(
            user = userForFullscreenAvatar!!,
            onDismiss = { showFullScreenAvatar = false }
        )
    }
}

@Composable
fun ProfilePopup(
    user: User,
    onDismiss: () -> Unit,
    onProfileClicked: () -> Unit,
    onFullscreenAvatarRequest: (User) -> Unit
) {
    // Используем BoxWithConstraints для получения доступных размеров экрана
    BoxWithConstraints {
        // Получаем доступные размеры экрана
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // Рассчитываем адаптивные размеры для диалога
        val dialogWidth = minOf(screenWidth * 0.9f, 500.dp)
        val maxDialogHeight = screenHeight * 0.85f // Максимум 85% от высоты экрана

        // Рассчитываем максимальный размер аватарки, чтобы оставалось место для кнопки
        val avatarMaxSize = if (screenHeight > 800.dp) 500.dp else minOf(screenWidth * 0.7f, screenHeight * 0.5f)

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(dialogWidth)
                    .heightIn(max = maxDialogHeight) // Ограничиваем максимальную высоту
                    .wrapContentHeight() // Позволяем контенту определять высоту
                    .padding(vertical = 8.dp), // Небольшой отступ от краев экрана
                shape = RoundedCornerShape(30.dp),
                color = TelegramColors.Surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween // Равномерное распределение элементов
                ) {
                    // Аватарка с адаптивным размером
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .sizeIn(maxWidth = avatarMaxSize, maxHeight = avatarMaxSize) // Максимальный размер
                            .aspectRatio(1f) // Сохраняем квадратное соотношение
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { onFullscreenAvatarRequest(user) }
                    ) {
                        if (user.avatarUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(user.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Аватар пользователя",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(TelegramColors.AvatarCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                // Адаптивный размер текста
                                val fontSize = if (screenWidth > 600.dp) 72.sp else 56.sp

                                Text(
                                    text = (user.username.firstOrNull() ?: "?").toString().uppercase(),
                                    color = TelegramColors.TextPrimary,
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Добавим информацию о пользователе
//                    Text(
//                        text = user.username.ifEmpty { "Пользователь" },
//                        color = TelegramColors.TextPrimary,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = if (screenWidth > 600.dp) 22.sp else 18.sp,
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.padding(vertical = 8.dp)
//                    )

                    // Кнопка перехода в профиль
                    Button(
                        onClick = onProfileClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TelegramColors.Primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Перейти в профиль",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (screenWidth > 600.dp) 18.sp else 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Перейти",
                                tint = Color.White
                            )
                        }
                    }

                    // Добавляем дополнительный отступ снизу для большей адаптивности
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun FullscreenAvatarView(
    user: User,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Аватар пользователя",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(TelegramColors.AvatarCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user.username.firstOrNull() ?: "?").toString().uppercase(),
                        color = TelegramColors.TextPrimary,
                        fontSize = 150.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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