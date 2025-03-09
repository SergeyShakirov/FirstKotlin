package com.example.justdo.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.data.models.User
import com.example.justdo.presentation.components.MessageItem
import com.example.justdo.presentation.components.TelegramStyleChatInput
import com.example.justdo.presentation.viewmodels.GeoMessageViewModel
import com.example.justdo.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Цвета в стиле Telegram
private val DarkBackground = Color(0xFF121B22)  // Почти черный фон
private val TextWhite = Color.White
private val TextSecondary = Color(0xFF8B9398)  // Серый для вторичного текста

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GeoChatScreen(
    currentUser: User,
    viewModel: GeoMessageViewModel
) {
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentUser) {
        if (currentLocation == null) {
            // Используем временное местоположение для Тбилиси, пока не получим реальное
            viewModel.setCurrentLocation(com.google.android.gms.maps.model.LatLng(41.7151, 44.8271))
        }
    }
    // Группируем сообщения только по дате
    val messagesByDate = remember(messages) {
        messages.groupBy { message ->
            getDateGroup(message.timestamp)
        }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Background,
                shadowElevation = 8.dp
            ) {
                TelegramStyleChatInput(
                    value = messageText,
                    onValueChange = { messageText = it },
                    onSendMessage = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                try {
                                    viewModel.sendGeoMessage(messageText)
                                    messageText = ""
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Ошибка отправки сообщения",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TextWhite
                )
            } else if (messages.isEmpty()) {
                // Показываем сообщение, если нет сообщений в радиусе
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Нет сообщений в вашем радиусе",
                            color = TextWhite,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Отправьте сообщение на карте, чтобы начать общение",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    //contentPadding = PaddingValues(vertical = 8.dp),
                    reverseLayout = true // Показываем последние сообщения внизу
                ) {
                    // Перебираем группы по дате (в обратном порядке для reverseLayout=true)
                    messagesByDate.forEach { (dateGroup, dateMessages) ->
                        // Сортируем сообщения за один день по времени (в обратном порядке для reverseLayout=true)
                        val sortedMessages = dateMessages.sortedByDescending { it.timestamp }

                        // Показываем сообщения для текущей даты
                        items(sortedMessages) { message ->
                            MessageItem(message, currentUser.id == message.senderId)
                        }

                        // Header с датой в стиле Telegram (для reverseLayout=true)
                        stickyHeader {
                            DateDivider(dateGroup)
                        }
                    }
                }
            }
        }
    }
}

// Функция для форматирования даты в группы в стиле Telegram
fun getDateGroup(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    return when {
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
            "Сегодня"
        }

        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> {
            "Вчера"
        }

        else -> {
            val dateFormat = SimpleDateFormat("d MMMM", Locale("ru"))
            dateFormat.format(Date(timestamp))
        }
    }
}

@Composable
fun DateDivider(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .background(
                    color = Color(0xFF1E2C3A),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
