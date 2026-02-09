package com.example.cms.service

import com.example.cms.config.JwtTokenProvider
import com.example.cms.dto.AuthResponse
import com.example.cms.dto.LoginRequest
import com.example.cms.dto.RegisterRequest
import com.example.cms.model.User
import com.example.cms.model.UserRole
import com.example.cms.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.nio.file.Paths

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${avatar.upload.base-url}") private val avatarBaseUrl: String
) {

    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            role = request.role ?: UserRole.AUTHOR
        )

        userRepository.save(user)

        return login(LoginRequest(request.username, request.password))
    }

    fun login(request: LoginRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        val token = jwtTokenProvider.generateToken(authentication)
        val sessionId = jwtTokenProvider.getSessionIdFromToken(token) ?: "unknown"
        val user = userRepository.findByUsername(request.username)
            .orElseThrow { IllegalArgumentException("User not found") }

        val avatarUrl = user.avatarPath?.let {
            val filename = Paths.get(it).fileName.toString()
            "$avatarBaseUrl/$filename"
        }

        return AuthResponse(
            token = token,
            sessionId = sessionId,
            username = user.username,
            id = user.id,
            email = user.email,
            role = user.role.name,
            avatarUrl = avatarUrl
        )
    }
}
