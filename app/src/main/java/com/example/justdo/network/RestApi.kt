package com.example.justdo.network

import android.util.Base64
import com.example.justdo.data.Message
import com.example.justdo.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

object RestApi {
    private const val BASE_URL = "http://10.0.2.2/KT/hs/KT"

    private var currentUsername: String? = null
    private var currentPassword: String? = null

    private fun getBasicAuth(username: String, password: String): String {
        val auth = "$username:$password"
        return Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun parseTimestamp(timestampString: String): Long {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            formatter.parse(timestampString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun parseUsersResponse(jsonString: String): List<User> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val users = mutableListOf<User>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                users.add(
                    User(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name")
                    )
                )
            }
            users
        } catch (e: Exception) {
            throw Exception("Ошибка парсинга ответа: ${e.message}")
        }
    }

    private fun parseMessagesResponse(jsonString: String): List<Message> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val messages = mutableListOf<Message>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                messages.add(
                    Message(
                        content = jsonObject.getString("content"),
                        id = jsonObject.getString("id"),
                        isFromMe = jsonObject.getBoolean("isFromMe"),
                        timestamp = parseTimestamp(jsonObject.getString("timestamp"))
                    )
                )
            }
            messages
        } catch (e: Exception) {
            throw Exception("Ошибка парсинга ответа: ${e.message}")
        }
    }

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/login")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic ${getBasicAuth(username, password)}")
                setRequestProperty("Content-Type", "application/json")
            }

            currentUsername = username
            currentPassword = password

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            throw Exception("Ошибка авторизации: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
        if (currentUsername == null || currentPassword == null) {
            throw Exception("Необходима авторизация")
        }

        val url = URL("$BASE_URL/users")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization",
                    "Basic ${getBasicAuth(currentUsername!!, currentPassword!!)}")
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }

            parseUsersResponse(response)
        } catch (e: Exception) {
            throw Exception("Ошибка получения списка пользователей: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun refreshMessages(userId: String): List<Message> = withContext(Dispatchers.IO) {
        if (currentUsername == null || currentPassword == null) {
            throw Exception("Необходима авторизация")
        }

        val url = URL("$BASE_URL/messages?Recipient=$userId")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization",
                    "Basic ${getBasicAuth(currentUsername!!, currentPassword!!)}")
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }

            parseMessagesResponse(response)
        } catch (e: Exception) {
            throw Exception("Ошибка получения списка пользователей: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun sendMessage(userId: String, message: String): Boolean = withContext(Dispatchers.IO) {

        if (currentUsername == null || currentPassword == null) {
            throw Exception("Необходима авторизация")
        }

        val url = URL("$BASE_URL/send")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization",
                    "Basic ${getBasicAuth(currentUsername!!, currentPassword!!)}")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val jsonBody = """
            {
                "Recipient": "$userId",
                "Content": "$message"
            }
            """.trimIndent()

            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Нет дополнительной информации"
                throw Exception("Ошибка HTTP ${connection.responseCode}: $errorResponse")
            }

            true

        } catch (e: Exception) {
            throw Exception("Ошибка отправки сообщения: ${e.message}")
        } finally {
            connection.disconnect()
        }

    }

    suspend fun register(username: String, password: String): Boolean = withContext(Dispatchers.IO) {

        val url = URL("$BASE_URL/createUser")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization",
                    "Basic ${getBasicAuth("Admin", "")}")
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
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Нет дополнительной информации"
                throw Exception("Ошибка HTTP ${connection.responseCode}: $errorResponse")
            }

            true

        } catch (e: Exception) {
            throw Exception("Ошибка отправки сообщения: ${e.message}")
        } finally {
            connection.disconnect()
        }

    }
}