package com.example.cms.service

import com.example.cms.dto.ChatMessageResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate

class RedisMessageSubscriber(
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper
) : MessageListener {

    private val logger = LoggerFactory.getLogger(RedisMessageSubscriber::class.java)

    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val payload = String(message.body, Charsets.UTF_8)
            val chatMessage = objectMapper.readValue(payload, ChatMessageResponse::class.java)
            messagingTemplate.convertAndSendToUser(
                chatMessage.recipientUsername,
                "/queue/messages",
                chatMessage
            )
        } catch (e: Exception) {
            logger.error("Errore durante l'elaborazione del messaggio Redis: ${e.message}", e)
        }
    }
}
