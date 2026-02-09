package com.example.cms.repository

import com.example.cms.model.Content
import com.example.cms.model.ContentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContentRepository : JpaRepository<Content, Long> {
    @Query("""
        SELECT DISTINCT c FROM Content c
        LEFT JOIN FETCH c.featuredImage
        LEFT JOIN FETCH c.author
        LEFT JOIN FETCH c.category
        WHERE c.slug = :slug
    """)
    fun findBySlug(slug: String): Optional<Content>

    @EntityGraph(attributePaths = ["featuredImage", "author", "category"])
    fun findByStatus(status: ContentStatus, pageable: Pageable): Page<Content>

    @EntityGraph(attributePaths = ["featuredImage", "author", "category"])
    @Query("SELECT c FROM Content c ORDER BY c.createdAt DESC")
    fun findAllContents(pageable: Pageable): Page<Content>

    @EntityGraph(attributePaths = ["featuredImage", "author", "category"])
    @Query("SELECT c FROM Content c WHERE c.status = 'PUBLISHED' ORDER BY c.publishedAt DESC")
    fun findPublishedContents(pageable: Pageable): Page<Content>

    @EntityGraph(attributePaths = ["featuredImage", "author", "category"])
    @Query("SELECT c FROM Content c WHERE c.category.id = :categoryId")
    fun findByCategoryId(categoryId: Long, pageable: Pageable): Page<Content>

    @EntityGraph(attributePaths = ["featuredImage", "author", "category"])
    @Query("SELECT c FROM Content c WHERE c.author.id = :authorId")
    fun findByAuthorId(authorId: Long, pageable: Pageable): Page<Content>

    @EntityGraph(attributePaths = ["featuredImage", "author", "category"])
    @Query("SELECT c FROM Content c JOIN c.tags t WHERE t.id = :tagId AND c.status = 'PUBLISHED'")
    fun findByTagId(tagId: Long, pageable: Pageable): Page<Content>
}
