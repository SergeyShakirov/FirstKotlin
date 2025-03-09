package com.example.justdo.domain.models

sealed class UploadState {
    data object Idle : UploadState()
    data class Loading(val progress: Int) : UploadState()
    data object Success : UploadState()
    data class Error(val message: String) : UploadState()
}