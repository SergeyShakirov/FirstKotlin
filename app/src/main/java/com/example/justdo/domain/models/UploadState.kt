package com.example.justdo.domain.models

sealed class UploadState {
    data object Idle : UploadState()
    data object Loading : UploadState()
    data class Success(val avatarUrl: String) : UploadState()
    data class Error(val message: String) : UploadState()
}