package com.example.justdo.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.data.models.GeoMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun MapMessage(
    message: GeoMessage,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {

    // Генерируем стабильный цвет для имени на основе ID пользователя
    val nameColor = remember(message.senderId) {
        // Создаем хеш-код из ID пользователя
        val hash = message.senderId.hashCode()

        // Список ярких цветов, хорошо видимых на темном фоне
        val baseColors = listOf(
            Color(0xFF00BCD4), // Бирюзовый
            Color(0xFF4CAF50), // Зеленый
            Color(0xFFFFEB3B), // Желтый
            // ...и другие яркие цвета
        )

        // Выбираем цвет на основе хеша
        val colorIndex = abs(hash) % baseColors.size
        baseColors[colorIndex]
    }

    Column(
        modifier = modifier.alpha(alpha)
    ) {
        Text(
            text = message.senderName,
            color = nameColor,
            fontSize = 16.sp, // Увеличиваем размер шрифта
            fontWeight = FontWeight.ExtraBold, // Делаем шрифт очень жирным
            letterSpacing = 0.5.sp // Немного увеличиваем межбуквенный интервал для лучшей читаемости
        )

        // Текст сообщения - более крупный и жирный шрифт
        Text(
            text = message.text,
            color = Color.White,
            fontSize = 16.sp, // Увеличиваем размер шрифта
            fontWeight = FontWeight.Bold, // Делаем шрифт жирным
            lineHeight = 20.sp, // Увеличиваем межстрочный интервал для лучшей читаемости
            modifier = Modifier.padding(top = 4.dp) // Увеличиваем отступ между именем и текстом
        )
    }
}