package com.example.justdo.data.repository

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

            // Создание профиля пользователя в Firestore
            val userProfile = hashMapOf(
                "uid" to firebaseUser.uid,
                "email" to email,
                "username" to username,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(userProfile)
                .await()

            Result.success(
                User(
                    id = firebaseUser.uid,
                    name = username,
                    email = email
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Вход по email
    suspend fun loginWithEmail(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Не удалось войти")

            // Получаем данные пользователя из Firestore
            val userSnapshot = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val username = userSnapshot.getString("username") ?: email

            Result.success(
                User(
                    id = firebaseUser.uid,
                    name = username,
                    email = email
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Выход
    fun logout() {
        auth.signOut()
    }

    // Проверка авторизации
    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return firebaseUser?.let {
            User(
                id = it.uid,
                name = it.displayName ?: "",
                email = it.email ?: ""
            )
        }
    }
}