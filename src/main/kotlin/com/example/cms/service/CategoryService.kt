package com.example.cms.service

import com.example.cms.dto.CategoryResponse
import com.example.cms.model.Category
import com.example.cms.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    fun getAllCategories(): List<CategoryResponse> {
        return categoryRepository.findAll().map { it.toResponse() }
    }

    fun getCategoryById(id: Long): CategoryResponse {
        return categoryRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Category not found") }
            .toResponse()
    }

    fun createCategory(name: String, slug: String, description: String?): CategoryResponse {
        val category = Category(
            name = name,
            slug = slug,
            description = description
        )
        return categoryRepository.save(category).toResponse()
    }

    fun updateCategory(id: Long, name: String, slug: String, description: String?): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Category not found") }

        val updated = category.copy(
            name = name,
            slug = slug,
            description = description
        )

        return categoryRepository.save(updated).toResponse()
    }

    fun deleteCategory(id: Long) {
        categoryRepository.deleteById(id)
    }

    private fun Category.toResponse() = CategoryResponse(
        id = id!!,
        name = name,
        slug = slug,
        description = description
    )
}
