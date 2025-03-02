package com.example.justdo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.ui.theme.TelegramColors

/**
 * Универсальная верхняя панель в стиле Telegram
 *
 * @param title Заголовок
 * @param subtitle Подзаголовок (опционально, например "был в сети" и т.д.)
 * @param showBackButton Показывать ли кнопку "Назад" слева
 * @param onBackClick Обработчик нажатия на кнопку "Назад"
 * @param actions Действия справа (опционально)
 * @param avatar Композируемый компонент аватара (опционально)
 * @param leadingIcon Иконка слева (если не задана кнопка "Назад" и аватар)
 * @param onLeadingIconClick Обработчик нажатия на иконку слева
 */
@Composable
fun TelegramTopBar(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = true,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    avatar: @Composable (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: () -> Unit = {}
) {
    Surface(
        color = TelegramColors.TopBar,
        contentColor = TelegramColors.TextPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "Назад" или другая иконка слева
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = TelegramColors.TextPrimary
                    )
                }
            } else if (leadingIcon != null) {
                IconButton(onClick = onLeadingIconClick) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = TelegramColors.TextPrimary
                    )
                }
            }

            // Аватар (если есть)
            if (avatar != null) {
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    avatar()
                }
            }

            // Заголовок и подзаголовок
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    color = TelegramColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = TelegramColors.TextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Действия справа
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

/**
 * Пример использования:
 *
 * TelegramTopBar(
 *     title = "Название чата",
 *     subtitle = "В сети",
 *     showBackButton = true,
 *     onBackClick = { /* обработка назад */ },
 *     avatar = {
 *         TelegramAvatar(name = "Имя пользователя", avatarUrl = null, size = 40.dp)
 *     },
 *     actions = {
 *         IconButton(onClick = { /* поиск */ }) {
 *             Icon(Icons.Default.Search, contentDescription = "Поиск")
 *         }
 *         IconButton(onClick = { /* показать меню */ }) {
 *             Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
 *         }
 *     }
 * )
 */