package com.example.justdo.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.data.models.User
import com.example.justdo.domain.models.UploadState
import com.example.justdo.ui.components.TelegramAvatar
import com.example.justdo.ui.theme.TelegramColors

@Composable
fun ProfileScreen(
    user: User?,
    uploadState: UploadState = UploadState.Idle,
    onLogout: () -> Unit,
    onAvatarSelected: (Uri) -> Unit,
    onMapClicked: () -> Unit,
    onNavigateToChats: () -> Unit = {} // Новый параметр для навигации к чатам
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            onAvatarSelected(it)
        }
    }

    Scaffold(
        containerColor = TelegramColors.Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TelegramColors.TopBar)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Выйти",
                        tint = TelegramColors.TextPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Секция с аватаром и именем пользователя
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TelegramColors.Surface)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Аватар с возможностью загрузки нового
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable(enabled = uploadState !is UploadState.Loading) {
                                launcher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Используем компонент TelegramAvatar или крайний вариант
                        if (selectedImageUri != null || user?.avatarUrl != null) {
                            TelegramAvatar(
                                name = user?.username ?: "Пользователь",
                                avatarUrl = (selectedImageUri ?: user?.avatarUrl)?.toString(),
                                size = 100.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(TelegramColors.AvatarCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user?.username?.firstOrNull() ?: "?").toString().uppercase(),
                                    color = TelegramColors.TextPrimary,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Показываем индикатор загрузки при необходимости
                        if (uploadState is UploadState.Loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(TelegramColors.Background.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = TelegramColors.Primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Имя пользователя
                    Text(
                        text = user?.username ?: "Гость",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TelegramColors.TextPrimary
                    )

                    // Статус (можно добавить в модель пользователя)
                    Text(
                        text = "онлайн",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TelegramColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Секция информации о пользователе
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Информационные элементы в стиле Telegram
                ProfileInfoItem(
                    icon = Icons.Default.Person,
                    title = "Имя пользователя",
                    value = user?.username ?: "Не указано"
                )

                ProfileInfoItem(
                    icon = Icons.Default.Email,
                    title = "Email",
                    value = user?.email ?: "Не указано"
                )

                // Дополнительные настройки профиля
                ProfileSettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления и звуки",
                    onClick = { /* Действие */ }
                )

                ProfileSettingItem(
                    icon = Icons.Default.Lock,
                    title = "Конфиденциальность",
                    onClick = { /* Действие */ }
                )

                ProfileSettingItem(
                    icon = Icons.Default.Storage,
                    title = "Данные и хранилище",
                    onClick = { /* Действие */ }
                )

                ProfileSettingItem(
                    icon = Icons.Default.Refresh,
                    title = "Обновить геоданные",
                    onClick = { onMapClicked() }
                )
            }
        }
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Surface(
        color = TelegramColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TelegramColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TelegramColors.TextSecondary
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TelegramColors.TextPrimary
                )
            }
        }
    }
}

@Composable
fun ProfileSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        color = TelegramColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = TelegramColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Название настройки
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TelegramColors.TextPrimary
                )
            }

            // Стрелка вправо
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TelegramColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}