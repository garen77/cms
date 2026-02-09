package com.example.cms.service

import com.example.cms.dto.*
import com.example.cms.model.Content
import com.example.cms.model.ContentStatus
import com.example.cms.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ContentService(
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val mediaRepository: MediaRepository
) {
    private val logger = LoggerFactory.getLogger(ContentService::class.java)

    fun getAllContents(pageable: Pageable, username: String?): Page<ContentResponse> {
        val contents = contentRepository.findAllContents(pageable)
        return filterContentsByPermissions(contents, username)
    }

    fun getAllPublishedContents(pageable: Pageable): Page<ContentResponse> {
        return contentRepository.findPublishedContents(pageable)
            .map { it.toResponse() }
    }

    fun getContentBySlug(slug: String, username: String?): ContentResponse {
        val content = contentRepository.findBySlug(slug)
            .orElseThrow { IllegalArgumentException("Content not found") }

        // Controllo di accesso
        if (content.status != ContentStatus.PUBLISHED) {
            // Content non pubblicato: verifica autorizzazione
            if (username == null) {
                throw IllegalArgumentException("Content not found")
            }

            val currentUser = userRepository.findByUsername(username)
                .orElseThrow { IllegalArgumentException("Content not found") }

            // Permetti accesso solo se:
            // 1. Sei l'autore del content
            // 2. Oppure sei un ADMIN
            val isAuthor = content.author?.id == currentUser.id
            val isAdmin = currentUser.role == com.example.cms.model.UserRole.ADMIN

            if (!isAuthor && !isAdmin) {
                logger.warn("User ${currentUser.username} attempted to access unpublished content: $slug")
                throw IllegalArgumentException("Content not found")
            }

            logger.info("User ${currentUser.username} accessing unpublished content: $slug (status: ${content.status})")
        }

        // Debug: verifica se featuredImage è caricato
        logger.info("Content found - ID: ${content.id}, Title: ${content.title}, Status: ${content.status}")
        logger.info("FeaturedImage is null: ${content.featuredImage == null}")
        if (content.featuredImage != null) {
            logger.info("FeaturedImage loaded - ID: ${content.featuredImage?.id}, Filename: ${content.featuredImage?.filename}")
        }

        // Converti a response PRIMA di salvare (per mantenere le relazioni lazy-loaded)
        val response = content.toResponse()

        // Increment view count solo per content pubblicati
        if (content.status == ContentStatus.PUBLISHED) {
            contentRepository.save(content.copy(viewCount = content.viewCount + 1))
        }

        return response
    }

    fun createContent(request: ContentRequest, authorUsername: String): ContentResponse {
        val author = userRepository.findByUsername(authorUsername)
            .orElseThrow { IllegalArgumentException("Author not found") }

        val category = request.categoryId?.let {
            categoryRepository.findById(it)
                .orElseThrow { IllegalArgumentException("Category not found") }
        }

        val tags = if (request.tags.isNotEmpty()) {
            tagRepository.findByNameIn(request.tags)
        } else {
            listOf()
        }

        val featuredImage = request.featuredImageId?.let {
            mediaRepository.findById(it.toLong())
                .orElseThrow { IllegalArgumentException("Featured image not found") }
        }

        val content = Content(
            title = request.title,
            slug = request.slug,
            author = author,
            category = category,
            excerpt = request.excerpt,
            body = request.body,
            featuredImage = featuredImage,
            status = request.status,
            publishedAt = if (request.status == ContentStatus.PUBLISHED) LocalDateTime.now() else null,
            tags = tags
        )

        return contentRepository.save(content).toResponse()
    }

    fun updateContent(id: Long, request: ContentRequest): ContentResponse {
        val content = contentRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Content not found") }

        val category = request.categoryId?.let {
            categoryRepository.findById(it)
                .orElseThrow { IllegalArgumentException("Category not found") }
        }

        val tags = if (request.tags.isNotEmpty()) {
            tagRepository.findByNameIn(request.tags)
        } else {
            listOf()
        }

        val featuredImage = request.featuredImageId?.let {
            mediaRepository.findById(it.toLong())
                .orElseThrow { IllegalArgumentException("Featured image not found") }
        }

        val updatedContent = content.copy(
            title = request.title,
            slug = request.slug,
            category = category,
            excerpt = request.excerpt,
            body = request.body,
            featuredImage = featuredImage,
            status = request.status,
            publishedAt = if (request.status == ContentStatus.PUBLISHED && content.publishedAt == null)
                LocalDateTime.now() else content.publishedAt,
            tags = tags,
            updatedAt = LocalDateTime.now()
        )

        return contentRepository.save(updatedContent).toResponse()
    }

    fun deleteContent(id: Long) {
        contentRepository.deleteById(id)
    }

    /**
     * Filtra i content in base ai permessi dell'utente:
     * - PUBLISHED: visibili a tutti
     * - DRAFT/ARCHIVED: visibili solo all'autore o agli admin
     */
    private fun filterContentsByPermissions(contents: Page<Content>, username: String?): Page<ContentResponse> {
        // Se l'utente non è autenticato, mostra solo i content PUBLISHED
        if (username == null) {
            val filteredContent = contents.content
                .filter { it.status == ContentStatus.PUBLISHED }
                .map { it.toResponse() }

            return PageImpl(filteredContent, contents.pageable, filteredContent.size.toLong())
        }

        // Ottieni l'utente autenticato
        val currentUser = userRepository.findByUsername(username).orElse(null)
        val isAdmin = currentUser?.role == com.example.cms.model.UserRole.ADMIN

        // Filtra i content in base ai permessi
        val filteredContent = contents.content
            .filter { content ->
                when (content.status) {
                    ContentStatus.PUBLISHED -> true
                    ContentStatus.DRAFT, ContentStatus.ARCHIVED -> {
                        // Mostra solo se sei l'autore o admin
                        isAdmin || content.author?.id == currentUser?.id
                    }
                }
            }
            .map { it.toResponse() }

        return PageImpl(filteredContent, contents.pageable, filteredContent.size.toLong())
    }

    fun getContentsByCategory(categoryId: Long, pageable: Pageable, username: String?): Page<ContentResponse> {
        val contents = contentRepository.findByCategoryId(categoryId, pageable)
        return filterContentsByPermissions(contents, username)
    }

    fun getContentsByTag(tagId: Long, pageable: Pageable, username: String?): Page<ContentResponse> {
        val contents = contentRepository.findByTagId(tagId, pageable)
        return filterContentsByPermissions(contents, username)
    }

    fun getContentsByAuthor(authorId: Long, pageable: Pageable, username: String?): Page<ContentResponse> {
        val contents = contentRepository.findByAuthorId(authorId, pageable)
        return filterContentsByPermissions(contents, username)
    }

    fun getContentsByCurrentUser(username: String, pageable: Pageable): Page<ContentResponse> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        return contentRepository.findByAuthorId(user.id!!.toLong(), pageable)
            .map { it.toResponse() }
    }

    private fun Content.toResponse() = ContentResponse(
        id = id!!,
        title = title,
        slug = slug,
        author = author?.let {
            AuthorResponse(
                id = it.id!!,
                username = it.username,
                firstName = it.firstName,
                lastName = it.lastName
            )
        },
        category = category?.let {
            CategoryResponse(
                id = it.id!!,
                name = it.name,
                slug = it.slug,
                description = it.description
            )
        },
        excerpt = excerpt,
        body = body,
        featuredImage = featuredImage?.let {
            MediaResponse(
                id = it.id!!,
                filename = it.filename,
                originalFilename = it.originalFilename,
                fileUrl = it.fileUrl ?: "",
                mimeType = it.mimeType,
                fileSize = it.fileSize,
                uploadedBy = it.uploadedBy?.let { user ->
                    MediaUploaderResponse(
                        id = user.id!!,
                        username = user.username
                    )
                },
                createdAt = it.createdAt
            )
        },
        status = status.name,
        publishedAt = publishedAt,
        viewCount = viewCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags.map { TagResponse(it.id!!, it.name, it.slug) }
    )
}
