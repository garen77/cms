package com.example.cms.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class SendMessageRequest(
    val recipientId: Int,
    @field:NotBlank val content: String
)

data class ChatMessageResponse(
    val id: Long?,
    val senderId: Int?,
    val senderUsername: String,
    val recipientId: Int?,
    val recipientUsername: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime
)

data class ConversationSummary(
    val userId: Int?,
    val username: String,
    val lastMessage: String?,
    val unreadCount: Long,
    val updatedAt: LocalDateTime?
)
