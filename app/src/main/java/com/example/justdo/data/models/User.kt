package com.example.justdo.data.models

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    var createAt: Timestamp = Timestamp.now(),
    val avatarUrl: String? = null,
    val fcmToken: String? = null,
    val phoneNumber: String? = null,
    val online: Boolean = false  // Добавленное поле для статуса "онлайн"
) {
    // Пустой конструктор необходим для Firestore
    constructor() : this("", "", "")
}
