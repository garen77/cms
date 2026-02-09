package com.example.cms.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    val content: Content,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    val author: User? = null,

    @Column(length = 100)
    val authorName: String? = null,

    @Column(length = 100)
    val authorEmail: String? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    val body: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val status: CommentStatus = CommentStatus.PENDING,

    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class CommentStatus {
    PENDING, APPROVED, REJECTED
}
