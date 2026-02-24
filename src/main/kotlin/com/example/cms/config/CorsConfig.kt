package com.example.cms.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    @Value("\${cors.allowed-origins}") private val allowedOriginsRaw: String
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val allowedOrigins = allowedOriginsRaw.split(",").map { it.trim() }

        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = allowedOrigins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }
}
