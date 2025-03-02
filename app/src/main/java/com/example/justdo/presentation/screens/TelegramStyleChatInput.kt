package com.example.justdo.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.ui.theme.TelegramColors

@Composable
fun TelegramStyleChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFocused = remember { mutableStateOf(false) }

    Surface(
        color = TelegramColors.Surface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка вложения файла (исправлено: используем AttachFile вместо Attach)
            IconButton(
                onClick = { /* Функция вложения файла */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Прикрепить",
                    tint = TelegramColors.TextSecondary
                )
            }

            // Поле ввода сообщения
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TelegramColors.DateBadge)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { newValue ->
                            onValueChange(newValue)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        textStyle = TextStyle(
                            color = TelegramColors.TextPrimary,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(TelegramColors.Primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrect = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = "Сообщение",
                                        color = TelegramColors.TextHint,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Кнопка эмодзи
                    IconButton(
                        onClick = { /* Функция вызова эмодзи-клавиатуры */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfied,
                            contentDescription = "Эмодзи",
                            tint = TelegramColors.TextSecondary
                        )
                    }
                }
            }

            // Кнопка отправки или микрофона (исправлено: используем Box с условиями вместо AnimatedVisibility)
            Box(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // Исправлено: используем условия вместо AnimatedVisibility
                if (value.isNotEmpty()) {
                    // Кнопка отправки
                    IconButton(
                        onClick = onSendMessage,
                        modifier = Modifier
                            .size(40.dp)
                            .background(TelegramColors.Primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Отправить",
                            tint = TelegramColors.TextPrimary
                        )
                    }
                } else {
                    // Кнопка голосового сообщения
                    IconButton(
                        onClick = { /* Функция записи голосового сообщения */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Голосовое сообщение",
                            tint = TelegramColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}