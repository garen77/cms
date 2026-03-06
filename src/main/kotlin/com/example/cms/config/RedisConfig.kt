package com.example.cms.config

import com.example.cms.service.RedisMessageSubscriber
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.messaging.simp.SimpMessagingTemplate

@Configuration
class RedisConfig {

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        subscriber: RedisMessageSubscriber
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(subscriber, PatternTopic("chat.messages"))
        return container
    }

    @Bean
    fun redisMessageSubscriber(
        messagingTemplate: SimpMessagingTemplate,
        objectMapper: ObjectMapper
    ): RedisMessageSubscriber = RedisMessageSubscriber(messagingTemplate, objectMapper)
}
