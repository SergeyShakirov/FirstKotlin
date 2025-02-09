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
import com.example.justdo.data.MessengerRepository
import com.example.justdo.data.User
import com.example.justdo.navigation.Screen
import com.example.justdo.utils.SessionManager

@Composable
fun MyApp(repository: MessengerRepository) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var products = listOf(
        Product(
            id = 1,
            name = "Смарт-часы",
            price = 199.99,
            description = "Современные смарт-часы с множеством функций для отслеживания активности и здоровья.",
            seller = User(
                id = "49bf4187-e38c-11ef-acda-98fa9b5804bb",
                name = "Сергей"
            )
        ),
        Product(
            id = 2,
            name = "Bluetooth-наушники",
            price = 129.99,
            description = "Беспроводные наушники с активным шумоподавлением и кристально чистым звуком.",
            seller = User(
                id = "49c05ca2-e5f0-11ef-acdc-98fa9b5804bb",
                name = "Милаша"
            )
        )
    )

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
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = { user ->
                        isAuthenticated = true
                        navController.navigate(Screen.Products.route) {
                            popUpTo("login") { inclusive = true }
                        }
                        currentUser = user
                    },
                    onRegisterClick = {
                        isRegistering = true
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    repository = repository,
                    onRegisterSuccess = { user ->
                        isAuthenticated = true
                        isRegistering = false
                        navController.navigate(Screen.Users.route) {
                            popUpTo(0)
                        }
                        currentUser = user
                    },
                    onBackToLogin = {
                        isRegistering = false
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
                    navArgument("productId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getInt("productId")
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
                        sessionManager.clearCredentials()
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
                        // Логика добавления товара (например, в репозиторий)
                        products = products + newProduct
                        navController.navigateUp()
                    },
                    currentUser = currentUser
                )
            }
        }
    }
}