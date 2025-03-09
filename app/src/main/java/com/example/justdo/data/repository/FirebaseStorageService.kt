package com.example.justdo.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.justdo.data.models.UploadState
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class FirebaseStorageService {
    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    /**
     * Загружает изображение в Firebase Storage и возвращает URL
     */
    suspend fun uploadImage(imageUri: Uri, contentResolver: ContentResolver, userId: String): String? {
        try {
            _uploadState.value = UploadState.Loading(0)

            // Определяем тип файла
            val fileExtension = getFileExtension(contentResolver, imageUri)

            // Добавляем временную метку к имени файла для сортировки по времени
            val timestamp = System.currentTimeMillis()
            val fileUuid = UUID.randomUUID()
            val fileName = "avatars/${userId}/${timestamp}_${fileUuid}.$fileExtension"
            val fileRef = storageReference.child(fileName)

            // Загружаем файл
            fileRef.putFile(imageUri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    _uploadState.value = UploadState.Loading(progress)
                }.await()

            // Получаем URL загруженного изображения
            val downloadUrl = fileRef.downloadUrl.await().toString()

            // Устанавливаем успешное состояние с URL
            _uploadState.value = UploadState.Success(downloadUrl)

            return downloadUrl
        } catch (e: Exception) {
            Log.e("FirebaseStorageService", "Ошибка при загрузке изображения: ${e.message}", e)
            _uploadState.value = UploadState.Error(e.message ?: "Произошла ошибка при загрузке изображения")
            return null
        }
    }

    /**
     * Получает список всех аватарок пользователя
     */
    suspend fun getUserAvatars(userId: String): List<String> {
        try {
            val avatarsRef = storageReference.child("avatars/$userId")
            val listResult = avatarsRef.listAll().await()

            // Получаем URL для каждого изображения
            val avatarUrls = mutableListOf<String>()
            for (item in listResult.items) {
                try {
                    val url = item.downloadUrl.await().toString()
                    avatarUrls.add(url)
                } catch (e: Exception) {
                    Log.e("FirebaseStorageService", "Не удалось получить URL для ${item.name}: ${e.message}")
                }
            }

            // Сортируем аватарки по имени файла (по временной метке)
            // Новые аватарки будут в начале списка
            avatarUrls.sortByDescending { url ->
                try {
                    // Извлекаем имя файла из URL
                    val fileName = url.substringAfterLast("%2F").substringBefore(".")
                    // Пробуем извлечь timestamp из начала имени файла
                    fileName.substringBefore("_").toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    Log.e("FirebaseStorageService", "Ошибка при сортировке аватарок: ${e.message}")
                    0L
                }
            }

            Log.d("FirebaseStorageService", "Получено ${avatarUrls.size} аватарок пользователя")
            return avatarUrls
        } catch (e: Exception) {
            Log.e("FirebaseStorageService", "Ошибка при получении списка аватарок: ${e.message}", e)
            // В случае ошибки возвращаем пустой список
            return emptyList()
        }
    }

    /**
     * Удаляет аватарку по URL - исправленная версия с поддержкой разных форматов URL
     */
    suspend fun deleteAvatarByUrl(avatarUrl: String): Boolean {
        Log.d("FirebaseStorageService", "Пытаемся удалить аватарку: $avatarUrl")

        try {
            // Метод 1: через Firebase.storage.getReferenceFromUrl (предпочтительный способ)
            try {
                val fileRef = Firebase.storage.getReferenceFromUrl(avatarUrl)
                Log.d("FirebaseStorageService", "Получена ссылка через Firebase SDK: ${fileRef.path}")
                fileRef.delete().await()
                Log.d("FirebaseStorageService", "Аватарка успешно удалена через Firebase SDK")
                return true
            } catch (e: Exception) {
                Log.d("FirebaseStorageService", "Не удалось удалить через Firebase SDK: ${e.message}")
                // Если первый метод не сработал, пробуем другие методы
            }

            // Метод 2: через распознавание пути из URL
            val pathFromUrl = extractPathFromUrl(avatarUrl)
            if (pathFromUrl != null) {
                try {
                    val fileRef = storageReference.child(pathFromUrl)
                    Log.d("FirebaseStorageService", "Получена ссылка через путь: ${fileRef.path}")
                    fileRef.delete().await()
                    Log.d("FirebaseStorageService", "Аватарка успешно удалена через путь из URL")
                    return true
                } catch (e: Exception) {
                    Log.d("FirebaseStorageService", "Не удалось удалить через путь: ${e.message}")
                }
            }

            // Если не удалось удалить через предыдущие методы, пробуем ещё один подход
            // Метод 3: Повторно загружаем список аватарок и ищем соответствие по URL
            val userId = extractUserIdFromUrl(avatarUrl)
            if (userId != null) {
                try {
                    val avatarsRef = storageReference.child("avatars/$userId")
                    val listResult = avatarsRef.listAll().await()

                    for (item in listResult.items) {
                        val itemUrl = item.downloadUrl.await().toString()
                        if (itemUrl == avatarUrl) {
                            item.delete().await()
                            Log.d("FirebaseStorageService", "Аватарка успешно удалена через перебор списка")
                            return true
                        }
                    }
                    Log.d("FirebaseStorageService", "URL не найден в списке аватарок")
                } catch (e: Exception) {
                    Log.d("FirebaseStorageService", "Не удалось удалить через перебор списка: ${e.message}")
                }
            }

            Log.e("FirebaseStorageService", "Не удалось удалить аватарку никаким из доступных методов")
            return false
        } catch (e: Exception) {
            Log.e("FirebaseStorageService", "Критическая ошибка при удалении аватарки: ${e.message}", e)
            return false
        }
    }

    /**
     * Извлекает путь к файлу из URL Firebase Storage
     */
    private fun extractPathFromUrl(url: String): String? {
        try {
            // Пример URL: https://firebasestorage.googleapis.com/v0/b/justdo-3104e.appspot.com/o/avatars%2FKJ4E0DVtuPdaiikPlY4cP9rveRt1%2F1709379669983_b3a95316-1ff2-48eb-a5f0-2e2fc27e3e3d.jpg?alt=media&token=f8b0df32-1967-42fb-9dba-81bceb004664

            // Декодируем URL-кодированные символы
            val decodedUrl = url
                .replace("%2F", "/")
                .replace("%3A", ":")
                .replace("%3F", "?")
                .replace("%3D", "=")
                .replace("%26", "&")

            // Извлекаем путь между /o/ и ?
            val pathStart = decodedUrl.indexOf("/o/")
            if (pathStart >= 0) {
                val startIndex = pathStart + 3 // длина "/o/"
                val queryIndex = decodedUrl.indexOf("?", startIndex)
                if (queryIndex > startIndex) {
                    return decodedUrl.substring(startIndex, queryIndex)
                }
            }

            return null
        } catch (e: Exception) {
            Log.e("FirebaseStorageService", "Ошибка при извлечении пути из URL: ${e.message}", e)
            return null
        }
    }

    /**
     * Извлекает ID пользователя из URL Firebase Storage
     */
    private fun extractUserIdFromUrl(url: String): String? {
        try {
            // Пример: .../avatars%2FKJ4E0DVtuPdaiikPlY4cP9rveRt1%2F...
            val decodedUrl = url.replace("%2F", "/")

            val avatarsIndex = decodedUrl.indexOf("/avatars/")
            if (avatarsIndex >= 0) {
                val startIndex = avatarsIndex + 9 // длина "/avatars/"
                val endIndex = decodedUrl.indexOf("/", startIndex)
                if (endIndex > startIndex) {
                    return decodedUrl.substring(startIndex, endIndex)
                }
            }

            return null
        } catch (e: Exception) {
            Log.e("FirebaseStorageService", "Ошибка при извлечении userId из URL: ${e.message}", e)
            return null
        }
    }



    /**
     * Получает StorageReference из URL
     */
    private fun getStorageRefFromUrl(url: String): StorageReference? {
        try {
            // URL вида: https://firebasestorage.googleapis.com/v0/b/[bucket]/o/avatars%2F[userId]%2F[filename]?alt=...
            val decodedUrl = url
                .replace("%2F", "/")
                .replace("%3A", ":")
                .replace("%3F", "?")
                .replace("%3D", "=")
                .replace("%26", "&")

            // Извлекаем путь к файлу
            val pathStart = decodedUrl.indexOf("/o/") + 3
            val pathEnd = decodedUrl.indexOf("?", pathStart)

            if (pathStart >= 3 && pathEnd > pathStart) {
                val storagePath = decodedUrl.substring(pathStart, pathEnd)
                return FirebaseStorage.getInstance().getReference(storagePath)
            }

            return null
        } catch (e: Exception) {
            Log.e("FirebaseStorageService", "Ошибка при получении ссылки из URL: ${e.message}", e)
            return null
        }
    }

    /**
     * Возвращает расширение файла
     */
    private fun getFileExtension(contentResolver: ContentResolver, uri: Uri): String {
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(contentResolver.getType(uri)) ?: "jpg"
    }
}