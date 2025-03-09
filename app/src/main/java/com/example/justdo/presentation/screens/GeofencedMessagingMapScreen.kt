package com.example.justdo.presentation.screens

import android.content.Intent
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
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import android.content.Context
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.justdo.data.models.GeoMessage
import com.example.justdo.presentation.viewmodels.GeoMessageViewModel
import com.example.justdo.ui.theme.TelegramColors
import kotlinx.coroutines.delay
import com.example.justdo.presentation.components.MapMessageOverlay
import kotlin.math.max
import kotlin.math.min

enum class MenuOption {
    TRANSPORT, NANNY, WALK, OTHER, NONE
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GeofencedMessagingMapScreen(
    geoMessageViewModel: GeoMessageViewModel,
    onClickChat: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val georgia = LatLng(41.7151, 44.8271)
    val radiusMeters = 500.0

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(georgia, 15f)
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    val nearbyMessages by geoMessageViewModel.messages.collectAsState()
    val currentUser by geoMessageViewModel.currentUser.collectAsState()
    val isLoading by geoMessageViewModel.isLoading.collectAsState()

    // Состояния для меню
    var selectedOption by remember { mutableStateOf(MenuOption.NONE) }
    var addressText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    // Состояние для выдвигающейся панели
    var isBottomSheetExpanded by remember { mutableStateOf(false) }

    // Анимация моргающей стрелки
    var arrowAlpha by remember { mutableStateOf(1f) }

    // Запускаем анимацию моргания только когда меню не развернуто
    LaunchedEffect(isBottomSheetExpanded) {
        if (!isBottomSheetExpanded) {
            while (true) {
                // Более выраженная анимация
                arrowAlpha = 1f
                delay(3500) // Уменьшаем задержку для более быстрой анимации
                arrowAlpha = 0.2f
                delay(3500)
            }
        }
    }

    // Для плавного скрытия индикатора сообщений
    var shouldShowMessageCount by remember { mutableStateOf(true) }
    var lastMessageCount by remember { mutableIntStateOf(-1) }

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

    if (showMessageDialog && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            containerColor = TelegramColors.Surface,
            title = {
                Text(
                    "Сообщение от ${selectedMessage!!.senderName}",
                    color = TelegramColors.NameColor,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Text(
                    selectedMessage!!.text,
                    color = TelegramColors.TextPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = { showMessageDialog = false }) {
                    Text(
                        "Закрыть",
                        color = TelegramColors.Primary
                    )
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Основной контент
        if (locationPermissionState.status.isGranted && isLocationEnabled) {
            // Стилизованная Google карта
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapStyleOptions = MapStyleOptions(
                        """
                        [
                          {
                            "featureType": "all",
                            "elementType": "geometry",
                            "stylers": [
                              { "color": "#242f3e" }
                            ]
                          },
                          {
                            "featureType": "all",
                            "elementType": "labels.text.stroke",
                            "stylers": [
                              { "color": "#242f3e" },
                              { "lightness": -80 }
                            ]
                          },
                          {
                            "featureType": "all",
                            "elementType": "labels.text.fill",
                            "stylers": [
                              { "color": "#746855" },
                              { "lightness": 20 }
                            ]
                          },
                          {
                            "featureType": "water",
                            "elementType": "geometry",
                            "stylers": [
                              { "color": "#17263c" }
                            ]
                          }
                        ]
                        """
                    )
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false, // Отключаем стандартную кнопку локации
                    zoomControlsEnabled = false, // Отключаем стандартные кнопки зума
                    scrollGesturesEnabled = true
                )
            ) {
                // Отображаем радиус вокруг текущего местоположения
                currentLocation?.let { location ->
                    Circle(
                        center = location,
                        radius = radiusMeters,
                        fillColor = TelegramColors.Primary.copy(alpha = 0.15f),
                        strokeColor = TelegramColors.Primary,
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

            // Кнопка чата с бейджем для количества сообщений
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                FloatingActionButton(
                    onClick = { onClickChat() },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    containerColor = TelegramColors.Primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Перейти в чат"
                    )
                }

                // Бейдж с количеством новых сообщений
                if (shouldShowMessageCount && nearbyMessages.isNotEmpty()) {
                    val count = nearbyMessages.size
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(22.dp)
                            .background(Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (count > 99) "99+" else count.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }

            // Колонка справа для кнопок управления картой
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

                // Кнопка определения местоположения
                FloatingActionButton(
                    onClick = { checkLocationAndUpdateUI() },
                    modifier = Modifier.size(48.dp),
                    containerColor = TelegramColors.Primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Моё местоположение"
                    )
                }
            }

            // Индикатор загрузки
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center),
                    color = TelegramColors.Primary
                )
            }
        } else {
            // Стилизованный экран с просьбой о разрешениях
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TelegramColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .shadow(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TelegramColors.Surface
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
                                color = TelegramColors.TextPrimary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Button(
                                onClick = { locationPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TelegramColors.Primary
                                ),
                                shape = RoundedCornerShape(8.dp)
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
                                color = TelegramColors.TextPrimary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TelegramColors.Primary
                                ),
                                shape = RoundedCornerShape(8.dp)
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

        // MapMessageOverlay в верхней части экрана
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopCenter)
//                .padding(top = 16.dp)
//                .zIndex(1f)
//        ) {
//            MapMessageOverlay(
//                messages = nearbyMessages,
//                currentUserId = currentUser?.id ?: "",
//                onMessageDismiss = {}
//            )
//        }

        // Нижнее выдвигающееся меню
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Стрелка над меню
            if (!isBottomSheetExpanded) {
                // Анимация перемещения вверх-вниз
                val offsetY by animateFloatAsState(
                    targetValue = if (arrowAlpha > 0.5f) -24f else 0f,
                    animationSpec = tween(durationMillis = 600),
                    label = "arrowOffsetY"
                )

                // Большая стрелка над меню
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-12).dp + offsetY.dp)
                        .size(120.dp, 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Потяните вверх",
                        tint = TelegramColors.Primary,
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(arrowAlpha)
                    )
                }
            }

            BottomSheetContainer(
                isExpanded = isBottomSheetExpanded,
                onExpandToggle = { isBottomSheetExpanded = it },
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption = it },
                addressText = addressText,
                onAddressChange = { addressText = it },
                priceText = priceText,
                onPriceChange = { priceText = it },
                descriptionText = descriptionText,
                onDescriptionChange = { descriptionText = it },
                onSubmit = {
                    // Здесь логика отправки заказа
                    selectedOption = MenuOption.NONE
                    addressText = ""
                    priceText = ""
                    descriptionText = ""
                    isBottomSheetExpanded = false
                }
            )
        }
    }
}

@Composable
fun BottomSheetContainer(
    isExpanded: Boolean,
    onExpandToggle: (Boolean) -> Unit,
    selectedOption: MenuOption,
    onOptionSelected: (MenuOption) -> Unit,
    addressText: String,
    onAddressChange: (String) -> Unit,
    priceText: String,
    onPriceChange: (String) -> Unit,
    descriptionText: String,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val density = LocalDensity.current

    // Состояние для измерения высоты содержимого
    var contentHeight by remember { mutableStateOf(0.dp) }

    // Минимальные высоты
    val collapsedHeight = 45.dp
    val initialExpandedHeight = 180.dp
    val minExpandedHeight = 180.dp

    // Отслеживаем, находится ли пользователь в процессе перетаскивания
    var isDragging by remember { mutableStateOf(false) }

    // Состояние для пользовательской высоты
    var userDraggedHeight by remember { mutableStateOf<Dp?>(null) }

    // Сбрасываем userDraggedHeight при смене опции меню или при открытии меню
    LaunchedEffect(selectedOption, isExpanded) {
        // Даем немного времени для измерения контента
        delay(100)

        // Сбрасываем пользовательскую высоту при изменении опции или при открытии меню
        if (selectedOption != MenuOption.NONE || isExpanded) {
            userDraggedHeight = null
        }
    }

    // Вычисляем целевую высоту
    val targetHeight = if (isExpanded) {
        when {
            // Если пользователь перетаскивает - используем высоту перетаскивания
            isDragging && userDraggedHeight != null -> userDraggedHeight!!

            // Если опция не выбрана, показываем только меню опций
            selectedOption == MenuOption.NONE -> initialExpandedHeight

            // Если опция выбрана и контент измерен, используем высоту контента + отступы
            contentHeight > 0.dp -> contentHeight + 80.dp

            // Запасной вариант - минимальная высота развернутого меню
            else -> minExpandedHeight
        }
    } else {
        // В свернутом состоянии всегда используем фиксированную высоту
        collapsedHeight
    }

    // Анимация изменения высоты
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(
            durationMillis = 300, // Используем более плавную анимацию (300мс)
            easing = FastOutSlowInEasing
        ),
        label = "height"
    )

    // Анимация моргающей стрелки
    var arrowAlpha by remember { mutableStateOf(1f) }

    // Запускаем анимацию моргания только когда меню не развернуто
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            while (true) {
                // Мигаем стрелкой, меняя прозрачность
                arrowAlpha = 1f
                delay(800) // Задержка в видимом состоянии
                arrowAlpha = 0.3f
                delay(800) // Задержка в полупрозрачном состоянии
            }
        }
    }

    // Состояние для драга (перетаскивания)
    val draggableState = rememberDraggableState { delta ->
        if (isExpanded) {
            isDragging = true
            // Получаем текущую высоту в пикселях
            val currentHeightPx = with(density) {
                (userDraggedHeight ?: animatedHeight).toPx()
            }

            // Вычисляем новую высоту (отрицательное delta, т.к. drag вверх = увеличение высоты)
            val newHeightPx = currentHeightPx - delta

            // Ограничиваем высоту между минимальным и максимальным значением
            val minHeightPx = with(density) { minExpandedHeight.toPx() }
            val maxHeightPx = with(density) { 600.dp.toPx() } // Максимум 600dp

            val clampedHeightPx = newHeightPx.coerceIn(minHeightPx, maxHeightPx)

            // Конвертируем обратно в Dp и сохраняем
            userDraggedHeight = with(density) { clampedHeightPx.toDp() }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clickable {
                // Сбрасываем пользовательскую высоту при переключении состояния
                if (!isExpanded) {
                    userDraggedHeight = null
                }
                onExpandToggle(!isExpanded)
            }
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                enabled = isExpanded,
                onDragStarted = { isDragging = true },
                onDragStopped = { isDragging = false }
            ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = TelegramColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Полоса-индикатор (также служит для перетаскивания)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TelegramColors.TextSecondary.copy(alpha = 0.7f))
            )

            // Содержимое меню (отображается только если развернуто)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) +
                        expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(200)) +
                        shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .verticalScroll(rememberScrollState())
                        .onGloballyPositioned { coordinates ->
                            // Измеряем и обновляем высоту контента только когда не перетаскиваем
                            if (!isDragging) {
                                contentHeight = with(density) { coordinates.size.height.toDp() }
                            }
                        }
                ) {
                    // Используем простую сетку из нескольких Row для опций с автопереносом
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                            //.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Первый ряд опций
                        Row(
                            //modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MenuOptionItem(
                                text = "Такси",
                                isSelected = selectedOption == MenuOption.TRANSPORT,
                                onClick = {
                                    onOptionSelected(if (selectedOption == MenuOption.TRANSPORT) MenuOption.NONE else MenuOption.TRANSPORT)
                                },
                                //modifier = Modifier.weight(1f, fill = false)
                            )

                            //Spacer(modifier = Modifier.width(8.dp))

                            MenuOptionItem(
                                text = "Няня, сиделка",
                                isSelected = selectedOption == MenuOption.NANNY,
                                onClick = {
                                    onOptionSelected(if (selectedOption == MenuOption.NANNY) MenuOption.NONE else MenuOption.NANNY)
                                },
                                //modifier = Modifier.weight(1f, fill = false)
                            )

                            MenuOptionItem(
                                text = "Выгулить собаку",
                                isSelected = selectedOption == MenuOption.WALK,
                                onClick = {
                                    onOptionSelected(if (selectedOption == MenuOption.WALK) MenuOption.NONE else MenuOption.WALK)
                                },
                                modifier = Modifier.weight(1f, fill = false)
                            )

                        }

                        // Второй ряд опций
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MenuOptionItem(
                                text = "Прочее",
                                isSelected = selectedOption == MenuOption.OTHER,
                                onClick = {
                                    onOptionSelected(if (selectedOption == MenuOption.OTHER) MenuOption.NONE else MenuOption.OTHER)
                                },
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Содержимое на основе выбранной опции
                    AnimatedVisibility(
                        visible = selectedOption != MenuOption.NONE,
                        enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                fadeIn(animationSpec = tween(300)),
                        exit = shrinkVertically(animationSpec = tween(200)) +
                                fadeOut(animationSpec = tween(200))
                    ) {
                        when (selectedOption) {
                            MenuOption.TRANSPORT -> TransportForm(
                                address = addressText,
                                onAddressChange = onAddressChange,
                                description = descriptionText,
                                onDescriptionChange = onDescriptionChange,
                                price = priceText,
                                onPriceChange = onPriceChange,
                                onSubmit = onSubmit
                            )

                            MenuOption.NANNY, MenuOption.WALK -> DescriptionForm(
                                description = descriptionText,
                                onDescriptionChange = onDescriptionChange,
                                onSubmit = onSubmit,
                                title = if (selectedOption == MenuOption.NANNY) "Няня" else "Выгул"
                            )

                            MenuOption.OTHER -> OtherForm(
                                description = descriptionText,
                                onDescriptionChange = onDescriptionChange,
                                onSubmit = onSubmit
                            )

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TelegramColors.Primary else TelegramColors.DateBadge,
        ),
        border = if (isSelected) null else BorderStroke(1.dp, TelegramColors.TextSecondary),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else TelegramColors.TextPrimary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransportForm(
    address: String,
    onAddressChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Адрес", color = TelegramColors.TextHint) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TelegramColors.Primary,
                focusedLabelColor = TelegramColors.Primary,
                unfocusedBorderColor = TelegramColors.TextSecondary,
                cursorColor = TelegramColors.Primary,
                focusedTextColor = TelegramColors.TextPrimary,
                unfocusedTextColor = TelegramColors.TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Описание", color = TelegramColors.TextHint) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TelegramColors.Primary,
                focusedLabelColor = TelegramColors.Primary,
                unfocusedBorderColor = TelegramColors.TextSecondary,
                cursorColor = TelegramColors.Primary,
                focusedTextColor = TelegramColors.TextPrimary,
                unfocusedTextColor = TelegramColors.TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            label = { Text("Цена", color = TelegramColors.TextHint) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TelegramColors.Primary,
                focusedLabelColor = TelegramColors.Primary,
                unfocusedBorderColor = TelegramColors.TextSecondary,
                cursorColor = TelegramColors.Primary,
                focusedTextColor = TelegramColors.TextPrimary,
                unfocusedTextColor = TelegramColors.TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = TelegramColors.Primary
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Заказать",
                modifier = Modifier.padding(vertical = 8.dp),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

@Composable
fun DescriptionForm(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    title: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Заказ: $title",
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = TelegramColors.NameColor,
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Описание", color = TelegramColors.TextHint) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TelegramColors.Primary,
                focusedLabelColor = TelegramColors.Primary,
                unfocusedBorderColor = TelegramColors.TextSecondary,
                cursorColor = TelegramColors.Primary,
                focusedTextColor = TelegramColors.TextPrimary,
                unfocusedTextColor = TelegramColors.TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = TelegramColors.Primary
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Заказать",
                modifier = Modifier.padding(vertical = 8.dp),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

@Composable
fun OtherForm(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Прочее",
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = TelegramColors.NameColor,
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Опишите вашу задачу", color = TelegramColors.TextHint) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TelegramColors.Primary,
                focusedLabelColor = TelegramColors.Primary,
                unfocusedBorderColor = TelegramColors.TextSecondary,
                cursorColor = TelegramColors.Primary,
                focusedTextColor = TelegramColors.TextPrimary,
                unfocusedTextColor = TelegramColors.TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 8,
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = TelegramColors.Primary
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Отправить заявку",
                modifier = Modifier.padding(vertical = 8.dp),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}