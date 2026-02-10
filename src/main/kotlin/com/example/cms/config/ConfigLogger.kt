package com.example.cms.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class ConfigLogger(
    private val env: Environment,
    @Value("\${profile}") private val profile: String,
    @Value("\${spring.datasource.url}") private val dbUrl: String,
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${media.upload.directory}") private val mediaDir: String
) {
    private val logger = LoggerFactory.getLogger(ConfigLogger::class.java)

    @PostConstruct
    fun logConfig() {
        logger.info("========================================")
        logger.info("CONFIGURATION LOADED")
        logger.info("========================================")
        logger.info("Active Profiles: ${profile}")
        logger.info("DB URL: $dbUrl")
        logger.info("JWT Secret Length: ${jwtSecret.length}")
        logger.info("Media Directory: $mediaDir")
        logger.info("========================================")
    }
}
