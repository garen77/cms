package com.example.cms.dto

import com.example.cms.model.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class RegisterRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String,

    @field:Size(max = 50, message = "First name must not exceed 50 characters")
    val firstName: String? = null,

    @field:Size(max = 50, message = "Last name must not exceed 50 characters")
    val lastName: String? = null,

    val role: UserRole? = null
)

data class AuthResponse(
    val token: String,
    val type: String = "Bearer",
    val sessionId: String,
    val id: Int?,
    val username: String,
    val email: String,
    val role: String,
    val avatarUrl: String?
)
