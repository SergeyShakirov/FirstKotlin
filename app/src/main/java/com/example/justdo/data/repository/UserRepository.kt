package com.example.justdo.data.repository

import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun uploadAvatar(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

            // Читаем изображение как bytes
            val inputStream = auth.app.applicationContext.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Cannot read file")

            // Конвертируем в Base64
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

            // Обновляем в Firestore
            firestore.collection("users")
                .document(userId)
                .update("avatarUrl", base64String)
                .await()

            Result.success(base64String)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}