package com.example.justdo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.justdo.data.models.GeoMessage

@Composable
fun MessageAvatar(message: MessageData) {
    // Генерируем цвет аватара на основе ID отправителя
    val avatarColor = when (message.senderId.hashCode() % 5) {
        0 -> Color(0xFF4CAF50)
        1 -> Color(0xFF2196F3)
        2 -> Color(0xFFFFC107)
        3 -> Color(0xFFE91E63)
        else -> Color(0xFF9C27B0)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.senderName.first().toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}