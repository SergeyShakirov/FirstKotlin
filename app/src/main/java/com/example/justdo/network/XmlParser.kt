package com.example.justdo.network

import android.util.Log
import com.example.justdo.presentation.screens.Message
import com.example.justdo.data.User
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.text.SimpleDateFormat
import java.util.Locale

object XmlParser {
    private val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }

    private fun Element.getElementTextContent(tagName: String): String {
        return getElementsByTagNameNS("http://www.messenger.org", tagName)
            .item(0)?.textContent
            ?: throw IllegalStateException("Required element $tagName not found")
    }

    fun parseLoginResponse(xmlResponse: String?): User? {
        if (xmlResponse.isNullOrEmpty()) {
            return null
        }

        return try {
            val builder = factory.newDocumentBuilder()
            val inputStream = xmlResponse.byteInputStream(Charsets.UTF_8)
            val document: Document = builder.parse(inputStream)

            val userElement = document.documentElement
                .getElementsByTagNameNS("http://www.messenger.org", "user")
                .item(0) as? Element ?: return null

            User(
                id = userElement.getElementTextContent("id"),
                name = userElement.getElementTextContent("name")
            )
        } catch (e: Exception) {
            Log.e("XMLParser", "Ошибка парсинга ответа авторизации", e)
            null
        }
    }

    fun parseUserList(xmlResponse: String?): List<User> {
        if (xmlResponse.isNullOrEmpty()) {
            return emptyList()
        }

        return try {
            val builder = factory.newDocumentBuilder()
            val inputStream = xmlResponse.byteInputStream(Charsets.UTF_8)
            val document: Document = builder.parse(inputStream)

            document.documentElement
                .getElementsByTagNameNS("http://www.messenger.org", "user")
                .run {
                    (0 until length).map { i ->
                        (item(i) as Element).let { element ->
                            User(
                                id = element.getElementTextContent("id"),
                                name = element.getElementTextContent("name")
                            )
                        }
                    }
                }
        } catch (e: ParserConfigurationException) {
            Log.e("XMLParser", "Ошибка парсинга пользователей: ошибка конфигурации парсера", e)
            emptyList()
        } catch (e: SAXException) {
            Log.e("XMLParser", "Ошибка парсинга пользователей: ошибка SAX", e)
            emptyList()
        } catch (e: IOException) {
            Log.e("XMLParser", "Ошибка парсинга пользователей: ошибка IO", e)
            emptyList()
        }
    }

    fun parseMessages(xmlResponse: String?): List<Message> {
        if (xmlResponse.isNullOrEmpty()) {
            return emptyList()
        }

        return try {
            val builder = factory.newDocumentBuilder()
            val inputStream = xmlResponse.byteInputStream(Charsets.UTF_8)
            val document: Document = builder.parse(inputStream)
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

            document.documentElement
                .getElementsByTagNameNS("http://www.messenger.org", "message")
                .run {
                    (0 until length).map { i ->
                        (item(i) as Element).let { element ->

                            val timestampStr = element.getElementTextContent("timestamp")
                            val timestamp = try {
                                format.parse(timestampStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                Log.e("XMLParser", "Ошибка парсинга даты: $timestampStr", e)
                                System.currentTimeMillis()
                            }

                            Message(
                                id = element.getElementTextContent("id"),
                                content = element.getElementTextContent("content"),
                                isFromMe = (element.getElementTextContent("isFromMe")).toBoolean(),
                                timestamp = timestamp
                            )
                        }
                    }
                }
        } catch (e: ParserConfigurationException) {
            Log.e("XMLParser", "Ошибка парсинга сообщений: ошибка конфигурации парсера", e)
            emptyList()
        } catch (e: SAXException) {
            Log.e("XMLParser", "Ошибка парсинга сообщений: ошибка SAX", e)
            emptyList()
        } catch (e: IOException) {
            Log.e("XMLParser", "Ошибка парсинга сообщений: ошибка IO", e)
            emptyList()
        }
    }
}