package com.example.cms.service

import com.example.cms.dto.MediaResponse
import com.example.cms.dto.MediaUploaderResponse
import com.example.cms.model.Media
import com.example.cms.repository.MediaRepository
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
import java.util.*

@Service
@Transactional
class MediaService(
    private val mediaRepository: MediaRepository,
    private val userRepository: UserRepository,
    private val s3Client: S3Client,
    @Value("\${s3.bucket.media}") private val mediaBucket: String,
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

    fun uploadFile(file: MultipartFile, username: String): Media {
        validateFile(file)

        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        val originalFilename = file.originalFilename ?: throw IllegalArgumentException("Invalid file")
        val extension = getFileExtension(originalFilename)
        val newFilename = "${UUID.randomUUID()}.$extension"

        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(mediaBucket)
                .key(newFilename)
                .contentType(file.contentType)
                .contentLength(file.size)
                .build()

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.inputStream, file.size))

            val media = Media(
                filename = newFilename,
                originalFilename = originalFilename,
                filePath = "/$mediaBucket/$newFilename",
                fileUrl = "$baseUrl/$newFilename",
                mimeType = file.contentType,
                fileSize = file.size,
                uploadedBy = user
            )

            return mediaRepository.save(media)

        } catch (e: Exception) {
            throw IllegalStateException("Failed to store file on S3: ${e.message}")
        }
    }

    fun getMediaById(id: Int): Media {
        return mediaRepository.findById(id.toLong())
            .orElseThrow { IllegalArgumentException("Media not found with id: $id") }
    }

    fun loadFileAsResource(filename: String): Resource {
        try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(mediaBucket)
                .key(filename)
                .build()

            val objectBytes = s3Client.getObject(getObjectRequest).readAllBytes()
            return ByteArrayResource(objectBytes)

        } catch (e: Exception) {
            throw IllegalArgumentException("File not found on S3: $filename", e)
        }
    }

    fun deleteFile(mediaId: Int, username: String) {
        val media = getMediaById(mediaId)

        if (media.uploadedBy?.username != username) {
            throw IllegalArgumentException("You are not authorized to delete this file")
        }

        try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(mediaBucket)
                .key(media.filename)
                .build()

            s3Client.deleteObject(deleteObjectRequest)
            mediaRepository.deleteById(mediaId.toLong())

        } catch (e: Exception) {
            throw IllegalStateException("Failed to delete file from S3: ${e.message}")
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
        fileUrl = filePath.substringAfterLast("/").let { "$baseUrl/$it" },
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
