package com.example.cms.service

import com.example.cms.model.User
import com.example.cms.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.nio.file.Paths
import java.util.*

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val s3Client: S3Client,
    @Value("\${s3.bucket.avatar}") private val avatarBucket: String,
    @Value("\${avatar.upload.base-url}") private val avatarBaseUrl: String
) {

    companion object {
        private val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
        )

        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    }

    /**
     * Upload avatar per l'utente autenticato
     */
    fun uploadAvatar(file: MultipartFile, username: String): String {
        validateFile(file)

        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Elimina il vecchio avatar se esiste
        user.avatarPath?.let { oldPath ->
            try {
                // Estrai il filename dall'S3 path (formato: s3://bucket/filename)
                val oldFilename = oldPath.substringAfterLast("/")
                val deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(avatarBucket)
                    .key(oldFilename)
                    .build()
                s3Client.deleteObject(deleteObjectRequest)
            } catch (e: Exception) {
                // Log ma continua
            }
        }

        val originalFilename = file.originalFilename ?: throw IllegalArgumentException("Invalid file")
        val extension = getFileExtension(originalFilename)
        val newFilename = "${user.id}_${UUID.randomUUID()}.$extension"

        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(avatarBucket)
                .key(newFilename)
                .contentType(file.contentType)
                .contentLength(file.size)
                .build()

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.inputStream, file.size))

            // Aggiorna il path dell'avatar nell'utente
            val updatedUser = user.copy(
                avatarPath = "/$avatarBucket/$newFilename",
                updatedAt = java.time.LocalDateTime.now()
            )

            userRepository.save(updatedUser)

            return "$avatarBaseUrl/$newFilename"

        } catch (e: Exception) {
            throw IllegalStateException("Failed to store avatar on S3: ${e.message}")
        }
    }

    /**
     * Ottieni l'URL dell'avatar dell'utente
     */
    fun getAvatarUrl(username: String): String? {
        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        return user.avatarPath?.let {
            val filename = Paths.get(it).fileName.toString()
            "$avatarBaseUrl/$filename"
        }
    }

    /**
     * Carica il file avatar come Resource per serving
     */
    fun loadAvatarAsResource(filename: String): Resource {
        try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(avatarBucket)
                .key(filename)
                .build()

            val objectBytes = s3Client.getObject(getObjectRequest).readAllBytes()
            return ByteArrayResource(objectBytes)

        } catch (e: Exception) {
            throw IllegalArgumentException("Avatar not found on S3: $filename", e)
        }
    }

    /**
     * Elimina l'avatar dell'utente
     */
    fun deleteAvatar(username: String) {
        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        user.avatarPath?.let { avatarPath ->
            try {
                // Estrai il filename dall'S3 path (formato: s3://bucket/filename)
                val filename = avatarPath.substringAfterLast("/")
                val deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(avatarBucket)
                    .key(filename)
                    .build()

                s3Client.deleteObject(deleteObjectRequest)

                val updatedUser = user.copy(
                    avatarPath = null,
                    updatedAt = java.time.LocalDateTime.now()
                )

                userRepository.save(updatedUser)

            } catch (e: Exception) {
                throw IllegalStateException("Failed to delete avatar from S3: ${e.message}")
            }
        } ?: throw IllegalArgumentException("User has no avatar")
    }

    /**
     * Ottieni informazioni utente per username
     */
    fun getUserByUsername(username: String): User {
        return userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw IllegalArgumentException("Cannot upload empty file")
        }

        val mimeType = file.contentType
        if (mimeType !in ALLOWED_MIME_TYPES) {
            throw IllegalArgumentException(
                "Invalid file type. Allowed types: jpg, jpeg, png, gif, webp"
            )
        }

        val filename = file.originalFilename ?: throw IllegalArgumentException("Invalid filename")
        val extension = getFileExtension(filename)
        if (extension !in ALLOWED_EXTENSIONS) {
            throw IllegalArgumentException(
                "Invalid file extension. Allowed extensions: jpg, jpeg, png, gif, webp"
            )
        }

        val maxSize = 5 * 1024 * 1024L // 5MB per gli avatar
        if (file.size > maxSize) {
            throw IllegalArgumentException("File size exceeds maximum limit of 5MB")
        }
    }

    private fun getFileExtension(filename: String): String {
        val lastDotIndex = filename.lastIndexOf('.')
        if (lastDotIndex == -1 || lastDotIndex == filename.length - 1) {
            throw IllegalArgumentException("File has no extension")
        }
        return filename.substring(lastDotIndex + 1).lowercase()
    }
}
