package com.example.justdo.navigation

sealed class Screen(val route: String) {
    data object Users : Screen("users")
    data object Chat : Screen("chat")
    data object Profile : Screen("profile")
    data object Chats : Screen("chats")
    data object Map : Screen("map")
}