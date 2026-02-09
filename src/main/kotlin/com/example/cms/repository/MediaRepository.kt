package com.example.cms.repository

import com.example.cms.model.Media
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MediaRepository : JpaRepository<Media, Long> {
    fun findByUploadedById(userId: Long): List<Media>
    fun findByFilename(filename: String): java.util.Optional<Media>
}
