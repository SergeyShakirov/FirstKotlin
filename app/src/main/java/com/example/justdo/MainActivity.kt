package com.example.justdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.justdo.data.MessengerRepository

data class Item(val id: Int, val name: String)

class MainActivity : ComponentActivity() {

    private val repository = MessengerRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(repository)
        }
    }
}

@Composable
fun MyApp(repository: MessengerRepository) {

    val scope = rememberCoroutineScope()
    val response = remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Состояние для отслеживания необходимости обновления
    val shouldUpdate by remember { mutableStateOf(true) }

    // Функция обновления данных
    val updateData = {
        scope.launch {
            isLoading = true
            try {
                response.value = repository.getUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(shouldUpdate) {
        while (shouldUpdate) {
            updateData()
            delay(5000)
        }
    }

    var selectedItem by remember { mutableStateOf<Item?>(null) }

    Scaffold { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            if (selectedItem == null) {
                val items = response.value.mapIndexed { index, name ->
                    Item(id = index, name = name)
                }
                ItemList(
                    items = items,
                    onItemClicked = { item -> selectedItem = item }
                )
            } else {
                ChatScreen(
                    item = selectedItem,
                    onBack = { selectedItem = null }
                )
            }
        }
    }}


@Composable
fun ItemList(items: List<Item>, onItemClicked: (Item) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onItemClicked(item) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}