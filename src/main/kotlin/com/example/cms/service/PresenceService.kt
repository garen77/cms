package com.example.cms.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PresenceService(
    private val redisTemplate: StringRedisTemplate
) {

    fun setOnline(userId: Int) {
        redisTemplate.opsForValue().set("presence:$userId", "1", 30, TimeUnit.SECONDS)
    }

    fun isOnline(userId: Int): Boolean =
        redisTemplate.hasKey("presence:$userId") == true

    fun heartbeat(userId: Int) {
        redisTemplate.expire("presence:$userId", 30, TimeUnit.SECONDS)
    }
}
