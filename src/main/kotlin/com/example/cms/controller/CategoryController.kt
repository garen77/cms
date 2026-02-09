package com.example.cms.controller

import com.example.cms.dto.CategoryResponse
import com.example.cms.service.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun getAllCategories(): ResponseEntity<List<CategoryResponse>> {
        return ResponseEntity.ok(categoryService.getAllCategories())
    }

    @GetMapping("/{id}")
    fun getCategoryById(@PathVariable id: Long): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.getCategoryById(id))
    }

    @PostMapping
    fun createCategory(
        @RequestParam name: String,
        @RequestParam slug: String,
        @RequestParam(required = false) description: String?
    ): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.createCategory(name, slug, description))
    }

    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @RequestParam name: String,
        @RequestParam slug: String,
        @RequestParam(required = false) description: String?
    ): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.updateCategory(id, name, slug, description))
    }

    @DeleteMapping("/{id}")
    fun deleteCategory(@PathVariable id: Long): ResponseEntity<Void> {
        categoryService.deleteCategory(id)
        return ResponseEntity.noContent().build()
    }
}
