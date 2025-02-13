package com.example.justdo.network

import com.example.justdo.data.models.Message
import com.example.justdo.network.constants.NetworkConstants
import com.example.justdo.network.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object MessagesApi {
    private const val BASE_URL = NetworkConstants.BASE_URL

    suspend fun refreshMessages(userId: String): List<Message> = withContext(Dispatchers.IO) {
        val username = AuthApi.getCurrentUsername()
        val password = AuthApi.getCurrentPassword()

        if (username == null || password == null) {
            throw Exception("Необходима авторизация")
        }

        val url = URL("$BASE_URL${NetworkConstants.Endpoints.MESSAGES}?Recipient=$userId")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Basic ${NetworkUtils.getBasicAuth(username, password)}")
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
            throw Exception("Ошибка получения сообщений: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun sendMessage(userId: String, message: String): Boolean = withContext(Dispatchers.IO) {
        val username = AuthApi.getCurrentUsername()
        val password = AuthApi.getCurrentPassword()

        if (username == null || password == null) {
            throw Exception("Необходима авторизация")
        }

        val url = URL("$BASE_URL${NetworkConstants.Endpoints.SEND_MESSAGE}")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic ${NetworkUtils.getBasicAuth(username, password)}")
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
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "Нет дополнительной информации"
                throw Exception("Ошибка HTTP ${connection.responseCode}: $errorResponse")
            }

            true
        } catch (e: Exception) {
            throw Exception("Ошибка отправки сообщения: ${e.message}")
        } finally {
            connection.disconnect()
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
                        timestamp = NetworkUtils.parseTimestamp(jsonObject.getString("timestamp"))
                    )
                )
            }
            messages
        } catch (e: Exception) {
            throw Exception("Ошибка парсинга ответа: ${e.message}")
        }
    }
}