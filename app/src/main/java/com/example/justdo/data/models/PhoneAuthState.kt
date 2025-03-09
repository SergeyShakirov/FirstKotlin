package com.example.justdo.data.models

import com.google.firebase.auth.PhoneAuthCredential

/**
 * Класс для управления состоянием верификации телефона
 */
data class PhoneAuthState(
    val verificationId: String,
    val phoneNumber: String,
    val callback: (PhoneAuthCredential?, Exception?) -> Unit
)