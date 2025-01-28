package com.example.justdo.data

import com.example.justdo.network.SoapApi
import com.example.justdo.network.XmlParser

class MessengerRepository {
    suspend fun getUsers(): List<String> {
        return try {
            val response = SoapApi.getUsers()
            XmlParser.parseUserList(response)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendMessage(userId: String, message: String): Boolean {
        return try {
            val response = SoapApi.sendMessage(userId, message)
            XmlParser.parseMessageResponse(response)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}