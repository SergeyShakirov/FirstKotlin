package com.example.justdo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelegramStyleChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Поле ввода на полную ширину
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Сообщение",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            },
            textStyle = TextStyle(
                fontSize = 16.sp
            ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = false,
            maxLines = 4
        )

        // Кнопка отправки
        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSendMessage,
            enabled = value.isNotEmpty(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    color = if (value.isNotEmpty())
                        Color(0xFFD32F2F)
                    else
                        Color.Gray.copy(alpha = 0.3f)
                )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Отправить",
                tint = Color.White
            )
        }
    }
}