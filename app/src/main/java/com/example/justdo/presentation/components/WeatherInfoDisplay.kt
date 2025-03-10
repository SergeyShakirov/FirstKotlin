package com.example.justdo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.services.WeatherData

// Определяем цвета здесь, чтобы избежать ошибок
val TextWhite = Color.White

@Composable
fun WeatherInfoDisplay(weatherData: WeatherData?, isLoading: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                color = Color(0xFF1E2C3A).copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        // Иконка погоды (солнце/облака)
        val weatherDescription = weatherData?.description ?: ""
        val cloudiness = weatherData?.cloudiness?.replace("%", "")?.toIntOrNull() ?: 0

        val iconVector = when {
            isLoading -> Icons.Default.WbCloudy
            weatherDescription.contains("ясно") ||
                    weatherDescription.contains("солнечно") ||
                    weatherDescription.contains("clear") ||
                    cloudiness < 30 -> Icons.Default.WbSunny
            weatherDescription.contains("облач") ||
                    weatherDescription.contains("пасмурно") ||
                    weatherDescription.contains("cloud") ||
                    cloudiness >= 70 -> Icons.Default.Cloud
            else -> Icons.Default.WbCloudy // Переменная облачность
        }

        val iconTint = when(iconVector) {
            Icons.Default.WbSunny -> Color(0xFFFFD700) // Золотистый для солнца
            else -> Color(0xFFE0E0E0) // Светло-серый для облаков
        }

        Icon(
            imageVector = iconVector,
            contentDescription = "Погода",
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )

        // Температура
        Text(
            text = if (isLoading || weatherData == null) "--°C" else weatherData.temperature,
            color = TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // Вертикальный разделитель (очень тонкий)
        HorizontalDivider(
            modifier = Modifier
                .height(10.dp)
                .width(1.dp),
            color = TextWhite.copy(alpha = 0.3f)
        )

        // Ветер
        Icon(
            imageVector = Icons.Outlined.Air,
            contentDescription = "Ветер",
            tint = TextWhite,
            modifier = Modifier.size(12.dp)
        )

        Text(
            text = if (isLoading || weatherData == null) "--" else weatherData.windSpeed,
            color = TextWhite,
            fontSize = 12.sp
        )
    }
}