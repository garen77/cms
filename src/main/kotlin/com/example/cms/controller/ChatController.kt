package com.example.cms.controller

import com.example.cms.dto.ChatMessageResponse
import com.example.cms.dto.ConversationSummary
import com.example.cms.dto.SendMessageRequest
import com.example.cms.repository.UserRepository
import com.example.cms.service.ChatService
import com.example.cms.service.PresenceService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.security.Principal

@Controller
class ChatController(
    private val chatService: ChatService,
    private val presenceService: PresenceService,
    private val userRepository: UserRepository
) {

    @MessageMapping("/chat.send")
    fun sendMessage(@Payload @Valid request: SendMessageRequest, principal: Principal) {
        chatService.sendMessage(principal.name, request)
    }

    @MessageMapping("/chat.heartbeat")
    fun heartbeat(principal: Principal) {
        userRepository.findByUsername(principal.name).ifPresent { user ->
            presenceService.heartbeat(user.id!!)
        }
    }
}

@RestController
@RequestMapping("/api/chat")
class ChatRestController(
    private val chatService: ChatService
) {

    @GetMapping("/history/{userId}")
    fun getHistory(
        @PathVariable userId: Int,
        pageable: Pageable,
        auth: Authentication
    ): Page<ChatMessageResponse> = chatService.getHistory(auth.name, userId, pageable)

    @GetMapping("/conversations")
    fun getConversations(auth: Authentication): List<ConversationSummary> =
        chatService.getConversations(auth.name)

    @PostMapping("/history/{userId}/read")
    fun markAsRead(@PathVariable userId: Int, auth: Authentication) =
        chatService.markAsRead(auth.name, userId)
}
