package com.example.justdo.data.models

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String = "",
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val showPhoneLogin: Boolean = false,
    val isCodeSent: Boolean = false,
    val isVerificationCompleted: Boolean = false,
    val canResendCode: Boolean = false,
    val resendTimeLeft: Int = 0,
    val countryList: List<CountryInfo> = emptyList(),
    val selectedCountry: CountryInfo? = null,
    val username: String = "",
    val formattedPhoneNumber: String = "",
    val isBiometricAvailable: Boolean = false,
    val showBiometricPrompt: Boolean = false,
    val useBiometricForNextLogin: Boolean = false,
    val isBiometricEnabled: Boolean = false
)