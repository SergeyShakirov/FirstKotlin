package com.example.justdo.network

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SoapApi {
    private const val BASE_URL = "http://10.0.2.2/Mess/ws/ws1.1cws"
    private const val USERNAME = "Admin"
    private const val PASSWORD = "123"

    private fun getBasicAuth(): String {
        val auth = "$USERNAME:$PASSWORD"
        return Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private suspend fun executeSoapRequest(
        soapAction: String,
        soapBody: String
    ): String = withContext(Dispatchers.IO) {
        val connection = URL(BASE_URL).openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic ${getBasicAuth()}")
                setRequestProperty("Content-Type", "text/xml; charset=utf-8")
                setRequestProperty("SOAPAction", soapAction)
                doOutput = true
            }

            // Отправка запроса
            connection.outputStream.use { it.write(soapBody.toByteArray(Charsets.UTF_8)) }

            // Проверка ответа
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }

            // Чтение ответа
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getUsers(): String {
        val soapBody = """
            <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:mess="Mess">
               <soap:Header/>
               <soap:Body>
                  <mess:Users/>
               </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return executeSoapRequest("Mess#Messenger:Users", soapBody)
    }

    // Добавьте здесь другие методы для работы с SOAP API
    suspend fun sendMessage(userId: String, message: String): String {
        val soapBody = """
            <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:mess="Mess">
               <soap:Header/>
               <soap:Body>
                  <mess:SendMessage>
                     <mess:userId>$userId</mess:userId>
                     <mess:message>$message</mess:message>
                  </mess:SendMessage>
               </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return executeSoapRequest("Mess#Messenger:SendMessage", soapBody)
    }
}