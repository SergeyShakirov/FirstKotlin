package com.example.justdo.network

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SoapApi {
    //private const val BASE_URL = "https://1c.moveit.kz/mobileapp/ws/Mess"
    private const val BASE_URL = "http://10.0.2.2/KT/ws/Mess.1cws"
    private var currentUsername: String? = null
    private var currentPassword: String? = null

    private fun getBasicAuth(username: String, password: String): String {
        val auth = "$username:$password"
        return Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private suspend fun executeSoapRequest(
        soapAction: String,
        soapBody: String,
        username: String? = currentUsername,
        password: String? = currentPassword
    ): String = withContext(Dispatchers.IO) {
        if (username == null || password == null) {
            throw Exception("Необходима авторизация")
        }

        val connection = URL(BASE_URL).openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic ${getBasicAuth(username, password)}")
                setRequestProperty("Content-Type", "text/xml; charset=utf-8")
                setRequestProperty("SOAPAction", soapAction)
                doOutput = true
            }

            connection.outputStream.use { it.write(soapBody.toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun login(username: String, password: String): String {
        val soapBody = """
            <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:mess="Mess">
               <soap:Header/>
               <soap:Body>
                  <mess:Login>
                     </mess:Login>
               </soap:Body>
            </soap:Envelope>
            """.trimIndent()

        val response = executeSoapRequest("Mess#Messenger:Users", soapBody, username, password)

        currentUsername = username
        currentPassword = password

        return response
    }

    suspend fun register(username: String, password: String): String {
        val soapBody = """
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:mess="Mess">
           <soap:Header/>
           <soap:Body>
              <mess:CreateUser>
                 <mess:Login>$username</mess:Login>
                 <mess:Password>$password</mess:Password>
              </mess:CreateUser>
           </soap:Body>
        </soap:Envelope>
        """.trimIndent()

        return executeSoapRequest("Mess#Messenger:Register", soapBody, "Admin", "")
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
                     <mess:Recipient>$userId</mess:Recipient>
                     <mess:Content>$message</mess:Content>
                  </mess:SendMessage>
               </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return executeSoapRequest("Mess#Messenger:SendMessage", soapBody)
    }

    suspend fun refreshMessages(userId: String): String {
        val soapBody = """
            <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:mess="Mess">
               <soap:Header/>
               <soap:Body>
                  <mess:RefreshMessages>
                     <mess:Recipient>$userId</mess:Recipient>
                  </mess:RefreshMessages>
               </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        return executeSoapRequest("Mess#Messenger:SendMessage", soapBody)
    }

}