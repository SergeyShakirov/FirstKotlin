package com.example.justdo.network

import org.w3c.dom.Document
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

object XmlParser {
    private val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }

    fun parseUserList(xmlResponse: String): List<String> {
        val list = mutableListOf<String>()

        try {
            val builder = factory.newDocumentBuilder()
            val inputStream = xmlResponse.byteInputStream(Charsets.UTF_8)
            val document: Document = builder.parse(inputStream)

            val userNodes: NodeList = document.getElementsByTagNameNS("http://www.messenger.org", "name")
            for (i in 0 until userNodes.length) {
                list.add(userNodes.item(i).textContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    fun parseMessageResponse(xmlResponse: String): Boolean {
        // Добавьте парсинг ответа на отправку сообщения
        return true
    }
}