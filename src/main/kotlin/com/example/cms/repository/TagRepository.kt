package com.example.cms.repository

import com.example.cms.model.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TagRepository : JpaRepository<Tag, Long> {
    fun findBySlug(slug: String): Optional<Tag>
    fun findByNameIn(names: List<String>): List<Tag>
}
