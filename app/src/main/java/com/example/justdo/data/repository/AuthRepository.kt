package com.example.justdo.data.repository

import android.app.Activity
import android.content.Intent
import com.example.justdo.data.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.ktx.auth
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions

@Singleton
class AuthRepository @Inject constructor(
    private val context: Context
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    // Клиент для Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient

    /**
     * Инициализация клиента Google Sign-In
     */
    fun initGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.example.justdo.R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(context, gso)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google Sign-In", e)
            throw e
        }
    }

    /**
     * Получение Intent для авторизации через Google
     */
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Авторизация с использованием Google токена
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Создаем credential с полученным ID-токеном
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // Авторизуемся в Firebase с этим credential
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return@withContext Result.failure(Exception("Ошибка авторизации через Google"))

            // Создаем или обновляем профиль пользователя
            val user = createOrUpdateUser(firebaseUser)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with Google", e)
            Result.failure(e)
        }
    }

    /**
     * Обновление профиля пользователя
     */
    suspend fun updateUserProfile(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            // Обновляем документ в Firestore
            firestore.collection("users").document(user.id)
                .set(user, SetOptions.merge())
                .await()

            Log.d(TAG, "Профиль пользователя обновлен: ${user.id}, имя: ${user.username}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении профиля пользователя", e)
            false
        }
    }

    /**
     * Получение пользователя по ID
     */
    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            return@withContext userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID", e)
            return@withContext null
        }
    }

    /**
     * Класс для управления состоянием верификации телефона
     */
    private data class PhoneAuthState(
        val verificationId: String,
        val phoneNumber: String,
        val callback: (PhoneAuthCredential?, Exception?) -> Unit
    )

    // Хранилище состояний верификации телефона
    private val phoneAuthStates = mutableMapOf<String, PhoneAuthState>()

    /**
     * Начать процесс авторизации по телефону
     */
    fun startPhoneAuth(
        phoneNumber: String,
        activity: Activity,
        onVerificationSent: (String) -> Unit,
        onAutoVerification: (PhoneAuthCredential) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Создаем callback-обработчики для событий верификации
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Автоматическая верификация (SMS прочитан автоматически)
                    Log.d(TAG, "Phone verification completed automatically")
                    onAutoVerification(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {  // Изменено с Exception на FirebaseException
                    Log.e(TAG, "Phone verification failed", e)
                    onError(e as Exception)  // Преобразуем к Exception, поскольку onError принимает Exception
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "SMS verification code has been sent")

                    // Сохраняем состояние верификации
                    phoneAuthStates[phoneNumber] = PhoneAuthState(
                        verificationId = verificationId,
                        phoneNumber = phoneNumber,
                        callback = { credential, error ->
                            if (error != null) {
                                onError(error)
                            } else if (credential != null) {
                                onAutoVerification(credential)
                            }
                        }
                    )

                    // Уведомляем о том, что код отправлен
                    onVerificationSent(verificationId)
                }
            }

            // Настройка параметров верификации
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            // Начало процесса верификации
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting phone authentication", e)
            onError(e)
        }
    }

    /**
     * Проверка кода подтверждения
     */
    fun verifyPhoneCode(verificationId: String, code: String): PhoneAuthCredential? {
        return try {
            PhoneAuthProvider.getCredential(verificationId, code)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying phone code", e)
            null
        }
    }

    /**
     * Авторизация с помощью PhoneAuthCredential
     */
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<User> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return@withContext Result.failure(Exception("Ошибка авторизации по телефону"))

            // Создаем или обновляем профиль пользователя
            val user = createOrUpdateUser(firebaseUser)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with phone credential", e)
            Result.failure(e)
        }
    }

    /**
     * Создание или обновление пользователя в Firestore
     */
    private suspend fun createOrUpdateUser(firebaseUser: FirebaseUser): User {
        // Создаем объект User из данных Firebase
        val user = User(
            id = firebaseUser.uid,
            username = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            phoneNumber = firebaseUser.phoneNumber,
            avatarUrl = firebaseUser.photoUrl?.toString()
        )

        try {
            // Проверяем, существует ли уже документ пользователя
            val userDoc = firestore.collection("users").document(user.id).get().await()

            if (!userDoc.exists()) {
                // Если пользователя нет, создаем новый документ
                firestore.collection("users").document(user.id).set(user).await()
                return user
            } else {
                // Если пользователь существует, получаем его данные
                val existingUser = userDoc.toObject(User::class.java)
                    ?: return user // Если преобразование не удалось, возвращаем объект из Firebase

                return existingUser
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating user document", e)
            // В случае ошибки возвращаем объект, созданный из данных Firebase
            return user
        }
    }

    /**
     * Получение текущего авторизованного пользователя
     */
    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val firebaseUser = auth.currentUser ?: return@withContext null

        try {
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user", e)
            null
        }
    }

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

    /**
     * Выход из аккаунта
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            // Выход из Google (если был использован)
            if (::googleSignInClient.isInitialized) {
                googleSignInClient.signOut().await()
            }

            // Выход из Firebase Auth
            auth.signOut()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error logging out", e)
            false
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}