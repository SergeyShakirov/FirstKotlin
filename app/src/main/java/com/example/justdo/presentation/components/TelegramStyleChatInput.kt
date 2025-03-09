package com.example.justdo.presentation.components

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.ui.theme.TelegramColors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TelegramStyleChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachImage: (String) -> Unit = {},
    onAttachFile: (String) -> Unit = {},
    onAttachLocation: () -> Unit = {},
    onVoiceMessageSent: (ByteArray) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Состояние для управления UI
    var isRecording by remember { mutableStateOf(false) }
    var recordingTimeSeconds by remember { mutableStateOf(0) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showEmojiKeyboard by remember { mutableStateOf(false) }

    // Цвет фона для панели эмодзи
    val emojiBackgroundColor = Color(0xFFF0F2F5)

    // Разрешение на запись аудио
    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Начинаем запись, если разрешение получено
            isRecording = true
            recordingTimeSeconds = 0

            // Имитация записи (инкрементируем таймер)
            coroutineScope.launch {
                while (isRecording) {
                    delay(1000)
                    recordingTimeSeconds++
                }
            }
        }
    }

    // Лаунчеры для вложений
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onAttachImage(it.toString())
            showAttachmentMenu = false
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onAttachFile(it.toString())
            showAttachmentMenu = false
        }
    }

    Surface(
        color = TelegramColors.Surface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Меню вложений
            AnimatedVisibility(
                visible = showAttachmentMenu,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Кнопка выбора изображения
                    AttachmentButton(
                        icon = Icons.Outlined.Photo,
                        label = "Фото",
                        backgroundColor = Color(0xFF5E35B1),
                        onClick = { imagePickerLauncher.launch("image/*") }
                    )

                    // Кнопка выбора файла
                    AttachmentButton(
                        icon = Icons.Outlined.Folder,
                        label = "Документ",
                        backgroundColor = Color(0xFF00897B),
                        onClick = { filePickerLauncher.launch("*/*") }
                    )

                    // Кнопка выбора местоположения
                    AttachmentButton(
                        icon = Icons.Outlined.LocationOn,
                        label = "Локация",
                        backgroundColor = Color(0xFFF9A825),
                        onClick = {
                            onAttachLocation()
                            showAttachmentMenu = false
                        }
                    )
                }
            }

            // Панель эмодзи (упрощенная имитация)
            AnimatedVisibility(
                visible = showEmojiKeyboard,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(emojiBackgroundColor)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Список популярных эмодзи
                    val commonEmojis = listOf("😊", "👍", "🙏", "❤️", "👏", "😂", "🔥", "🎉")
                    commonEmojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    onValueChange(value + emoji)
                                }
                        )
                    }
                }
            }

            // Основная строка ввода
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка вложения файла
                IconButton(
                    onClick = {
                        showAttachmentMenu = !showAttachmentMenu
                        showEmojiKeyboard = false
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Прикрепить",
                        tint = if (showAttachmentMenu) TelegramColors.Primary else TelegramColors.TextSecondary
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
                        if (isRecording) {
                            // Отображение статуса записи голосового сообщения
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Запись",
                                    tint = Color.Red,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = formatRecordingTime(recordingTimeSeconds),
                                    color = TelegramColors.TextPrimary
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "Свайпните влево для отмены",
                                    color = TelegramColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            // Поле ввода текста
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
                        }

                        // Кнопка эмодзи
                        if (!isRecording) {
                            IconButton(
                                onClick = {
                                    showEmojiKeyboard = !showEmojiKeyboard
                                    showAttachmentMenu = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SentimentSatisfied,
                                    contentDescription = "Эмодзи",
                                    tint = if (showEmojiKeyboard) TelegramColors.Primary else TelegramColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Кнопка отправки сообщения или записи голосового
                Box(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (value.isNotEmpty()) {
                        // Кнопка отправки текстового сообщения
                        IconButton(
                            onClick = {
                                onSendMessage()
                                showEmojiKeyboard = false
                                showAttachmentMenu = false
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(TelegramColors.Primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Отправить",
                                tint = Color.White
                            )
                        }
                    } else if (isRecording) {
                        // Кнопка остановки записи
                        IconButton(
                            onClick = {
                                isRecording = false

                                // Имитируем отправку голосового сообщения
                                if (recordingTimeSeconds > 0) {
                                    // В реальном приложении здесь бы отправлялись реальные аудиоданные
                                    onVoiceMessageSent(ByteArray(0))
                                }
                                recordingTimeSeconds = 0
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Red, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Остановить запись",
                                tint = Color.White
                            )
                        }
                    } else {
                        // Кнопка начала записи голосового сообщения
                        IconButton(
                            onClick = {
                                // Запрашиваем разрешение на запись аудио
                                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                showEmojiKeyboard = false
                                showAttachmentMenu = false
                            },
                            modifier = Modifier
                                .size(40.dp)
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
}

@Composable
private fun AttachmentButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(backgroundColor, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = TelegramColors.TextSecondary
        )
    }
}

// Функция для форматирования времени записи
private fun formatRecordingTime(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}