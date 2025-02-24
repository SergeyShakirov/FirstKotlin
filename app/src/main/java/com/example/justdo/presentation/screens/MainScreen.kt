package com.example.justdo.presentation.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.navigation.Screen
import kotlinx.coroutines.launch
import com.example.justdo.data.repository.ChatRepository
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.presentation.ChatListViewModel
import com.example.justdo.utils.NotificationHelper
import kotlinx.coroutines.flow.first

@Composable
fun MyApp(
    repository: AuthRepository,
    chatRepository: ChatRepository = ChatRepository(),
    userRepository: UserRepository = UserRepository(),
    viewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.Factory(chatRepository, userRepository)
    )
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = context as? Activity
    val currentUser by viewModel.currentUser.collectAsState()
    val currentChat by viewModel.currentChat.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val notificationHelper = remember { NotificationHelper(context) }
    val chatId = remember { activity?.intent?.getStringExtra("chatId") }

    val pagerState = rememberPagerState(pageCount = { 3 })

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
            isAuthenticated = true

            viewModel.loadUserChats(user.id)
            val loadedChats = viewModel.chats.first { it.isNotEmpty() }

            chatId?.let { id ->
                loadedChats.find { it.id == id }?.let { chat ->
                    viewModel.setCurrentChat(chat)
                    viewModel.currentChat.first { it?.id == chat.id }
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Chats.route) { inclusive = true }
                    }
                }
            }

            if (!notificationHelper.hasNotificationPermission()) {
                notificationHelper.requestNotificationPermission(permissionLauncher)
            }
        } catch (e: Exception) {
            Log.e("MyApp", "Ошибка при инициализации", e)
        } finally {
            viewModel.setIsLoading(false)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        currentUser?.let {
            when (pagerState.currentPage) {
                0 -> pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                1 -> pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                2 -> pagerState.animateScrollToPage(
                    page = 1,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    Scaffold(
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
                !isAuthenticated -> ("login")
                chatId != null -> Screen.Chat.route
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
                                viewModel.getChat(selectedUser.id, user.id)?.let { chat ->
                                    viewModel.setCurrentChat(chat)
                                } ?: run {
                                    viewModel.createChat(selectedUser.id, user.id).let { chat ->
                                        if (chat != null) {
                                            viewModel.setCurrentChat(chat)
                                            navController.navigate(Screen.Chat.route)
                                        } else {
                                            navController.navigate(Screen.Chats.route)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            composable(Screen.Chats.route) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 0.dp,
                ) { page ->
                    when (page) {
                        0 -> currentUser?.let {
                            MapScreen(
                                onBack = {
                                    navController.navigate(Screen.Chats.route)
                                }
                            )
                        }

                        1 -> currentUser?.let { user ->
                            ChatList(
                                currentUser = user,
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
                        2 -> ProfileScreen(
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
                            },
                            onAvatarSelected = { uri ->
                                viewModel.uploadAvatar(uri)
                            },
                            onMapClicked = {
                                navController.navigate(Screen.Map.route)
                            }
                        )
                    }
                }
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
//            composable(Screen.Map.route) {
//                MapScreen(
//                    onBack = {
//                        navController.navigate(Screen.Chats.route)
//                    }
//                )
//            }
        }
    }
}