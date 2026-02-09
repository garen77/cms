package com.example.cms.dto

import com.example.cms.model.ContentStatus
import java.time.LocalDateTime

data class ContentRequest(
    val title: String,
    val slug: String,
    val categoryId: Long?,
    val excerpt: String?,
    val body: String,
    val featuredImageId: Int?,
    val status: ContentStatus,
    val tags: List<String> = listOf()
)

data class ContentResponse(
    val id: Int,
    val title: String,
    val slug: String,
    val author: AuthorResponse?,
    val category: CategoryResponse?,
    val excerpt: String?,
    val body: String,
    val featuredImage: MediaResponse?,
    val status: String,
    val publishedAt: LocalDateTime?,
    val viewCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val tags: List<TagResponse>
)

data class AuthorResponse(
    val id: Int,
    val username: String,
    val firstName: String?,
    val lastName: String?
)

data class CategoryResponse(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String?
)

data class TagResponse(
    val id: Int,
    val name: String,
    val slug: String
)

data class CommentRequest(
    val contentId: Int,
    val authorName: String?,
    val authorEmail: String?,
    val body: String
)

data class CommentResponse(
    val id: Int,
    val authorName: String?,
    val body: String,
    val status: String,
    val createdAt: LocalDateTime
)
