package com.example.justdo.presentation.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import android.content.Context
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.justdo.data.models.GeoMessage
import com.example.justdo.presentation.viewmodels.GeoMessageViewModel
import com.example.justdo.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.justdo.presentation.components.MapMessageOverlay
import com.example.justdo.presentation.components.TelegramStyleChatInput
import com.example.justdo.ui.theme.TelegramColors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GeofencedMessagingMapScreen(
    geoMessageViewModel: GeoMessageViewModel,
    onClickChat: () -> Unit
)  {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val georgia = LatLng(41.7151, 44.8271)
    val radiusMeters = 500.0

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(georgia, 15f)
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    val nearbyMessages by geoMessageViewModel.messages.collectAsState()
    val currentUser by geoMessageViewModel.currentUser.collectAsState()
    val isLoading by geoMessageViewModel.isLoading.collectAsState()

    // Для плавного скрытия индикатора сообщений
    var shouldShowMessageCount by remember { mutableStateOf(true) }
    var lastMessageCount by remember { mutableIntStateOf(-1) }

    // Анимация для плавного скрытия
    val messageCountAlpha by animateFloatAsState(
        targetValue = if (shouldShowMessageCount) 1f else 0f,
        animationSpec = tween(durationMillis = 1000), label = ""
    )

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Обновляем время последнего взаимодействия при отправке сообщения
    val updateInteractionTime = {
        lastInteractionTime = System.currentTimeMillis()
    }

    // Отслеживаем изменение количества сообщений
    LaunchedEffect(nearbyMessages.size) {
        if (nearbyMessages.size != lastMessageCount) {
            lastMessageCount = nearbyMessages.size
            shouldShowMessageCount = true

            // Если количество сообщений не изменилось в течение 5 секунд, скрываем индикатор
            delay(5000)
            shouldShowMessageCount = false
        }
    }

    var showMessageDialog by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<GeoMessage?>(null) }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Функция для проверки состояния геолокации и обновления UI
    val checkLocationAndUpdateUI = {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (locationPermissionState.status.isGranted && isLocationEnabled) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val newLocation = LatLng(location.latitude, location.longitude)
                        currentLocation = newLocation
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(newLocation, 15f)

                        // Обновляем местоположение в ViewModel
                        geoMessageViewModel.setCurrentLocation(newLocation)
                    }
                }
            } catch (e: SecurityException) {
                // Обработка ошибки
                Log.e("GeofencedMessagingMapScreen", "Ошибка доступа к местоположению", e)
            }
        }
    }

    // Первоначальная проверка
    LaunchedEffect(Unit) {
        checkLocationAndUpdateUI()

        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Наблюдение за жизненным циклом приложения
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Проверяем статус геолокации при возвращении в приложение
                checkLocationAndUpdateUI()
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Отслеживаем изменения разрешений
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            checkLocationAndUpdateUI()
        }
    }

    // Диалог для просмотра сообщения в новом стиле
    if (showMessageDialog && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            containerColor = AppColors.Background,
            title = {
                Text(
                    "Сообщение от ${selectedMessage!!.senderName}",
                    color = AppColors.Primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Text(
                    selectedMessage!!.text,
                    color = AppColors.TextPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = { showMessageDialog = false }) {
                    Text(
                        "Закрыть",
                        color = AppColors.Primary
                    )
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (locationPermissionState.status.isGranted && isLocationEnabled && currentLocation != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Background,
                    shadowElevation = 8.dp
                ) {
                    TelegramStyleChatInput(
                        value = messageText,
                        onValueChange = { messageText = it },
                        onSendMessage = {
                            if (messageText.isNotBlank() && currentLocation != null && currentUser != null) {
                                scope.launch {
                                    try {
                                        // Обновляем время последнего взаимодействия
                                        updateInteractionTime()

                                        geoMessageViewModel.sendGeoMessage(messageText)

                                        messageText = ""

                                    } catch (e: Exception) {
                                        Log.e("GeofencedScreen", "Ошибка отправки сообщения", e)
                                        Toast.makeText(context, "Ошибка отправки сообщения", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (locationPermissionState.status.isGranted && isLocationEnabled) {

                // Стилизованная Google карта
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        zoomControlsEnabled = false, // Отключаем стандартные кнопки зума
                        scrollGesturesEnabled = true
                    )
                ) {
                    // Отображаем радиус вокруг текущего местоположения
                    currentLocation?.let { location ->
                        Circle(
                            center = location,
                            radius = radiusMeters,
                            fillColor = AppColors.AccentLight,
                            strokeColor = AppColors.Accent,
                            strokeWidth = 2f
                        )

                        // Отображаем маркеры сообщений поблизости
                        nearbyMessages.forEach { message ->
                            // Не показываем собственные сообщения маркерами
                            if (currentUser == null || message.senderId != currentUser!!.id) {
                                Marker(
                                    state = MarkerState(position = message.location),
                                    title = "Сообщение от ${message.senderName}",
                                    snippet = "Нажмите, чтобы прочитать",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                    onClick = {
                                        selectedMessage = message
                                        showMessageDialog = true
                                        true
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onClickChat()
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    shape = CircleShape,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    // Используем иконку чата или стрелки назад
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat, // Замените на подходящую иконку
                        contentDescription = "Перейти в чат",
                        tint = Color.White
                    )
                }

                // Кастомные кнопки зума (справа по центру)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка увеличения
                    FloatingActionButton(
                        onClick = {
                            // Увеличиваем зум карты на 1 уровень
                            val currentZoom = cameraPositionState.position.zoom
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                cameraPositionState.position.target,
                                currentZoom + 1f
                            )
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = TelegramColors.Primary,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    // Кнопка уменьшения
                    FloatingActionButton(
                        onClick = {
                            // Уменьшаем зум карты на 1 уровень
                            val currentZoom = cameraPositionState.position.zoom
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                cameraPositionState.position.target,
                                (currentZoom - 1f).coerceAtLeast(1f) // Минимальный зум 1
                            )
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = TelegramColors.Primary,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "−", // Используем специальный знак минуса
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }

                // Индикатор загрузки
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center),
                        color = AppColors.Primary
                    )
                }
            } else {
                // Стилизованный экран с просьбой о разрешениях
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .shadow(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.Surface
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            if (!locationPermissionState.status.isGranted) {
                                Text(
                                    "Для использования карты необходимо разрешить доступ к местоположению",
                                    textAlign = TextAlign.Center,
                                    color = AppColors.TextPrimary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                Button(
                                    onClick = { locationPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.Primary
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Разрешить доступ",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                            }

                            if (!isLocationEnabled) {
                                Text(
                                    "Необходимо включить геолокацию в настройках устройства",
                                    textAlign = TextAlign.Center,
                                    color = AppColors.TextPrimary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.Primary
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Открыть настройки",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (currentUser != null) {
            MapMessageOverlay(
                messages = nearbyMessages,
                currentUserId = currentUser!!.id,
                onMessageDismiss = {}
            )
        }
    }
}