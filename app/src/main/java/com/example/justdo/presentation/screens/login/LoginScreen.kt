package com.example.justdo.presentation.screens.login

import androidx.compose.runtime.*
import com.example.justdo.data.models.User
import com.example.justdo.presentation.viewmodels.UserViewModel

@Composable
fun LoginScreen(
    userViewModel: UserViewModel,
    onLoginSuccess: (User) -> Unit
) {
    val uiState by userViewModel.uiState.collectAsState()

    // Обработка изменений состояния
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated && uiState.user != null) {
            userViewModel.resetState()
            onLoginSuccess(uiState.user!!)
        }
    }

    // Выбор экрана в зависимости от состояния
    when {
        uiState.isVerificationCompleted -> {
            UsernameScreen(
                userViewModel = userViewModel,
                onBack = { userViewModel.backToVerification() }
            )
        }
        uiState.showPhoneLogin -> {
            PhoneAuthScreen(
                userViewModel = userViewModel,
                onBack = { userViewModel.resetState() }
            )
        }
        else -> {
            MainLoginScreen(
                userViewModel = userViewModel,
            )
        }
    }
}