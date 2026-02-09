package com.example.cms.dto

import java.time.LocalDateTime

data class MediaResponse(
    val id: Int,
    val filename: String,
    val originalFilename: String,
    val fileUrl: String,
    val mimeType: String?,
    val fileSize: Long?,
    val uploadedBy: MediaUploaderResponse?,
    val createdAt: LocalDateTime
)

data class MediaUploaderResponse(
    val id: Int,
    val username: String
)

data class MediaUploadResponse(
    val success: Boolean,
    val message: String,
    val media: MediaResponse?
)

data class ErrorResponse(
    val success: Boolean = false,
    val message: String,
    val errors: List<String>? = null
)
