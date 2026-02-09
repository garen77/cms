package com.example.cms.controller

import com.example.cms.dto.ContentRequest
import com.example.cms.dto.ContentResponse
import com.example.cms.service.ContentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/contents")
class ContentController(
    private val contentService: ContentService
) {

    @GetMapping
    fun getAllContents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        authentication: Authentication?
    ): ResponseEntity<Page<ContentResponse>> {
        val username = authentication?.name
        return ResponseEntity.ok(
            contentService.getAllContents(PageRequest.of(page, size), username)
        )
    }

    @GetMapping("/published")
    fun getAllPublishedContents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<ContentResponse>> {
        return ResponseEntity.ok(
            contentService.getAllPublishedContents(PageRequest.of(page, size))
        )
    }

    @GetMapping("/slug/{slug}")
    fun getContentBySlug(
        @PathVariable slug: String,
        authentication: Authentication?
    ): ResponseEntity<ContentResponse> {
        val username = authentication?.name
        return ResponseEntity.ok(contentService.getContentBySlug(slug, username))
    }

    @PostMapping
    fun createContent(
        @RequestBody request: ContentRequest,
        authentication: Authentication
    ): ResponseEntity<ContentResponse> {
        return ResponseEntity.ok(
            contentService.createContent(request, authentication.name)
        )
    }

    @PutMapping("/{id}")
    fun updateContent(
        @PathVariable id: Long,
        @RequestBody request: ContentRequest
    ): ResponseEntity<ContentResponse> {
        return ResponseEntity.ok(contentService.updateContent(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteContent(@PathVariable id: Long): ResponseEntity<Void> {
        contentService.deleteContent(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/category/{categoryId}")
    fun getContentsByCategory(
        @PathVariable categoryId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        authentication: Authentication?
    ): ResponseEntity<Page<ContentResponse>> {
        val username = authentication?.name
        return ResponseEntity.ok(
            contentService.getContentsByCategory(categoryId, PageRequest.of(page, size), username)
        )
    }

    @GetMapping("/tag/{tagId}")
    fun getContentsByTag(
        @PathVariable tagId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        authentication: Authentication?
    ): ResponseEntity<Page<ContentResponse>> {
        val username = authentication?.name
        return ResponseEntity.ok(
            contentService.getContentsByTag(tagId, PageRequest.of(page, size), username)
        )
    }

    @GetMapping("/author/{authorId}")
    fun getContentsByAuthor(
        @PathVariable authorId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        authentication: Authentication?
    ): ResponseEntity<Page<ContentResponse>> {
        val username = authentication?.name
        return ResponseEntity.ok(
            contentService.getContentsByAuthor(authorId, PageRequest.of(page, size), username)
        )
    }

    @GetMapping("/my-contents")
    fun getMyContents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        authentication: Authentication
    ): ResponseEntity<Page<ContentResponse>> {
        return ResponseEntity.ok(
            contentService.getContentsByCurrentUser(authentication.name, PageRequest.of(page, size))
        )
    }
}
