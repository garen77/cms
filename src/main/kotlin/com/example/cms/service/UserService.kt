package com.example.cms.service

import com.example.cms.model.User
import com.example.cms.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    @Value("\${avatar.upload.directory}") private val avatarDirectory: String,
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

    init {
        try {
            val avatarPath = Paths.get(avatarDirectory)
            if (!Files.exists(avatarPath)) {
                Files.createDirectories(avatarPath)
            }
        } catch (e: IOException) {
            throw IllegalStateException("Could not create avatar directory: ${e.message}")
        }
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
                Files.deleteIfExists(Paths.get(oldPath))
            } catch (e: IOException) {
                // Log ma continua
            }
        }

        val originalFilename = file.originalFilename ?: throw IllegalArgumentException("Invalid file")
        val extension = getFileExtension(originalFilename)
        val newFilename = "${user.id}_${UUID.randomUUID()}.$extension"

        try {
            val targetLocation = Paths.get(avatarDirectory).resolve(newFilename)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

            // Aggiorna il path dell'avatar nell'utente
            val updatedUser = user.copy(
                avatarPath = targetLocation.toString(),
                updatedAt = java.time.LocalDateTime.now()
            )

            userRepository.save(updatedUser)

            return "$avatarBaseUrl/$newFilename"

        } catch (e: IOException) {
            throw IllegalStateException("Failed to store avatar: ${e.message}")
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
            val filePath = Paths.get(avatarDirectory).resolve(filename).normalize()
            val resource = UrlResource(filePath.toUri())

            if (resource.exists() && resource.isReadable) {
                return resource
            } else {
                throw IllegalArgumentException("Avatar not found: $filename")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Avatar not found: $filename", e)
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
                Files.deleteIfExists(Paths.get(avatarPath))

                val updatedUser = user.copy(
                    avatarPath = null,
                    updatedAt = java.time.LocalDateTime.now()
                )

                userRepository.save(updatedUser)

            } catch (e: IOException) {
                throw IllegalStateException("Failed to delete avatar: ${e.message}")
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
