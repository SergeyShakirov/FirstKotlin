package com.example.justdo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.data.models.GeoMessage
import com.example.justdo.data.models.Message
import com.example.justdo.ui.theme.TelegramColors.NameColor
import com.example.justdo.ui.theme.TelegramColors.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MyMessageColor = Color(0xFF2B5278)  // Синий для собственных сообщений
private val OtherMessageColor = Color(0xFF222E3A)  // Темно-серый для чужих сообщений

// Интерфейс для общих свойств разных типов сообщений
interface MessageData {
    val senderId: String
    val senderName: String
    val text: String
    val timestamp: Long
}

// Расширения для приведения разных типов сообщений к общему интерфейсу
fun GeoMessage.toMessageData(): MessageData = object : MessageData {
    override val senderId: String = this@toMessageData.senderId
    override val senderName: String = this@toMessageData.senderName
    override val text: String = this@toMessageData.text
    override val timestamp: Long = this@toMessageData.timestamp
}

fun Message.toMessageData(): MessageData = object : MessageData {
    override val senderId: String = this@toMessageData.senderId
    override val senderName: String = this@toMessageData.senderName.toString()
    override val text: String = this@toMessageData.text
    override val timestamp: Long = this@toMessageData.timestamp
}

// Функция для форматирования времени
private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MessageItem(message: Any, isCurrentUser: Boolean) {

    // Преобразуем сообщение в общий интерфейс MessageData
    val messageData: MessageData = when (message) {
        is GeoMessage -> message.toMessageData()
        is Message -> message.toMessageData()
        else -> return // Если передан неподдерживаемый тип, просто выходим
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 48.dp else 8.dp,
                end = if (isCurrentUser) 8.dp else 48.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Аватарка для чужих сообщений (слева внизу)
        if (!isCurrentUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = (-4).dp) // Небольшой отрицательный отступ для лучшего соединения с облаком
            ) {
                MessageAvatar(messageData)
            }
        }
        // Облако сообщения
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(4.dp)
        ) {
            // Форма облака сообщения
            val bubbleShape = if (isCurrentUser) {
                RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)
            }
            // Содержимое сообщения
            Box(
                modifier = Modifier
                    .background(
                        color = if (isCurrentUser) MyMessageColor else OtherMessageColor,
                        shape = bubbleShape
                    )
                    .clip(bubbleShape)
                    .padding(10.dp, 5.dp, 10.dp)
            ) {
                Column {
                    if (!isCurrentUser) {
                        Text(
                            text = messageData.senderName,
                            color = NameColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Box {
                        Text(
                            text = messageData.text,
                            color = TextWhite,
                            fontSize = 15.sp,
                        )
                    }
                    Text(
                        text = formatMessageTime(messageData.timestamp),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .align(Alignment.End)
                    )
                }
            }
        }
    }
}

