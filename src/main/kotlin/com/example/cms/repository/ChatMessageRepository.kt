package com.example.cms.repository

import com.example.cms.model.ChatMessage
import com.example.cms.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT m FROM ChatMessage m
        JOIN FETCH m.sender JOIN FETCH m.recipient
        WHERE (m.sender = :user1 AND m.recipient = :user2)
           OR (m.sender = :user2 AND m.recipient = :user1)
        ORDER BY m.createdAt ASC
    """)
    fun findConversation(
        @Param("user1") user1: User,
        @Param("user2") user2: User,
        pageable: Pageable
    ): Page<ChatMessage>

    @Query(
        value = """
            SELECT DISTINCT
                CASE WHEN sender_id = :userId THEN recipient_id ELSE sender_id END AS partner_id
            FROM chat_messages
            WHERE sender_id = :userId OR recipient_id = :userId
        """,
        nativeQuery = true
    )
    fun findConversationPartners(@Param("userId") userId: Int): List<Int>

    @Query(
        value = """
            SELECT * FROM chat_messages
            WHERE (sender_id = :userId AND recipient_id = :partnerId)
               OR (sender_id = :partnerId AND recipient_id = :userId)
            ORDER BY created_at DESC
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findLastMessageBetween(
        @Param("userId") userId: Int,
        @Param("partnerId") partnerId: Int
    ): ChatMessage?

    fun countByRecipientIdAndSenderIdAndIsReadFalse(recipientId: Int, senderId: Int): Long

    @Modifying
    @Query("""
        UPDATE ChatMessage m SET m.isRead = true
        WHERE m.sender.id = :senderId AND m.recipient.id = :recipientId AND m.isRead = false
    """)
    fun markAsRead(@Param("senderId") senderId: Int, @Param("recipientId") recipientId: Int)
}
