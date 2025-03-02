package com.example.justdo.presentation.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.LatLng
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.maps.android.compose.*
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import android.content.Context
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.justdo.data.models.GeoMessage
import com.example.justdo.ui.theme.AppColors
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.delay

interface MessageRepository {
    fun sendMessage(message: GeoMessage)
    suspend fun getNearbyMessages(location: LatLng, radiusMeters: Double): List<GeoMessage>
}

class InMemoryMessageRepository : MessageRepository {
    private val messages = mutableListOf<GeoMessage>()

    override fun sendMessage(message: GeoMessage) {
        messages.add(message)
    }

    override suspend fun getNearbyMessages(location: LatLng, radiusMeters: Double): List<GeoMessage> {
        return messages.filter { message ->
            isInRadius(location, message.location, radiusMeters)
        }
    }

    private fun isInRadius(point1: LatLng, point2: LatLng, radiusMeters: Double): Boolean {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0] <= radiusMeters
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GeofencedMessagingMapScreen() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val georgia = LatLng(41.7151, 44.8271)
    val radiusMeters = 500.0

    // Создаем in-memory репозиторий для демонстрации
    val messageRepository = remember { InMemoryMessageRepository() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(georgia, 15f)
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var nearbyMessages by remember { mutableStateOf<List<GeoMessage>>(emptyList()) }
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
                        currentLocation = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLocation!!, 15f)
                    }
                }
            } catch (e: SecurityException) {
                // Обработка ошибки
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

    // Периодически проверяем сообщения поблизости
    LaunchedEffect(currentLocation) {
        while (true) {
            currentLocation?.let { location ->
                nearbyMessages = messageRepository.getNearbyMessages(location, radiusMeters)
            }
            delay(10000) // Проверяем каждые 10 секунд
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
                    fontWeight = FontWeight.Bold
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
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = AppColors.Background
                ) {
                    // Используем Box для полного контроля над размещением элементов
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(AppColors.Surface)
                    ) {
                        // Базовое текстовое поле без встроенных отступов и декораций
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 52.dp)
                                .align(Alignment.CenterStart),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = AppColors.TextPrimary
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(AppColors.Primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // Показываем плейсхолдер только если текстовое поле пустое
                                    if (messageText.isEmpty()) {
                                        Text(
                                            "Сообщение",
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Кнопка отправки
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .background(AppColors.Primary, CircleShape)
                                .clickable {
                                    if (messageText.isNotBlank() && currentLocation != null) {
                                        val newMessage = GeoMessage(
                                            text = messageText,
                                            location = currentLocation!!,
                                            senderName = "Вы",
                                            radiusMeters = radiusMeters
                                        )
                                        messageRepository.sendMessage(newMessage)
                                        messageText = ""
                                        Toast.makeText(
                                            context,
                                            "Сообщение отправлено",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Отправить",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                        zoomControlsEnabled = true,
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
                            if (message.senderName != "Вы") {
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

                // Стилизованный индикатор количества сообщений
                if (nearbyMessages.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .shadow(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.Primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Сообщений поблизости: ${nearbyMessages.size}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
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
                        shape = RoundedCornerShape(16.dp)
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
                                    fontWeight = FontWeight.Medium
                                )
                                Button(
                                    onClick = { locationPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.Primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Разрешить доступ",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (!isLocationEnabled) {
                                Text(
                                    "Необходимо включить геолокацию в настройках устройства",
                                    textAlign = TextAlign.Center,
                                    color = AppColors.TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.Primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Открыть настройки",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}