package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.justdo.data.repository.MessengerRepository
import com.example.justdo.data.models.User
import com.example.justdo.data.models.Product
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.navigation.Screen
import com.example.justdo.presentation.screens.products.ProductsScreen
import com.example.justdo.presentation.screens.products.detail.ProductDetailScreen
import com.example.justdo.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MyApp(repository: AuthRepository) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

//    LaunchedEffect(Unit) {
//        while (true) {
//            try {
//                products = repository.products()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            delay(5000)
//        }
//    }

    Scaffold(
        bottomBar = {
            if (isAuthenticated) {
                NavigationBar {
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Products") },
                        label = { Text("Товары") },
                        selected = currentRoute == Screen.Products.route,
                        onClick = {
                            if (currentRoute != Screen.Products.route) {
                                navController.navigate(Screen.Products.route) {
                                    popUpTo(Screen.Products.route) { inclusive = true }
                                }
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Users") },
                        label = { Text("Пользователи") },
                        selected = currentRoute == Screen.Users.route,
                        onClick = {
                            if (currentRoute != Screen.Users.route) {
                                navController.navigate(Screen.Users.route) {
                                    popUpTo(Screen.Users.route) { inclusive = true }
                                }
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Профиль") },
                        label = { Text("Профиль") },
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            if (currentRoute != Screen.Profile.route) {
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(Screen.Profile.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (!isAuthenticated) "login" else Screen.Products.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                AnimatedLoginScreen(
                    repository = repository,
                    onLoginSuccess = { user ->
                        isAuthenticated = true
                        navController.navigate(Screen.Products.route) {
                            popUpTo("login") { inclusive = true }
                        }
                        currentUser = user
                    },
                    onRegisterClick = {
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {
                AnimatedRegisterScreen(
                    repository = repository,
                    onRegisterSuccess = { user ->
                        isAuthenticated = true
                        navController.navigate(Screen.Users.route) {
                            popUpTo(0)
                        }
                        currentUser = user
                    },
                    onBackToLogin = {
                        navController.navigateUp()
                    }
                )
            }

            composable(Screen.Users.route) {
                UserList(
                    repository = repository,
                    onUserClicked = { user ->
                        selectedUser = user
                        navController.navigate(Screen.Chat.route)
                    }
                )
            }

            // Обновленный маршрут для списка товаров
            composable(Screen.Products.route) {
                ProductsScreen(
                    products = products,
                    onProductClick = { productId ->
                        navController.navigate("product_detail/$productId")
                    }
                )
            }

            // Новый маршрут для деталей товара
            composable(
                route = "product_detail/{productId}",
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                    ?: return@composable

                val product = products.find { it.id == productId }
                    ?: return@composable

                ProductDetailScreen(
                    product = product,
                    onBackClick = { navController.navigateUp() },
                    onUserClicked = { user ->
                        selectedUser = user
                        navController.navigate(Screen.Chat.route)
                    },
                    writeSeller = product.seller?.id != currentUser?.id
                )
            }

            composable(Screen.Chat.route) {
                ChatScreen(
                    user = selectedUser,
                    onBack = {
                        selectedUser = null
                        navController.navigateUp()
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    user = currentUser, // Используйте текущего авторизованного пользователя
                    onLogout = {
                        repository.logout()
                        isAuthenticated = false
                        currentUser = null
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    },
                    onCreateProduct = {
                        navController.navigate("create_product")
                    }
                )
            }
            composable("create_product") {
                CreateProductScreen(
                    onBackClick = { navController.navigateUp() },
                    onProductCreate = { newProduct ->
                        scope.launch {
                            try {
                                //repository.addProduct(newProduct)
                                navController.navigate(Screen.Products.route) {
                                    // Очищаем бэкстек до экрана продуктов
                                    popUpTo(Screen.Products.route) {
                                        inclusive = true
                                    }
                                }
                            }catch (e: Exception){
                                snackbarHostState.showSnackbar(
                                    message = e.message ?: "Ошибка создания товара")
                            }

                        }
                        navController.navigateUp()
                    },
                    currentUser = currentUser
                )
            }
        }
    }
}