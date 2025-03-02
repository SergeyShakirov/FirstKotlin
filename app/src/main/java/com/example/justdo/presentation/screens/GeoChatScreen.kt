package com.example.justdo.presentation.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.justdo.data.models.GeoMessage
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.GeoMessageRepository
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.presentation.components.WeatherInfoDisplay
import com.example.justdo.presentation.viewmodels.GeoMessageViewModel
import com.example.justdo.ui.theme.AppColors
import com.example.justdo.utils.GeocoderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.justdo.services.OpenWeatherService
import com.example.justdo.services.WeatherData

// Цвета в стиле Telegram
private val DarkBackground = Color(0xFF121B22)  // Почти черный фон
private val MyMessageColor = Color(0xFF2B5278)  // Синий для собственных сообщений
private val OtherMessageColor = Color(0xFF222E3A)  // Темно-серый для чужих сообщений
private val DateBackground = Color(0xFF1E2C3A)  // Фон для дат
private val TextWhite = Color.White
private val TextSecondary = Color(0xFF8B9398)  // Серый для вторичного текста
private val NameColor = Color(0xFF6CB1ED)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GeoChatScreen(
    currentUser: User,
    onNavigateToChats: () -> Unit,
    viewModel: GeoMessageViewModel = viewModel(
        factory = GeoMessageViewModel.Factory(
            GeoMessageRepository(),
            UserRepository(),
            LocalContext.current
        )
    )
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val context = LocalContext.current

    // Состояние для хранения данных о погоде
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var isLoadingWeather by remember { mutableStateOf(true) }

    // Создаем GeocoderHelper для получения названия текущего местоположения
    val geocoderHelper = remember { GeocoderHelper(context) }
    val weatherService = remember { OpenWeatherService() }

    // Состояние для хранения названия текущего местоположения
    var currentLocationName by remember { mutableStateOf("Загрузка...") }

    // Корутинный скоуп для запуска suspend-функций
    val coroutineScope = rememberCoroutineScope()

    // Получаем название текущего местоположения и данные о погоде
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            coroutineScope.launch {
                currentLocationName = geocoderHelper.getLocationName(location)
                // Получаем данные о погоде через OpenWeatherMap API
                isLoadingWeather = true
                try {
                    val weather = weatherService.getWeatherData(location.latitude, location.longitude)
                    weatherData = weather
                } catch (e: Exception) {
                    Log.e("GeoChatScreen", "Ошибка при получении погоды", e)
                } finally {
                    isLoadingWeather = false
                }
            }
        }
    }

    // Обновляем местоположение из GeofencedMessagingMapScreen
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Верхняя панель с названием местоположения и данными о погоде
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Название текущего местоположения с иконкой
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Местоположение",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = currentLocationName,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            WeatherInfoDisplay(weatherData, isLoadingWeather)
        }

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
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
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

@Composable
fun MessageItem(message: GeoMessage, isCurrentUser: Boolean) {
    val context = LocalContext.current
    var showTranslation by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 56.dp else 8.dp,
                end = if (isCurrentUser) 8.dp else 56.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Аватарка (только для чужих сообщений)
        if (!isCurrentUser) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(32.dp)
            ) {
                MessageAvatar(message)
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Колонка с сообщением
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Сообщение с закругленными углами в стиле Telegram
            val shape = when {
                isCurrentUser -> RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp
                )
                else -> RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (isCurrentUser) MyMessageColor else OtherMessageColor,
                        shape = shape
                    )
                    .clip(shape)
                    //.widthIn(max = 260.dp)
            ) {
                // Используем Box для правильного позиционирования времени отправки
                Box(
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 8.dp, // Увеличенный отступ справа для размещения времени
                        top = 3.dp,
                        bottom = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(1.dp)
                    ) {
                        // Имя отправителя (только для чужих сообщений)
                        if (!isCurrentUser) {
                            Text(
                                text = message.senderName,
                                color = NameColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Текст сообщения
                        Text(
                            text = message.text,
                            color = TextWhite,
                            fontSize = 16.sp,
                            //lineHeight = 17.sp,
                            textAlign = if (isCurrentUser) TextAlign.End else TextAlign.Start
                        )
                        //Spacer(modifier = Modifier.width(100.dp))
                        // Время отправки - всегда в правом нижнем углу
                        Text(
                            text = formatMessageTime(message.timestamp),
                            fontSize = 10.sp,
                            color = TextSecondary,
                            //textAlign = TextAlign.End,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageAvatar(message: GeoMessage) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when {
                    message.avatarUrl != null -> Color.Transparent
                    else -> {
                        // Определяем цвет по первой букве имени
                        val initial = message.senderName
                            .firstOrNull()
                            ?.toString()
                            ?.uppercase() ?: "?"
                        when (initial) {
                            "W" -> Color(0xFFF79A38) // Оранжевый как на вашем скриншоте
                            "X" -> Color(0xFFEF5F8B) // Розовый как на вашем скриншоте
                            else -> Color(0xFF5296D5) // Голубой по умолчанию
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (message.avatarUrl != null && message.avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(message.avatarUrl)
                    .crossfade(true)
                    .transformations(CircleCropTransformation())
                    .build(),
                contentDescription = "Аватар ${message.senderName}",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Первая буква имени как инициал
            Text(
                text = message.senderName.firstOrNull()?.toString()?.uppercase() ?: "?",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
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

// Функция для форматирования времени - нужно совпадение с вашим скриншотом
private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}