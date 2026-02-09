package com.example.cms.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(unique = true, nullable = false, length = 100)
    val slug: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "category")
    val contents: List<Content> = listOf()
)
