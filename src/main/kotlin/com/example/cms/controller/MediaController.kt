package com.example.cms.controller

import com.example.cms.dto.MediaResponse
import com.example.cms.dto.MediaUploadResponse
import com.example.cms.dto.MediaUploaderResponse
import com.example.cms.service.MediaService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/media")
class MediaController(
    private val mediaService: MediaService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadMedia(
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<MediaUploadResponse> {
        return try {
            val media = mediaService.uploadFile(file, authentication.name)

            ResponseEntity.ok(
                MediaUploadResponse(
                    success = true,
                    message = "File uploaded successfully",
                    media = MediaResponse(
                        id = media.id!!,
                        filename = media.filename,
                        originalFilename = media.originalFilename,
                        fileUrl = media.fileUrl ?: "",
                        mimeType = media.mimeType,
                        fileSize = media.fileSize,
                        uploadedBy = media.uploadedBy?.let {
                            MediaUploaderResponse(
                                id = it.id!!,
                                username = it.username
                            )
                        },
                        createdAt = media.createdAt
                    )
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                MediaUploadResponse(
                    success = false,
                    message = e.message ?: "Invalid file",
                    media = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                MediaUploadResponse(
                    success = false,
                    message = "Failed to upload file: ${e.message}",
                    media = null
                )
            )
        }
    }

    @GetMapping("/{filename:.+}")
    fun serveFile(@PathVariable filename: String): ResponseEntity<Resource> {
        return try {
            val resource = mediaService.loadFileAsResource(filename)

            val contentType = determineContentType(filename)

            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"$filename\""
                )
                .body(resource)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/info/{id}")
    fun getMediaInfo(@PathVariable id: Int): ResponseEntity<MediaResponse> {
        return try {
            val media = mediaService.getMediaById(id)
            ResponseEntity.ok(
                MediaResponse(
                    id = media.id!!,
                    filename = media.filename,
                    originalFilename = media.originalFilename,
                    fileUrl = media.fileUrl ?: "",
                    mimeType = media.mimeType,
                    fileSize = media.fileSize,
                    uploadedBy = media.uploadedBy?.let {
                        MediaUploaderResponse(
                            id = it.id!!,
                            username = it.username
                        )
                    },
                    createdAt = media.createdAt
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteMedia(
        @PathVariable id: Int,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        return try {
            mediaService.deleteFile(id, authentication.name)
            ResponseEntity.ok(
                mapOf(
                    "success" to "true",
                    "message" to "File deleted successfully"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                mapOf(
                    "success" to "false",
                    "message" to e.message!!
                )
            )
        }
    }

    @GetMapping("/my-uploads")
    fun getMyUploads(authentication: Authentication): ResponseEntity<List<MediaResponse>> {
        return ResponseEntity.ok(mediaService.getMediaByUser(authentication.name))
    }

    private fun determineContentType(filename: String): String {
        return when (filename.substringAfterLast('.').lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}
