package com.example.cms.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(unique = true, nullable = false, length = 50)
    val username: String,

    @Column(unique = true, nullable = false, length = 100)
    val email: String,

    @Column(nullable = false)
    val passwordHash: String,

    @Column(length = 50)
    val firstName: String? = null,

    @Column(length = 50)
    val lastName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val role: UserRole = UserRole.AUTHOR,

    @Column(length = 500)
    val avatarPath: String? = null,

    val isActive: Boolean = true,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "author")
    val contents: List<Content> = listOf()
)

enum class UserRole {
    ADMIN, EDITOR, AUTHOR, SUBSCRIBER
}
