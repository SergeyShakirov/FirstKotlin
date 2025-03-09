package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.User
import com.example.justdo.presentation.viewmodels.ChatListViewModel
import com.example.justdo.ui.components.TelegramAvatar
import com.example.justdo.ui.components.TelegramTopBar
import com.example.justdo.ui.theme.TelegramColors

@Composable
fun UserList(
    viewModel: ChatListViewModel,
    onUserClicked: (User) -> Unit,
    onBackPressed: () -> Unit = {} // Добавляем опциональный колбэк для кнопки "Назад"
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Состояние для поиска
    var searchQuery by remember { mutableStateOf("") }

    // Фильтрация пользователей по запросу
    val filteredUsers = if (searchQuery.isBlank()) {
        users
    } else {
        users.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                    (it.email?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TelegramTopBar(
                title = "Пользователи",
                showBackButton = true,
                onBackClick = onBackPressed,
                actions = {
                    IconButton(onClick = { /* Функция поиска */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = TelegramColors.TextPrimary
                        )
                    }
                }
            )
        },
        containerColor = TelegramColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center),
                    color = TelegramColors.Primary
                )
            } else if (users.isEmpty()) {
                // Показываем сообщение, если список пуст
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Нет доступных пользователей",
                        color = TelegramColors.TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredUsers) { user ->
                        TelegramUserListItem(
                            user = user,
                            onClick = { onUserClicked(user) }
                        )

                        // Тонкий разделитель между элементами
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp), // Отступ слева для выравнивания с текстом
                            thickness = 0.5.dp,
                            color = TelegramColors.Surface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TelegramUserListItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар
        TelegramAvatar(
            name = user.username,
            avatarUrl = user.avatarUrl,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Информация о пользователе
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Имя пользователя
            Text(
                text = user.username,
                color = TelegramColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )

            // Email или другая дополнительная информация
            user.email?.let {
                Text(
                    text = it,
                    color = TelegramColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Опционально: индикатор онлайн-статуса
        if (user.online) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = TelegramColors.Online, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

// Если в модели User нет поля isOnline, его можно добавить или использовать другую логику
// для определения, онлайн ли пользователь