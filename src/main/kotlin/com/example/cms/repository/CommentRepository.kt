package com.example.cms.repository

import com.example.cms.model.Comment
import com.example.cms.model.CommentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    fun findByContentId(contentId: Long): List<Comment>
    fun findByStatus(status: CommentStatus): List<Comment>
}
