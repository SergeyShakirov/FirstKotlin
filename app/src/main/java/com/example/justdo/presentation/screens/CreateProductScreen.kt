package com.example.justdo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.User
import com.example.justdo.data.models.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductScreen(
    onBackClick: () -> Unit,
    onProductCreate: (Product) -> Unit,
    currentUser: User?
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Создание товара",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Название товара
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название товара") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Цена
            OutlinedTextField(
                value = price,
                onValueChange = { newValue ->
                    // Разрешаем только цифры и точку
                    val filteredValue = newValue.filter { it.isDigit() || it == '.' }

                    // Ограничиваем две цифры после запятой
                    val parts = filteredValue.split('.')
                    val formattedValue = if (parts.size > 1) {
                        "${parts[0]}.${parts[1].take(2)}"
                    } else {
                        filteredValue
                    }

                    price = formattedValue
                },
                label = { Text("Цена (₾)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Text("₾", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка создания товара
            Button(
                onClick = {
                    val newProduct = Product(
                        id = System.currentTimeMillis().toString(), // Временный ID
                        name = name,
                        price = price.toDoubleOrNull() ?: 0.0,
                        description = description,
                        seller = currentUser
                    )

                    onProductCreate(newProduct)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() &&
                        price.isNotBlank() &&
                        price.toDoubleOrNull() != null && // Проверка, что цена - корректное число
                        description.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Создать товар")
            }
        }
    }
}