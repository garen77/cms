package com.example.cms.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tags")
data class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(unique = true, nullable = false, length = 50)
    val name: String,

    @Column(unique = true, nullable = false, length = 50)
    val slug: String,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany(mappedBy = "tags")
    val contents: List<Content> = listOf()
)
