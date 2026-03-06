package com.example.cms.service

import com.example.cms.dto.ChatMessageResponse
import com.example.cms.dto.ConversationSummary
import com.example.cms.dto.SendMessageRequest
import com.example.cms.model.ChatMessage
import com.example.cms.repository.ChatMessageRepository
import com.example.cms.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun sendMessage(senderUsername: String, request: SendMessageRequest): ChatMessageResponse {
        val sender = userRepository.findByUsername(senderUsername)
            .orElseThrow { NoSuchElementException("Sender not found: $senderUsername") }
        val recipient = userRepository.findById(request.recipientId.toLong())
            .orElseThrow { NoSuchElementException("Recipient not found: ${request.recipientId}") }

        val saved = chatMessageRepository.save(
            ChatMessage(sender = sender, recipient = recipient, content = request.content)
        )
        val response = saved.toResponse()

        redisTemplate.convertAndSend("chat.messages", objectMapper.writeValueAsString(response))

        return response
    }

    @Transactional(readOnly = true)
    fun getHistory(currentUsername: String, otherUserId: Int, pageable: Pageable): Page<ChatMessageResponse> {
        val currentUser = userRepository.findByUsername(currentUsername)
            .orElseThrow { NoSuchElementException("User not found") }
        val otherUser = userRepository.findById(otherUserId.toLong())
            .orElseThrow { NoSuchElementException("User not found") }
        return chatMessageRepository.findConversation(currentUser, otherUser, pageable)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getConversations(username: String): List<ConversationSummary> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { NoSuchElementException("User not found") }
        val userId = user.id!!

        return chatMessageRepository.findConversationPartners(userId)
            .mapNotNull { partnerId ->
                val partner = userRepository.findById(partnerId.toLong()).orElse(null) ?: return@mapNotNull null
                val lastMessage = chatMessageRepository.findLastMessageBetween(userId, partnerId)
                val unreadCount = chatMessageRepository.countByRecipientIdAndSenderIdAndIsReadFalse(userId, partnerId)
                ConversationSummary(
                    userId = partner.id,
                    username = partner.username,
                    lastMessage = lastMessage?.content,
                    unreadCount = unreadCount,
                    updatedAt = lastMessage?.createdAt
                )
            }
            .sortedByDescending { it.updatedAt }
    }

    @Transactional
    fun markAsRead(currentUsername: String, otherUserId: Int) {
        val currentUser = userRepository.findByUsername(currentUsername)
            .orElseThrow { NoSuchElementException("User not found") }
        chatMessageRepository.markAsRead(otherUserId, currentUser.id!!)
    }

    private fun ChatMessage.toResponse() = ChatMessageResponse(
        id = id,
        senderId = sender.id,
        senderUsername = sender.username,
        recipientId = recipient.id,
        recipientUsername = recipient.username,
        content = content,
        isRead = isRead,
        createdAt = createdAt
    )
}
