package com.example.justdo

import android.R.attr.password
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList

data class Item(val id: Int, val name: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {

    val response = remember { mutableStateOf("") }
    var isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(isLoading) {
        if (isLoading.value) {
            response.value = sendSoapRequest() // Вызов suspend-функции
            isLoading.value = false
        }
    }

}

//    val itemList = listOf(
//        Item(1, "Item 1"),
//        Item(2, "Item 2"),
//        Item(3, "Item 3"),
//    )
//
//    var selectedItem by remember { mutableStateOf<Item?>(null) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(title = { Text(text = "Jetpack") })
//        }
//    ) { contentPadding ->
//        Box(modifier = Modifier.padding(contentPadding)) {
//            if (selectedItem == null) {
//                ItemList(
//                    items = itemList,
//                    onItemClicked = { item -> selectedItem = item }
//                )
//            } else {
//                DetailScreen(
//                    item = selectedItem,
//                    onBack = { selectedItem = null }
//                )
//            }
//        }
//    }


//@Composable
//fun ItemList(items: List<Item>, onItemClicked: (Item) -> Unit) {
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        items(items) { item ->
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 8.dp)
//                    .clickable { onItemClicked(item) },
//                elevation = CardDefaults.cardElevation(4.dp)
//            ) {
//                Text(
//                    text = item.name,
//                    modifier = Modifier.padding(16.dp),
//                    style = MaterialTheme.typography.bodyLarge
//                )
//            }
//        }
//    }
//}

@Composable
fun DetailScreen(item: Item?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text(text = "Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Details for ${item?.name}",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

suspend fun sendSoapRequest(): String {
    val url = "http://10.0.2.2/Mess/ws/ws1.1cws"
    val soapRequest = """
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:mess="Mess">
           <soap:Header/>
           <soap:Body>
              <mess:Users/>
           </soap:Body>
        </soap:Envelope>
    """

    return withContext(Dispatchers.IO) { // Выполнение сетевого запроса в фоновом потоке
        try {
            // Создание соединения
            val connection = URL(url).openConnection() as HttpURLConnection

            val auth: String = "Admin:123"
            val encodedAuth: String = Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
            connection.setRequestProperty("SOAPAction", "Mess#Messenger:Users")

            // Отправка SOAP-запроса
            connection.outputStream.use { outputStream: OutputStream ->
                outputStream.write(soapRequest.toByteArray(Charsets.UTF_8))
            }

            // Чтение ответа от сервера
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) { // Проверка успешности ответа
                BufferedReader(
                    InputStreamReader(connection.inputStream)
                ).use { reader ->
                    val response = StringBuilder()
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        response.append(line)
                    }

                    val xmlResponse = response.toString()
                    parseXmlResponse(xmlResponse)

                    // Возвращаем ответ
                    return@withContext response.toString()
                }
            } else {
                return@withContext "Ошибка: HTTP код $responseCode"
            }

            // Закрываем соединение
            connection.disconnect()
        } catch (e: Exception) {
            return@withContext "Exception: ${e.message}"
        }.toString()
    }
}

fun parseXmlResponse(xmlResponse: String): MutableList<String> {

    val list = mutableListOf<String>()  // Создаем пустой изменяемый список
    list.add("Hello")  // Добавляем элемент
    list.add("World")
    list.add("Kotlin")

    try {
        // Создание фабрики и парсинг XML
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true // Учитываем пространства имен
        val builder = factory.newDocumentBuilder()
        val inputStream = xmlResponse.byteInputStream(Charsets.UTF_8)
        val document: Document = builder.parse(inputStream)

        // Извлечение элементов <name> из <user>
        val userNodes: NodeList = document.getElementsByTagNameNS("http://www.messenger.org", "name")
        for (i in 0 until userNodes.length) {
            val node = userNodes.item(i)
            list.add(node.textContent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return list
}

@Preview(showBackground = true)
@Composable
fun MyAppPreview() {
    MyApp()
}