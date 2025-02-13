package com.example.justdo.network

import com.example.justdo.data.models.User
import com.example.justdo.network.constants.NetworkConstants
import com.example.justdo.network.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UserApi {
    private const val BASE_URL = NetworkConstants.BASE_URL

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

    suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL${NetworkConstants.Endpoints.USERS}")

        val username = AuthApi.getCurrentUsername()
        val password = AuthApi.getCurrentPassword()

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization",
                    "Basic ${NetworkUtils.getBasicAuth(username, password)}")
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
}