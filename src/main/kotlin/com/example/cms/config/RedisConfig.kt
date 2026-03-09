package com.example.cms.config

import com.example.cms.service.RedisMessageSubscriber
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.net.URI

@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.url}") private val redisUrl: String
) {

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val uri = URI.create(redisUrl)
        val isTls = redisUrl.startsWith("rediss://")

        val standaloneConfig = RedisStandaloneConfiguration(uri.host, uri.port)
        uri.userInfo?.substringAfter(":")?.takeIf { it.isNotEmpty() }?.let {
            standaloneConfig.setPassword(it)
        }

        val clientConfig = if (isTls) {
            LettuceClientConfiguration.builder()
                .useSsl()
                .disablePeerVerification()
                .build()
        } else {
            LettuceClientConfiguration.defaultConfiguration()
        }

        return LettuceConnectionFactory(standaloneConfig, clientConfig)
    }

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
