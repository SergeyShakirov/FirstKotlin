// MyApp.kt
package com.example.justdo.presentation.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.justdo.data.models.Chat
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.navigation.Screen
import kotlinx.coroutines.launch
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.presentation.ChatListViewModel
import com.example.justdo.services.MessageHandler
import com.example.justdo.utils.NotificationHelper
import kotlinx.coroutines.flow.first

@Composable
fun MyApp(
    repository: AuthRepository,
    chatRepository: ChatRepository = ChatRepository(),
    viewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.Factory(chatRepository)
    )
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = context as? Activity
    val navigationRoute = remember { activity?.intent?.getStringExtra("navigation_route") }
    val currentUser by viewModel.currentUser.collectAsState()
    val currentChat by viewModel.currentChat.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageHandler by remember { mutableStateOf<MessageHandler?>(null) }
    val notificationHelper = remember { NotificationHelper(context) }
    val chatId = remember { activity?.intent?.getStringExtra("chat_id") }

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
        viewModel.setIsLoading(true)
        try {
            viewModel.getCurrentUser()
            val user = viewModel.currentUser.first() ?: return@LaunchedEffect

            viewModel.loadChats()
            val loadedChats = viewModel.chats.first { it.isNotEmpty() }

            chatId?.let { id ->
                loadedChats.find { it.id == id }?.let { chat ->
                    viewModel.setCurrentChat(chat)
                }
            }

            val handler = MessageHandler(context, loadedChats, user.id).also {
                it.startListening(chatId ?: "")
            }
            viewModel.updateMessageHandler(handler)

            isAuthenticated = true
            if (!notificationHelper.hasNotificationPermission()) {
                notificationHelper.requestNotificationPermission(permissionLauncher)
            }
        } catch (e: Exception) {
            Log.e("MyApp", "Ошибка при инициализации", e)
        } finally {
            viewModel.setIsLoading(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            messageHandler?.stopListening()
            messageHandler = null
        }
    }

    Scaffold(
        bottomBar = {
            currentUser?.let {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                        NavigationBarItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentRoute == Screen.Chats.route)
                                                Color(0xFFD32F2F).copy(alpha = 0.1f)
                                            else
                                                Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Чаты",
                                        tint = if (currentRoute == Screen.Chats.route)
                                            Color(0xFFD32F2F) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    "Чаты",
                                    color = if (currentRoute == Screen.Chats.route)
                                        Color(0xFFD32F2F) else Color.Gray
                                )
                            },
                            selected = currentRoute == Screen.Chats.route,
                            onClick = {
                                if (currentRoute != Screen.Chats.route) {
                                    navController.navigate(Screen.Chats.route) {
                                        popUpTo(Screen.Chats.route) { inclusive = true }
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFD32F2F),
                                unselectedIconColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )

                        NavigationBarItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentRoute == Screen.Profile.route)
                                                Color(0xFFD32F2F).copy(alpha = 0.1f)
                                            else
                                                Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "Профиль",
                                        tint = if (currentRoute == Screen.Profile.route)
                                            Color(0xFFD32F2F) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    "Профиль",
                                    color = if (currentRoute == Screen.Profile.route)
                                        Color(0xFFD32F2F) else Color.Gray
                                )
                            },
                            selected = currentRoute == Screen.Profile.route,
                            onClick = {
                                if (currentRoute != Screen.Profile.route) {
                                    navController.navigate(Screen.Profile.route) {
                                        popUpTo(Screen.Profile.route) { inclusive = true }
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFD32F2F),
                                unselectedIconColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    snackbarData = data
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = when {
                !isAuthenticated -> "login"
                chatId != null -> Screen.Chat.route
                navigationRoute != null -> navigationRoute
                else -> Screen.Chats.route
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                LoginScreen(
                    isLoading = isLoading,
                    repository = repository,
                    onLoginSuccess = { user ->
                        viewModel.setCurrentUser(user)
                        viewModel.loadChats()
                        isAuthenticated = true
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    repository = repository,
                    onRegisterSuccess = { user ->
                        viewModel.setCurrentUser(user)
                        isAuthenticated = true
                        navController.navigate(Screen.Chats.route) {
                            popUpTo(0)
                        }
                    },
                    onBackToLogin = {
                        navController.navigateUp()
                    }
                )
            }

            composable(Screen.Users.route) {
                currentUser?.let { user ->
                    UserList(viewModel,
                        onUserClicked = { selectedUser ->
                            scope.launch {
                                val chatId = "${user.id}_${selectedUser.id}"
                                val newChat = Chat(
                                    id = chatId,
                                    name = selectedUser.username
                                )
                                viewModel.checkAndAddChatToUsers(newChat, selectedUser)
                                viewModel.setCurrentChat(newChat)
                                navController.navigate(Screen.Chat.route)
                            }
                        }
                    )
                } ?: run {
                    LaunchedEffect(Unit) {
                        navController.navigate("login")
                    }
                }
            }

            composable(Screen.Chats.route) {
                ChatList(
                    chats = chats,
                    onChatClicked = { chat ->
                        scope.launch {
                            viewModel.setCurrentChat(chat)
                            navController.navigate(Screen.Chat.route)
                        }
                    },
                    onAddClicked = {
                        navController.navigate(Screen.Users.route)
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.Chat.route) {
                currentChat?.let { it1 ->
                    ChatScreen(
                        viewModel = viewModel,
                        chat = it1,
                        onBack = {
                            navController.navigate(Screen.Chats.route)
                        }
                    )
                }
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    user = currentUser,
                    onLogout = {
                        scope.launch {
                            repository.logout()
                            viewModel.logout()
                            isAuthenticated = false
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    }
                )
            }
        }
    }
}