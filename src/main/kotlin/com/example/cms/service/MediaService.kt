package com.example.cms.service

import com.example.cms.dto.MediaResponse
import com.example.cms.dto.MediaUploaderResponse
import com.example.cms.model.Media
import com.example.cms.repository.MediaRepository
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
class MediaService(
    private val mediaRepository: MediaRepository,
    private val userRepository: UserRepository,
    @Value("\${media.upload.directory}") private val uploadDirectory: String,
    @Value("\${media.upload.base-url}") private val baseUrl: String
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
            val uploadPath = Paths.get(uploadDirectory)
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath)
            }
        } catch (e: IOException) {
            throw IllegalStateException("Could not create upload directory: ${e.message}")
        }
    }

    fun uploadFile(file: MultipartFile, username: String): Media {
        validateFile(file)

        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        val originalFilename = file.originalFilename ?: throw IllegalArgumentException("Invalid file")
        val extension = getFileExtension(originalFilename)
        val newFilename = "${UUID.randomUUID()}.$extension"

        try {
            val targetLocation = Paths.get(uploadDirectory).resolve(newFilename)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

            val media = Media(
                filename = newFilename,
                originalFilename = originalFilename,
                filePath = targetLocation.toString(),
                fileUrl = "$baseUrl/$newFilename",
                mimeType = file.contentType,
                fileSize = file.size,
                uploadedBy = user
            )

            return mediaRepository.save(media)

        } catch (e: IOException) {
            throw IllegalStateException("Failed to store file: ${e.message}")
        }
    }

    fun getMediaById(id: Int): Media {
        return mediaRepository.findById(id.toLong())
            .orElseThrow { IllegalArgumentException("Media not found with id: $id") }
    }

    fun loadFileAsResource(filename: String): Resource {
        try {
            val filePath = Paths.get(uploadDirectory).resolve(filename).normalize()
            val resource = UrlResource(filePath.toUri())

            if (resource.exists() && resource.isReadable) {
                return resource
            } else {
                throw IllegalArgumentException("File not found: $filename")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("File not found: $filename", e)
        }
    }

    fun deleteFile(mediaId: Int, username: String) {
        val media = getMediaById(mediaId)

        if (media.uploadedBy?.username != username) {
            throw IllegalArgumentException("You are not authorized to delete this file")
        }

        try {
            val filePath = Paths.get(media.filePath)
            Files.deleteIfExists(filePath)

            mediaRepository.deleteById(mediaId.toLong())

        } catch (e: IOException) {
            throw IllegalStateException("Failed to delete file: ${e.message}")
        }
    }

    fun getMediaByUser(username: String): List<MediaResponse> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        return mediaRepository.findByUploadedById(user.id!!.toLong())
            .map { it.toResponse() }
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

        val maxSize = 10 * 1024 * 1024L
        if (file.size > maxSize) {
            throw IllegalArgumentException("File size exceeds maximum limit of 10MB")
        }
    }

    private fun getFileExtension(filename: String): String {
        val lastDotIndex = filename.lastIndexOf('.')
        if (lastDotIndex == -1 || lastDotIndex == filename.length - 1) {
            throw IllegalArgumentException("File has no extension")
        }
        return filename.substring(lastDotIndex + 1).lowercase()
    }

    private fun Media.toResponse() = MediaResponse(
        id = id!!,
        filename = filename,
        originalFilename = originalFilename,
        fileUrl = fileUrl ?: "",
        mimeType = mimeType,
        fileSize = fileSize,
        uploadedBy = uploadedBy?.let {
            MediaUploaderResponse(
                id = it.id!!,
                username = it.username
            )
        },
        createdAt = createdAt
    )
}
