package com.example.justdo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.justdo.ui.theme.TelegramColors

/**
 * Универсальная функция для отображения аватаров в стиле Telegram
 *
 * @param name Имя пользователя для инициалов
 * @param avatarUrl URL аватара (если есть)
 * @param size Размер аватара в dp
 * @param modifier Дополнительные модификаторы (опционально)
 */
@Composable
fun TelegramAvatar(
    name: String,
    avatarUrl: String? = null,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    if (!avatarUrl.isNullOrEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .transformations(CircleCropTransformation())
                .build(),
            contentDescription = "Аватар $name",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        // Генерируем аватар с инициалами в стиле Telegram
        val initial = name.firstOrNull()?.toString() ?: "?"
        val avatarColor = getTelegramAvatarColor(initial)

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.uppercase(),
                color = Color.White,
                fontSize = when {
                    size <= 28.dp -> 12.sp
                    size <= 36.dp -> 16.sp
                    else -> 18.sp
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Возвращает цвет аватара в зависимости от первой буквы имени (как в Telegram)
 */
fun getTelegramAvatarColor(initial: String): Color {
    return when (initial.uppercase()) {
        "А", "Б", "В", "Г", "Д", "A", "B", "C", "D", "E" -> TelegramColors.AvatarRed
        "Е", "Ж", "З", "И", "К", "F", "G", "H", "I", "J" -> TelegramColors.AvatarBlue
        "Л", "М", "Н", "О", "П", "K", "L", "M", "N", "O" -> TelegramColors.AvatarCyan
        "Р", "С", "Т", "У", "Ф", "P", "Q", "R", "S", "T" -> TelegramColors.AvatarPurple
        "Х", "Ц", "Ч", "Ш", "Щ", "U", "V", "W", "X", "Y" -> TelegramColors.AvatarGreen
        else -> TelegramColors.AvatarOrange
    }
}