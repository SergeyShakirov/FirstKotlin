package com.example.justdo.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.data.models.UploadState
import com.example.justdo.presentation.viewmodels.ProfileViewModel
import com.example.justdo.ui.theme.TelegramColors
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.content.FileProvider

// Константа для максимального количества фотографий
private const val MAX_PHOTOS = 5

@Composable
fun ProfileScreen(
    userRepository: UserRepository,
    onLogout: () -> Unit,
) {
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(userRepository)
    )

    val uploadState by profileViewModel.uploadState.collectAsState()
    val userAvatars by profileViewModel.userAvatars.collectAsState()
    val currentUser by profileViewModel.currentUser.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFullScreenAvatar by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoLimitWarning by remember { mutableStateOf(false) }

    // Состояние для всплывающего меню
    var showOptionsMenu by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Объединяем аватарки из хранилища и текущую аватарку пользователя
    val avatarsList = remember(userAvatars, currentUser) {
        val combinedList = mutableListOf<String>()

        // Добавляем текущий аватар пользователя, если он есть
        currentUser?.avatarUrl?.takeIf { it.isNotEmpty() }?.let {
            combinedList.add(it)
        }

        // Добавляем другие аватары из истории, исключая текущий (уже добавлен)
        userAvatars.forEach { avatarUrl ->
            if (avatarUrl.isNotEmpty() && !combinedList.contains(avatarUrl)) {
                combinedList.add(avatarUrl)
            }
        }

        // Ограничиваем количество фото до MAX_PHOTOS
        combinedList.take(MAX_PHOTOS)
    }

    // Индекс текущей аватарки
    var currentAvatarIndex by remember { mutableIntStateOf(0) }

    // Состояние для HorizontalPager
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { if (avatarsList.isEmpty()) 1 else avatarsList.size }
    )

    LaunchedEffect(Unit) {
        profileViewModel.loadUserAvatars()
    }

    LaunchedEffect(uploadState) {
        // Когда загрузка успешно завершена - обновляем список аватарок
        if (uploadState is UploadState.Success) {
            profileViewModel.loadUserAvatars()
        }
    }

    // Синхронизация между пейджером и выбранной аватаркой
    LaunchedEffect(pagerState.currentPage) {
        currentAvatarIndex = pagerState.currentPage
    }

    // Состояние для полноэкранного пейджера
    val fullscreenPagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { if (avatarsList.isEmpty()) 1 else avatarsList.size }
    )

    // Лаунчер для выбора изображения из галереи
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (avatarsList.size < MAX_PHOTOS) {
                selectedImageUri = it
                profileViewModel.uploadAvatar(it, context)
            } else {
                showPhotoLimitWarning = true
            }
        }
    }

    // Лаунчер для камеры
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            if (avatarsList.size < MAX_PHOTOS) {
                selectedImageUri = tempCameraUri
                tempCameraUri?.let {
                    profileViewModel.uploadAvatar(it, context)
                }
            } else {
                showPhotoLimitWarning = true
            }
        }
    }

    // Функция для создания временного файла для камеры
    fun createTempImageUri(): Uri {
        val tempFile = File.createTempFile(
            "camera_photo_",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    // Функция для удаления аватара
    fun deleteCurrentAvatar() {
        if (avatarsList.isNotEmpty() && currentAvatarIndex < avatarsList.size) {
            val avatarToDelete = avatarsList[currentAvatarIndex]
            currentUser?.id?.let { userId ->
                // Раскомментировать, когда будет реализован метод в viewModel
                profileViewModel.deleteAvatar(userId, avatarToDelete)
            }
        }
    }

    // Меню для выбора источника фото
    var showPhotoSourceMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TelegramColors.Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TelegramColors.TopBar)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Замена кнопки выхода на кнопку меню
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = TelegramColors.TextPrimary
                        )
                    }

                    // Всплывающее меню
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(TelegramColors.Surface)
                    ) {
                        // Опция "Добавить фото"
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = TelegramColors.Primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Добавить фото")
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                if (avatarsList.size < MAX_PHOTOS) {
                                    showPhotoSourceMenu = true
                                } else {
                                    showPhotoLimitWarning = true
                                }
                            }
                        )

                        // Опция "Удалить текущее фото"
                        if (avatarsList.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Удалить текущее фото")
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        tint = TelegramColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Выйти")
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Секция с аватаром
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(TelegramColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                if (avatarsList.isNotEmpty()) {
                    // Используем HorizontalPager для прокрутки аватарок
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    scope.launch {
                                        fullscreenPagerState.animateScrollToPage(pagerState.currentPage)
                                    }
                                    showFullScreenAvatar = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Отображаем текущую аватарку
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarsList[page])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Аватар",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Индикатор страниц, если аватарок больше одной
                    if (avatarsList.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(avatarsList.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) TelegramColors.Primary
                                            else TelegramColors.Primary.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                } else {
                    // Заглушка, если нет аватарки
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TelegramColors.AvatarCyan)
                            .clickable { showFullScreenAvatar = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // Имя профиля поверх заглушки
                        Text(
                            text = (currentUser?.username?.firstOrNull() ?: "?").toString().uppercase(),
                            color = TelegramColors.TextPrimary,
                            fontSize = 100.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Счетчик фотографий
                if (avatarsList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Отображаем счетчик только при наличии более одной фотографии
                        if (avatarsList.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${avatarsList.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Показываем индикатор загрузки при необходимости
                if (uploadState is UploadState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TelegramColors.Background.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = TelegramColors.Primary,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(uploadState as UploadState.Loading).progress}%",
                                color = TelegramColors.TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Секция информации о пользователе
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        newUsername = currentUser?.username ?: ""
                        showEditNameDialog = true
                    }
            ) {
                // Информационные элементы в стиле Telegram
                ProfileInfoItem(
                    icon = Icons.Default.Person,
                    title = "Имя пользователя",
                    value = currentUser?.username ?: "Не указано"
                )
            }
        }
    }

    // Диалог изменения имени
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Изменить имя") },
            text = {
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Имя пользователя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newUsername.isNotBlank()) {
                        currentUser?.let { profileViewModel.updateUsername(it.id, newUsername) }
                        showEditNameDialog = false
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditNameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Предупреждение о превышении лимита фотографий
    if (showPhotoLimitWarning) {
        AlertDialog(
            onDismissRequest = { showPhotoLimitWarning = false },
            title = { Text("Превышен лимит фотографий") },
            text = { Text("Вы не можете добавить больше $MAX_PHOTOS фотографий. Удалите одну из существующих фотографий, чтобы добавить новую.") },
            confirmButton = {
                Button(onClick = { showPhotoLimitWarning = false }) {
                    Text("Понятно")
                }
            }
        )
    }

    // Диалоговое окно выбора источника фото
    if (showPhotoSourceMenu) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceMenu = false },
            title = { Text("Выбрать источник фото") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Опция Камера
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceMenu = false
                                tempCameraUri = createTempImageUri()
                                tempCameraUri?.let { cameraLauncher.launch(it) }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Камера",
                            tint = TelegramColors.Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Сделать фото")
                    }

                    HorizontalDivider()

                    // Опция Галерея
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceMenu = false
                                galleryLauncher.launch("image/*")
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Галерея",
                            tint = TelegramColors.Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Выбрать из галереи")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceMenu = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог подтверждения удаления фото
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить фото") },
            text = { Text("Вы уверены, что хотите удалить это фото?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCurrentAvatar()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Полноэкранный просмотр аватарки с рабочим счетчиком
    if (showFullScreenAvatar) {
        // При открытии полноэкранного просмотра синхронизируем состояние пейджера
        LaunchedEffect(showFullScreenAvatar) {
            fullscreenPagerState.animateScrollToPage(pagerState.currentPage)
        }

        Dialog(
            onDismissRequest = { showFullScreenAvatar = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Создаем HorizontalPager для прокрутки изображений
                if (avatarsList.isNotEmpty()) {
                    HorizontalPager(
                        state = fullscreenPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        // Отображаем каждое изображение
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarsList[page])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Аватар",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Счетчик фотографий - теперь использует состояние fullscreenPagerState
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${fullscreenPagerState.currentPage + 1} / ${avatarsList.size}",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Кнопка удаления текущего фото в полноэкранном режиме
                    IconButton(
                        onClick = {
                            showFullScreenAvatar = false
                            showDeleteConfirmDialog = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    // Заглушка если нет аватарки
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TelegramColors.AvatarCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.username?.firstOrNull() ?: "?").toString().uppercase(),
                            color = TelegramColors.TextPrimary,
                            fontSize = 100.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Кнопка закрытия
                IconButton(
                    onClick = { showFullScreenAvatar = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Surface(
        color = TelegramColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TelegramColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TelegramColors.TextSecondary
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TelegramColors.TextPrimary
                )
            }
        }
    }
}