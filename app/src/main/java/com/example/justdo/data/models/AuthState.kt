package com.example.justdo.data.models

sealed class AuthState {
    /** Состояние загрузки/проверки авторизации */
    data object Loading : AuthState()

    /** Состояние, когда пользователь не авторизован */
    data object Unauthorized : AuthState()

    /** Состояние, когда пользователь авторизован */
    data class Authorized(val user: User) : AuthState()
}