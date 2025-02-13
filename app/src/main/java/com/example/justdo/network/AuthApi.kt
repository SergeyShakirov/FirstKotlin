package com.example.justdo.network

import android.content.Context
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.MessengerRepository
import com.example.justdo.network.constants.NetworkConstants
import com.example.justdo.network.utils.NetworkUtils
import com.example.justdo.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AuthApi {
    private const val BASE_URL = NetworkConstants.BASE_URL

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"

    private lateinit var currentUsername: String
    private lateinit var currentPassword: String
    private lateinit var currentUserId: String

    fun getCurrentUsername() = currentUsername
    fun getCurrentPassword() = currentPassword
    fun getCurrentUserId() = currentUserId

    // Метод для сохранения данных пользователя
    private fun saveUserData(context: Context, userId: String, username: String, password: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            apply()
        }
    }

    fun getCurrentUserId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, "") ?: ""
    }

    fun getCurrentUsername(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, "") ?: ""
    }

    fun getCurrentPassword(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASSWORD, "") ?: ""
    }

    suspend fun login(username: String, password: String): User = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL${NetworkConstants.Endpoints.LOGIN}")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic ${NetworkUtils.getBasicAuth(username, password)}")
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }

            val user = parseLoginResponse(username, response)

            currentUsername = username
            currentPassword = password

            user
        } catch (e: Exception) {
            throw Exception("Ошибка авторизации: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun register(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL${NetworkConstants.Endpoints.REGISTER}")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic ${NetworkUtils.getBasicAuth("Admin", "")}")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val jsonBody = """
                {
                    "Login": "$username",
                    "Password": "$password"
                }
                """.trimIndent()

            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "Нет дополнительной информации"
                throw Exception("Ошибка HTTP ${connection.responseCode}: $errorResponse")
            }

            true

        } catch (e: Exception) {
            throw Exception("Ошибка регистрации: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun saveFcmToken(userId: String, token: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL${NetworkConstants.Endpoints.ADD_FCM_TOKEN}")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic ${NetworkUtils.getBasicAuth(currentUsername, currentPassword)}")
                setRequestProperty("Content-Type", "application/json")
            }

            val jsonBody = """
                {
                    "id": "$userId",
                    "token": "$token"
                }
                """.trimIndent()

            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            throw Exception("Ошибка добавления токена: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun tokenFCM(userId: String): String = withContext(Dispatchers.IO) {
        if (currentUsername.isBlank()) {
            throw Exception("Необходима авторизация")
        }

        val url = URL("${BASE_URL}${NetworkConstants.Endpoints.FCM_TOKEN}?Recipient=$userId")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Basic ${NetworkUtils.getBasicAuth(currentUsername, currentPassword)}")
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }

            parseTokenResponse(response)
        } catch (e: Exception) {
            throw Exception("Ошибка получения сообщений: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseTokenResponse(jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.getString("token")
        } catch (e: Exception) {
            throw Exception("Ошибка парсинга JSON: ${e.message}")
        }
    }

    private fun parseLoginResponse(username: String, jsonString: String): User {
        return try {
            val jsonObject = JSONObject(jsonString)
            User(
                id = jsonObject.getString("id"),
                name = username,
                password = ""
            )
        } catch (e: Exception) {
            throw Exception("Ошибка парсинга JSON: ${e.message}")
        }
    }

    fun logout(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Удаляем токен для этого userId с сервера
                //deleteFcmToken(userId)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }
}