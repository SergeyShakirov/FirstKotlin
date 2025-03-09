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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.data.repository.GeoMessageRepository
import com.example.justdo.navigation.Screen
import com.example.justdo.presentation.viewmodels.ChatListViewModel
import com.example.justdo.presentation.viewmodels.GeoMessageViewModel
import com.example.justdo.presentation.viewmodels.ProfileViewModel
import com.example.justdo.ui.components.BottomNavItem
import com.example.justdo.ui.components.TelegramBottomNavigation
import com.example.justdo.ui.theme.TelegramColors
import com.example.justdo.utils.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MyApp(
    userRepository: UserRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val chatRepository = remember { ChatRepository() }

    val chatViewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.Factory(chatRepository, userRepository)
    )

    val geoMessageViewModel: GeoMessageViewModel = viewModel(
        factory = GeoMessageViewModel.Factory(
            userRepository,
            GeoMessageRepository(),
            context
        )
    )
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val activity = context as? Activity
    val chatId = remember { activity?.intent?.getStringExtra("chatId") }

    val currentUser by userRepository.currentUser.collectAsState()
    val currentChat by chatViewModel.currentChat.collectAsState()

    val notificationHelper = remember { NotificationHelper(context) }

    // Отслеживаем текущий выбранный элемент в навигации
    var selectedItem by remember { mutableIntStateOf(0) } // По умолчанию карта

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MyApp", "Разрешение на уведомления получено")
        } else {
            Log.d("MyApp", "Разрешение на уведомления отклонено")
        }
    }

    LaunchedEffect(Unit) {
//        geoMessageViewModel.setCurrentUser(initialUser)
//        chatViewModel.setCurrentUser(initialUser)

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
    }

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
//                        2 -> {
//                            // Переход на экран профиля с очисткой стека
//                            navController.navigate("profile_tab") {
//                                popUpTo(navController.graph.id) {
//                                    inclusive = true
//                                }
//                            }
//                        }
                    }
                }, navController = navController, items = listOf(
                    BottomNavItem(Icons.Default.Map, "map_tab"), BottomNavItem(
                        Icons.Default.Forum, "chats"
                    )
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
            }, modifier = Modifier.padding(paddingValues)
        ) {
            composable("map_tab") {
                GeofencedMessagingMapScreen(
                    geoMessageViewModel = geoMessageViewModel,
                    onClickChat = {
                        navController.navigate("geochat_tab")
                    }
                )
            }

            composable("geochat_tab") {
                GeoChatScreen(
                    currentUser = currentUser!!,
                    viewModel = geoMessageViewModel,
                )
            }

            composable("profile_tab") {
                ProfileScreen(
                    userRepository = userRepository,
                    onLogout = { onLogout() },
                )
            }

            composable("chats") {
                ChatList(
                    currentUser = currentUser!!,
                    onChatClicked = { chat ->
                        scope.launch {
                            chatViewModel.setCurrentChat(chat)
                            navController.navigate(Screen.Chat.route)
                        }
                    },
                    onAddClicked = {
                        navController.navigate(Screen.Users.route)
                    },
                    onProfileClicked = {navController.navigate("profile_tab")},
                    viewModel = chatViewModel
                )
            }
            composable(Screen.Users.route) {
                UserList(chatViewModel,
                    onUserClicked = { selectedUser ->
                        scope.launch {
                            var chat = chatViewModel.getChat(selectedUser.id)
                            if (chat != null) {
                                chat = chatViewModel.updateChatWithUserData(chat)
                                chatViewModel.setCurrentChat(chat)
                                navController.navigate(Screen.Chat.route)
                            } else {
                                chat = chatViewModel.createChat(selectedUser.id)
                                if (chat != null) {
                                    chat = chatViewModel.updateChatWithUserData(chat)
                                    chatViewModel.setCurrentChat(chat)
                                    navController.navigate(Screen.Chat.route)
                                } else {
                                    navController.navigate(Screen.Chats.route)
                                }
                            }
                        }
                    },
                    onBackPressed = {
                        navController.navigate(Screen.Chats.route)
                    }
                )
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
        }
    }
}