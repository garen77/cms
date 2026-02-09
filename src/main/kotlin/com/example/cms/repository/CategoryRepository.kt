package com.example.cms.repository

import com.example.cms.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun findBySlug(slug: String): Optional<Category>
}
