venga salvato il percorso di# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Content Management System (CMS) backend built with Spring Boot and Kotlin. It provides RESTful APIs for managing content, users, categories, and authentication using JWT tokens.

## Common Development Commands

### Run the application
```bash
# Using Gradle wrapper (recommended)
./gradlew bootRun

# On Windows
gradlew.bat bootRun

# With live reload for development
./gradlew bootRun --continuous
```

### Build the project
```bash
# Build JAR file
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Database setup
- PostgreSQL is required
- Database name: `cms_db` 
- Configuration in `src/main/resources/application.yml`
- Uses JPA with `ddl-auto: validate` (expects existing schema)

## Architecture Overview

### Package Structure
- `config/` - Security configuration (JWT, CORS, Spring Security)
- `controller/` - REST API endpoints
- `dto/` - Data Transfer Objects for API requests/responses
- `model/` - JPA entities (User, Content, Category, Tag, Comment, Media)
- `repository/` - Spring Data JPA repositories
- `service/` - Business logic layer

### Key Entities and Relationships
- **User**: Has role-based access (ADMIN, EDITOR, AUTHOR, SUBSCRIBER)
- **Content**: Belongs to User (author) and Category, has many Tags and Comments
- **Category**: Hierarchical structure for organizing content
- **Tag**: Many-to-many relationship with Content
- **Comment**: Belongs to Content and User

### Security Model
- JWT-based authentication with configurable expiration (default 24 hours)
- Role-based authorization
- Public endpoints: GET requests for content, categories, tags
- Protected endpoints: All POST/PUT/DELETE operations
- CORS configured for frontend integration

### Status Enums
- **ContentStatus**: DRAFT, PUBLISHED, ARCHIVED
- **UserRole**: ADMIN, EDITOR, AUTHOR, SUBSCRIBER

## Configuration Files
- `application.yml` - Main configuration (database, JWT secret, server port)
- `build.gradle.kts` - Dependencies and build configuration
- Spring Boot 3.2.0 with Kotlin 1.9.20, requires JDK 17+

## API Endpoints Structure
- Authentication: `/api/auth/**` (register, login)
- Content: `/api/contents/**` (CRUD operations)
- Categories: `/api/categories/**` (CRUD operations)
- User roles determine access levels to modification endpoints

## Development Notes
- Uses BCrypt for password hashing
- Supports file uploads (max 10MB)
- PostgreSQL dialect with SQL logging enabled in development
- Validation using Spring Boot Validation starter
- Jackson Kotlin module for JSON serialization