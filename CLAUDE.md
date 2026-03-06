# CLAUDE.md

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
- `config/` - Security configuration (JWT, CORS, Spring Security, WebSocket, Redis)
- `controller/` - REST API endpoints + WebSocket message handlers
- `dto/` - Data Transfer Objects for API requests/responses
- `model/` - JPA entities (User, Content, Category, Tag, Comment, Media, ChatMessage)
- `repository/` - Spring Data JPA repositories
- `service/` - Business logic layer (includes ChatService, PresenceService, RedisMessageSubscriber)

### Key Entities and Relationships
- **User**: Has role-based access (ADMIN, EDITOR, AUTHOR, SUBSCRIBER)
- **Content**: Belongs to User (author) and Category, has many Tags and Comments
- **Category**: Hierarchical structure for organizing content
- **Tag**: Many-to-many relationship with Content
- **Comment**: Belongs to Content and User
- **ChatMessage**: One-to-one messaging between users; fields: `id (Long)`, `sender (User)`, `recipient (User)`, `content (TEXT)`, `isRead (Boolean)`, `createdAt`

### Security Model
- JWT-based authentication with configurable expiration (default 24 hours)
- Role-based authorization
- Public endpoints: GET requests for content, categories, tags
- Protected endpoints: All POST/PUT/DELETE operations
- CORS configured for frontend integration
- WebSocket `/ws/**` is `permitAll` — authentication handled by `WebSocketAuthInterceptor` on STOMP CONNECT

### Status Enums
- **ContentStatus**: DRAFT, PUBLISHED, ARCHIVED
- **UserRole**: ADMIN, EDITOR, AUTHOR, SUBSCRIBER

## Configuration Files
- `application.yml` - Main configuration (database, JWT secret, server port, Redis URL)
- `application-prod.yml` - Production overrides (requires `REDIS_URL` env var)
- `build.gradle.kts` - Dependencies and build configuration
- Spring Boot 3.2.0 with Kotlin 1.9.20, requires JDK 17+

## API Endpoints Structure
- Authentication: `/api/auth/**` (register, login)
- Content: `/api/contents/**` (CRUD operations)
- Categories: `/api/categories/**` (CRUD operations)
- User roles determine access levels to modification endpoints
- **Users**: `/api/users/**` (authenticated unless noted)
  - `GET  /api/users` — list all users; returns `List<UserSummaryResponse>` (`id`, `username`, `role`, `avatarUrl` — no `passwordHash`, no `email`)
  - `GET  /api/users/search?q={query}` — search users by username (case-insensitive, excludes the caller); returns `[{id, username, email}]`
  - `GET  /api/users/avatar` — URL avatar dell'utente autenticato
  - `POST /api/users/avatar` — upload avatar (multipart, max 5 MB, formati: jpg/png/gif/webp)
  - `DELETE /api/users/avatar` — elimina avatar dell'utente autenticato
  - `GET  /api/users/avatars/{filename}` — serve il file avatar (pubblico)
- **Chat REST**: `/api/chat/**` (all authenticated)
  - `GET  /api/chat/conversations` — list of active conversations with last message and unread count
  - `GET  /api/chat/history/{userId}?page=0&size=20` — paginated message history with a specific user
  - `POST /api/chat/history/{userId}/read` — mark all messages from `{userId}` as read

## Real-time Messaging (WebSocket + Redis)
### Architecture
```
Client  ──STOMP over WS──▶  /ws?token={JWT}
                             WebSocketAuthInterceptor  (validates JWT on CONNECT)
                             @MessageMapping("/chat.send")  →  ChatService.sendMessage()
                             ChatService  →  saves to PostgreSQL + publishes JSON to Redis "chat.messages"
                             RedisMessageSubscriber  →  SimpMessagingTemplate.convertAndSendToUser()
Client  ◀── /user/queue/messages ──  recipient receives message
```

### WebSocket connection
- **Endpoint**: `ws://{host}/ws?token={JWT}` (native WebSocket)
- **SockJS fallback**: `http://{host}/ws?token={JWT}`
- Token can also be sent as a STOMP header on CONNECT: `Authorization: Bearer {JWT}` or `token: {JWT}`
- After connect, subscribe to `/user/queue/messages` to receive incoming messages

### STOMP destinations
| Direction | Destination | Payload |
|-----------|-------------|---------|
| Client → Server | `/app/chat.send` | `{ "recipientId": Int, "content": String }` |
| Client → Server | `/app/chat.heartbeat` | _(empty)_ |
| Server → Client | `/user/queue/messages` | `ChatMessageResponse` (see DTOs) |

### DTOs
```kotlin
// Inbound (client → server)
SendMessageRequest(recipientId: Int, content: String)

// Outbound (server → client / REST responses)
ChatMessageResponse(
  id: Long?, senderId: Int?, senderUsername: String,
  recipientId: Int?, recipientUsername: String,
  content: String, isRead: Boolean, createdAt: LocalDateTime
)
ConversationSummary(
  userId: Int?, username: String,
  lastMessage: String?, unreadCount: Long, updatedAt: LocalDateTime?
)
```

### Redis
- **Pub/Sub channel**: `chat.messages` — JSON payload is `ChatMessageResponse`
- **Presence keys**: `presence:{userId}` — TTL 30 s, refreshed by `/app/chat.heartbeat`
- Auto-configured via `spring.data.redis.url` (Lettuce client)

### Database
The `chat_messages` table is auto-created in dev (`ddl-auto: update`).
For production (`ddl-auto: validate`) run manually:
```sql
CREATE TABLE chat_messages (
  id          BIGSERIAL PRIMARY KEY,
  sender_id   INTEGER NOT NULL REFERENCES users(id),
  recipient_id INTEGER NOT NULL REFERENCES users(id),
  content     TEXT NOT NULL,
  is_read     BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_sender    ON chat_messages(sender_id);
CREATE INDEX idx_chat_recipient ON chat_messages(recipient_id);
CREATE INDEX idx_chat_created   ON chat_messages(created_at);
```

### Environment variables
| Variable | Description | Example |
|----------|-------------|---------|
| `REDIS_URL` | Redis connection URL (required in prod) | `redis://:<pw>@host:6379` or `rediss://...` for TLS |

Recommended provider: **Upstash** (https://upstash.com) — free Redis, TLS, compatible with Render.

## Development Notes
- Uses BCrypt for password hashing
- Supports file uploads (max 10MB)
- PostgreSQL dialect with SQL logging enabled in development
- Validation using Spring Boot Validation starter
- Jackson Kotlin module for JSON serialization
- WebSocket dependencies: `spring-boot-starter-websocket`, `spring-boot-starter-data-redis`