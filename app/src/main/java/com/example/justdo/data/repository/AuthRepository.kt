package com.example.justdo.data.repository

import android.util.Log
import com.example.justdo.data.models.Chat
import com.example.justdo.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Регистрация по email
    suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Не удалось создать пользователя")

            val userProfile = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                chats = emptyList()
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(userProfile)
                .await()

            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Вход по email
    suspend fun loginWithEmail(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Не удалось войти")

            val userSnapshot = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userSnapshot.toObject(User::class.java)
                ?: throw Exception("Данные пользователя не найдены")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Выход из системы
    fun logout() {
        auth.signOut()
    }

    // Получение текущего пользователя
    suspend fun getCurrentUser(): User? {
        try {
            val firebaseUser = auth.currentUser ?: return null

            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            return userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("getCurrentUser", "Ошибка при получении текущего пользователя", e)
            return null
        }
    }
}