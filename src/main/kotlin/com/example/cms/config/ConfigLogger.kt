package com.example.cms.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class ConfigLogger(
    private val env: Environment,
    @Value("\${active-profile}") private val activeProfile: String,
    @Value("\${spring.datasource.url}") private val dbUrl: String,
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${s3.endpoint}") private val s3Endpoint: String,
    @Value("\${s3.bucket.media}") private val mediaBucket: String,
    @Value("\${s3.bucket.avatar}") private val avatarBucket: String
) {

    private val logger = LoggerFactory.getLogger(ConfigLogger::class.java)

    @PostConstruct
    fun logConfig() {
        logger.info("=== Configurazione Applicazione ===")
        logger.info("Active Profiles: $activeProfile")
        logger.info("DB URL: $dbUrl")
        logger.info("JWT Secret Length: ${jwtSecret.length} characters")
        logger.info("S3 Endpoint: $s3Endpoint")
        logger.info("S3 Media Bucket: $mediaBucket")
        logger.info("S3 Avatar Bucket: $avatarBucket")
        logger.info("===================================")
    }
}
