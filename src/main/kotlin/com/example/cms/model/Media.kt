package com.example.cms.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "media")
data class Media(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    val filename: String,

    @Column(nullable = false, length = 255)
    val originalFilename: String,

    @Column(nullable = false, length = 500)
    val filePath: String,

    @Column(length = 500)
    val fileUrl: String? = null,

    @Column(length = 100)
    val mimeType: String? = null,

    val fileSize: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    val uploadedBy: User? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
