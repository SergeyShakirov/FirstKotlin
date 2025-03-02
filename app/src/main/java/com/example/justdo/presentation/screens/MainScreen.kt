package com.example.justdo.presentation.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.data.repository.GeoMessageRepository
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.navigation.Screen
import com.example.justdo.presentation.viewmodels.ChatListViewModel
import com.example.justdo.presentation.viewmodels.GeoMessageViewModel
import com.example.justdo.presentation.viewmodels.LoginViewModel
import com.example.justdo.ui.components.BottomNavItem
import com.example.justdo.ui.components.TelegramBottomNavigation
import com.example.justdo.ui.theme.JustDoTheme
import com.example.justdo.ui.theme.TelegramColors
import com.example.justdo.utils.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MyApp(
    repository: AuthRepository,
    chatRepository: ChatRepository = ChatRepository(),
    userRepository: UserRepository = UserRepository(),
    initialUser: User,
    onLogout: () -> Unit
) {

    // Создаем общие ViewModel
    val chatViewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.Factory(chatRepository, userRepository)
    )

    // Создаем LoginViewModel
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory(repository)
    )

    // Создание GeoMessageViewModel с контекстом для Geocoder
    val geoMessageViewModel: GeoMessageViewModel = viewModel(
        factory = GeoMessageViewModel.Factory(
            GeoMessageRepository(),
            UserRepository(),
            LocalContext.current
        )
    )

    var isAuthenticated by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = context as? Activity
    val currentUser by chatViewModel.currentUser.collectAsState()
    val currentChat by chatViewModel.currentChat.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val notificationHelper = remember { NotificationHelper(context) }
    val chatId = remember { activity?.intent?.getStringExtra("chatId") }

    // Отслеживаем текущий выбранный элемент в навигации
    var selectedItem by remember { mutableStateOf(0) } // По умолчанию карта

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MyApp", "Разрешение на уведомления получено")
        } else {
            Log.d("MyApp", "Разрешение на уведомления отклонено")
        }
    }

    // При изменении пользователя в ChatViewModel обновляем его в GeoMessageViewModel
    LaunchedEffect(initialUser) {
        geoMessageViewModel.setCurrentUser(initialUser)
        chatViewModel.setCurrentUser(initialUser)
    }

    LaunchedEffect(Unit) {
        chatViewModel.setIsLoading(true)
        try {
//            chatViewModel.getCurrentUser()
//            val user = chatViewModel.currentUser.first() ?: return@LaunchedEffect
//            isAuthenticated = true

            // Устанавливаем пользователя в оба ViewModel
//            geoMessageViewModel.setCurrentUser(user)
//
//            chatViewModel.loadUserChats(user.id)


            chatId?.let { id ->
                val loadedChats = chatViewModel.chats.first { it.isNotEmpty() }
                loadedChats.find { it.id == id }?.let { chat ->
                    chatViewModel.setCurrentChat(chat)
                    chatViewModel.currentChat.first { it?.id == chat.id }
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Map.route) { inclusive = true }
                    }
                }
            }

            if (!notificationHelper.hasNotificationPermission()) {
                notificationHelper.requestNotificationPermission(permissionLauncher)
            }
        } catch (e: Exception) {
            Log.e("MyApp", "Ошибка при инициализации", e)
        } finally {
            chatViewModel.setIsLoading(false)
        }
    }

    // Применяем тему Telegram
    JustDoTheme(forceTelegramStyle = true) {
        Scaffold(
            containerColor = TelegramColors.Background,
            bottomBar = {
                TelegramBottomNavigation(
                    selectedItem = selectedItem,
                    onItemSelected = { index ->
                        selectedItem = index
                        when (index) {
                            0 -> {
                                // Всегда переходить на экран карты с полной очисткой стека навигации
                                navController.navigate("map_tab") {
                                    // Очищаем весь стек навигации до корня
                                    popUpTo(navController.graph.id) {
                                        inclusive = true
                                    }
                                }
                            }

                            1 -> {
                                // Переход на экран геочата с очисткой стека
                                navController.navigate("chats") {
                                    popUpTo(navController.graph.id) {
                                        inclusive = true
                                    }
                                }
                            }

                            2 -> {
                                // Переход на экран профиля с очисткой стека
                                navController.navigate("profile_tab") {
                                    popUpTo(navController.graph.id) {
                                        inclusive = true
                                    }
                                }
                            }
                        }
                    },
                    navController = navController,
                    items = listOf(
                        BottomNavItem(Icons.Default.Map, "map_tab"),
                        BottomNavItem(
                            Icons.Default.Forum,
                            "chats"
                        ), // Исправлено с geoChat_tab на geochat_tab
                        BottomNavItem(Icons.Default.Person, "profile_tab")
                    )
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        containerColor = TelegramColors.DateBadge,
                        contentColor = TelegramColors.TextPrimary,
                        snackbarData = data
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination =
                when {
                    chatId != null -> Screen.Chat.route
                    else -> "map_tab" // Карта как стартовый экран
                },
                modifier = Modifier.padding(paddingValues)
            ) {
                // Логин и регистрация
                composable("login") {
                    LoginScreen(
                        viewModel = loginViewModel, // Передаем ViewModel вместо repository
                        onLoginSuccess = {
                            // Получаем пользователя из ViewModel
                            val user = loginViewModel.uiState.value.user
                            if (user != null) {
                                chatViewModel.setCurrentUser(user)
                                isAuthenticated = true
                            }
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        repository = repository,
                        onRegisterSuccess = { user ->
                            chatViewModel.setCurrentUser(user)
                            isAuthenticated = true
                        },
                        onBackToLogin = {
                            navController.navigateUp()
                    }
                )
            }

            // Вкладки нижней навигации
            composable("map_tab") {
                currentUser?.let {
                    // Передаем ViewModel для гео-сообщений
                    GeofencedMessagingMapScreen(
                        geoMessageViewModel = geoMessageViewModel,
                        onClickChat = {
                            navController.navigate("geochat_tab")
                        }
                    )
                }
            }

            composable("geochat_tab") {
                currentUser?.let { user ->
                    // Передаем ViewModel для гео-сообщений
                    GeoChatScreen(
                        currentUser = user,
                        viewModel = geoMessageViewModel,
                        onNavigateToChats = {
                            navController.navigate(Screen.Chats.route)
                        }
                    )
                }
            }

            composable("profile_tab") {
                ProfileScreen(
                    user = currentUser,
                    onLogout = {
                        scope.launch {
                            repository.logout()
                            chatViewModel.logout()
                            geoMessageViewModel.setCurrentUser(null)
                            isAuthenticated = false
                            onLogout()
//                                navController.navigate("login") {
//                                    popUpTo(0)
//                                }
                        }
                    },
                    onAvatarSelected = { uri ->
                        chatViewModel.uploadAvatar(uri)
                    },
                    onMapClicked = {
                        selectedItem = 0
                        navController.navigate("map_tab")
                    },
                    onNavigateToChats = {
                        navController.navigate(Screen.Chats.route)
                    }
                )
            }

            composable("chats") {
                currentUser?.let { user ->
                    ChatList(
                        currentUser = user,
                        onChatClicked = { chat ->
                            scope.launch {
                                chatViewModel.setCurrentChat(chat)
                                navController.navigate(Screen.Chat.route)
                            }
                        },
                        onAddClicked = {
                            navController.navigate(Screen.Users.route)
                        },
                        viewModel = chatViewModel
                    )
                }
            }

            // Другие экраны
            composable(Screen.Users.route) {
                currentUser?.let { user ->
                    UserList(chatViewModel,
                        onUserClicked = { selectedUser ->
                            scope.launch {
                                chatViewModel.getChat(selectedUser.id, user.id)?.let { chat ->
                                    chatViewModel.setCurrentChat(chat)
                                    navController.navigate(Screen.Chat.route)
                                } ?: run {
                                    chatViewModel.createChat(selectedUser.id, user.id).let { chat ->
                                        if (chat != null) {
                                            chatViewModel.setCurrentChat(chat)
                                            navController.navigate(Screen.Chat.route)
                                        } else {
                                            navController.navigate(Screen.Chats.route)
                                        }
                                    }
                                }
                            }
                        },
                        onBackPressed = {
                            navController.navigate(Screen.Chats.route)
                        }
                    )
                }
            }

            composable(Screen.Chat.route) {
                currentChat?.let { chat ->
                    ChatScreen(
                        viewModel = chatViewModel,
                        chat = chat,
                        onBack = {
                            navController.navigate(Screen.Chats.route)
                        }
                    )
                }
            }

            // Добавляем отдельный маршрут для GeoChat
            composable(Screen.GeoChat.route) {
                currentUser?.let { user ->
                    GeoChatScreen(
                        currentUser = user,
                        viewModel = geoMessageViewModel,
                        onNavigateToChats = {
                            navController.navigate(Screen.Chats.route)
                        }
                    )
                }
            }
        }
        }
    }
}