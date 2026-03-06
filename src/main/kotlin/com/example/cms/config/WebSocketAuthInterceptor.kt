package com.example.cms.config

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component

@Component
class WebSocketAuthInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor != null && StompCommand.CONNECT == accessor.command) {
            val token = extractToken(accessor)
                ?: throw MessageDeliveryException("Missing JWT token")

            if (!jwtTokenProvider.validateToken(token)) {
                throw MessageDeliveryException("Invalid JWT token")
            }

            val username = jwtTokenProvider.getUsernameFromToken(token)
            val userDetails = userDetailsService.loadUserByUsername(username)
            val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            accessor.user = auth
        }

        return message
    }

    private fun extractToken(accessor: StompHeaderAccessor): String? {
        // 1. STOMP Authorization header: "Bearer <token>"
        val authHeader = accessor.getFirstNativeHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }

        // 2. STOMP token header
        val tokenHeader = accessor.getFirstNativeHeader("token")
        if (tokenHeader != null) return tokenHeader

        // 3. URL query param set by TokenHandshakeInterceptor during HTTP handshake
        return accessor.sessionAttributes?.get("token") as? String
    }
}
