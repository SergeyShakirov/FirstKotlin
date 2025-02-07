package com.example.justdo.data

import android.util.Log
import com.example.justdo.network.RestApi
import com.example.justdo.presentation.screens.Message
import com.example.justdo.network.SoapApi
import com.example.justdo.network.XmlParser

class MessengerRepository {
    suspend fun login(username: String, password: String): Boolean {
        return try {
            RestApi.login(username, password)
        } catch (e: Exception) {
            throw Exception("Ошибка авторизации")
        }
    }

    suspend fun register(username: String, password: String): User {
        return try {
            Log.d("MessengerRepository", "Начало регистрации для пользователя: $username")

            val response = SoapApi.register(username, password)
            Log.d("MessengerRepository", "Ответ сервера: $response")

            val registeredUser = XmlParser.parseLoginResponse(response)
            Log.d("MessengerRepository", "Результат парсинга: $registeredUser")

            val loginResponse = SoapApi.login(username, password)
            Log.d("MessengerRepository", "Ответ на логин: $loginResponse")

            XmlParser.parseLoginResponse(loginResponse)
                ?: throw Exception("Ошибка авторизации")

        } catch (e: Exception) {
            Log.e("MessengerRepository", "Ошибка при регистрации", e)
            throw Exception("Ошибка регистрации: ${e.message}")
        }
    }

    suspend fun getUsers(): List<User> {
        return try {
            RestApi.getUsers()
        } catch (e: Exception) {
            throw Exception("Ошибка получения списка пользователей")
        }
    }

    suspend fun sendMessage(userId: String, message: String): Boolean {
        return try {
            RestApi.sendMessage(userId, message)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun refreshMessages(userId: String): List<Message> {
        return try {
            RestApi.refreshMessages(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}