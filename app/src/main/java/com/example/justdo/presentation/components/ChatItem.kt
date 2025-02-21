package com.example.justdo.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.justdo.data.models.Chat

@Composable
fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Заменяем Box на AsyncImage с fallback на букву
            if (chat.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(chat.avatarUrl)
                        .crossfade(true)
                        .transformations(CircleCropTransformation())
                        .build(),
                    contentDescription = "Аватар пользователя ${chat.name}",
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.name.isNotEmpty()) chat.name.first().uppercase() else "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = chat.name.ifEmpty { "Новый чат" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = chat.lastMessage.ifEmpty { "Нет сообщений" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}