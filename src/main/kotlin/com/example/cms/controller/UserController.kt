package com.example.cms.controller

import com.example.cms.service.UserService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    /**
     * Upload avatar per l'utente autenticato
     */
    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(
        @RequestParam("file") file: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        return try {
            val avatarUrl = userService.uploadAvatar(file, authentication.name)

            ResponseEntity.ok(
                mapOf(
                    "success" to "true",
                    "message" to "Avatar uploaded successfully",
                    "avatarUrl" to avatarUrl
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to "false",
                    "message" to (e.message ?: "Invalid file")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "success" to "false",
                    "message" to "Failed to upload avatar: ${e.message}"
                )
            )
        }
    }

    /**
     * Serve l'avatar per filename
     * Endpoint pubblico
     */
    @GetMapping("/avatars/{filename:.+}")
    fun serveAvatar(@PathVariable filename: String): ResponseEntity<Resource> {
        return try {
            val resource = userService.loadAvatarAsResource(filename)

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

    /**
     * Ottieni URL avatar dell'utente autenticato
     */
    @GetMapping("/avatar")
    fun getMyAvatar(authentication: Authentication): ResponseEntity<Map<String, String?>> {
        return try {
            val avatarUrl = userService.getAvatarUrl(authentication.name)

            ResponseEntity.ok(
                mapOf(
                    "avatarUrl" to avatarUrl
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Elimina avatar dell'utente autenticato
     */
    @DeleteMapping("/avatar")
    fun deleteAvatar(authentication: Authentication): ResponseEntity<Map<String, String>> {
        return try {
            userService.deleteAvatar(authentication.name)

            ResponseEntity.ok(
                mapOf(
                    "success" to "true",
                    "message" to "Avatar deleted successfully"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to "false",
                    "message" to (e.message ?: "Failed to delete avatar")
                )
            )
        }
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
