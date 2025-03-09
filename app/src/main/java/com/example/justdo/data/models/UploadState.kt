package com.example.justdo.data.models

/**
 * Состояния загрузки файла в хранилище
 */
sealed class UploadState {
    /**
     * Начальное состояние, загрузка не начата
     */
    object Idle : UploadState()

    /**
     * Загрузка в процессе с указанием прогресса в процентах
     */
    data class Loading(val progress: Int) : UploadState()

    /**
     * Загрузка успешно завершена
     * Добавлен параметр url для хранения URL загруженного файла
     */
    data class Success(val url: String = "") : UploadState()

    /**
     * Произошла ошибка при загрузке
     */
    data class Error(val message: String) : UploadState()
}