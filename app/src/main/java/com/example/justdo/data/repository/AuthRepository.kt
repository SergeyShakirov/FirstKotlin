package com.example.justdo.data.repository

import android.util.Log
import com.example.justdo.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Не удалось создать пользователя")
            val token = Firebase.messaging.token.await()

            val userProfile = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                avatarUrl = "",
                fcmToken = token
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

    fun logout() {
        auth.signOut()
    }
}