package com.example.justdo.data

import android.util.Log
import com.example.justdo.network.RestApi

class MessengerRepository {
    suspend fun login(username: String, password: String): Boolean {
        return try {
            RestApi.login(username, password)
        } catch (e: Exception) {
            throw Exception("Ошибка авторизации")
        }
    }

    suspend fun register(username: String, password: String): Boolean {
        return try {
            RestApi.register(username, password)
            RestApi.login(username, password)
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