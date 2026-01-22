# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MeeTeam Backend - A Spring Boot API server for a developer team matching and project collaboration platform. Features include team matching, project management, real-time WebSocket chat, AI-powered code review (GPT-4o-mini), GitHub integration, and SSE notifications.

## Build & Development Commands

```bash
# Build the project (also runs QueryDSL code generation)
./gradlew clean build

# Run tests
./gradlew test

# Run application locally
./gradlew bootRun

# Generate QueryDSL Q-classes only
./gradlew compileQuerydsl

# Start infrastructure (Redis, RabbitMQ, Prometheus, Grafana)
docker-compose up -d
```

**Local Development Requirements:**
- Java 17
- MySQL 8.0+
- Redis 7.0+
- RabbitMQ with STOMP plugin (for WebSocket)

## Architecture

**Layered Architecture with DDD principles:**
```
web/        → Controllers (presentation layer)
domain/     → Business logic, entities, repositories per domain
global/     → Cross-cutting concerns (config, auth, exceptions, response)
```

**Domain modules:** applicant, category, chat, codereview, file, llm, member, notification, pr, project, projectLike, projectMember, review, skill, webhook

**Key patterns:**
- All entities extend `BaseEntity` (provides `createdAt`, `editedAt` with JPA auditing)
- Standardized responses via `SuccessResponse<T>` with `SuccessCode` enum
- Custom exceptions via `CustomException` with `ErrorCode` enum - add new error codes to `global/response/ErrorCode.java`
- Event-driven notifications using Spring events + `@TransactionalEventListener` + `@Async`
- Optimistic locking with `@Retry` AOP aspect for concurrent updates
- QueryDSL for complex queries (Q-classes generated in `build/generated/querydsl`)

## API Response Format

```java
// Success response
SuccessResponse.onSuccess(data)           // Uses default _OK code
SuccessResponse.of(SuccessCode.XXX, data) // Uses specific code

// Error handling - throw CustomException
throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
```

## Key Configuration

- **Authentication:** JWT (Access: 10h, Refresh: 7d) + OAuth2 (Google, GitHub)
- **WebSocket:** STOMP over RabbitMQ (port 61613), endpoint `/ws`
- **Caching:** EhCache for main page projects, Redis for sessions
- **File storage:** AWS S3
- **AI:** Spring AI with OpenAI GPT-4o-mini

## Chat Room Types

`PROJECT` (team chat), `TOPIC` (subject-based), `PRIVATE` (1:1), `PR_REVIEW` (code review discussions)

## Testing

Uses JUnit 5 with H2 in-memory database. Tests located in `src/test/java/`.

## Code Documentation

Javadoc required for public classes/methods. Use `@Operation` annotation on controller methods for Swagger documentation. API docs available at `/swagger-ui.html`.
