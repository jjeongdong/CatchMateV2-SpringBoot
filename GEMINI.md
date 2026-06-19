# Catchmate Spring Boot Project

## Project Overview
Catchmate is a matching service for baseball game attendees. The backend is built with Spring Boot and follows a strict **Hexagonal Architecture (Ports & Adapters)** and **Domain-Driven Design (DDD)** principles.

### Core Technologies
- **Language:** Java 21
- **Framework:** Spring Boot 3.4.2
- **Persistence:** MySQL, Spring Data JPA, QueryDSL 5.0.0
- **Caching & Real-time:** Redis (Pub/Sub, Write-Behind Buffer, Lua Scripts)
- **Communication:** WebSocket + STOMP
- **Notifications:** Firebase Cloud Messaging (FCM)
- **Cloud Storage:** AWS S3
- **Security:** Spring Security + JWT
- **Documentation:** OpenAPI (SpringDoc/Swagger)

### Architecture
The project is structured into Bounded Contexts (e.g., `board`, `chat`, `enroll`, `notification`). Each context follows this internal structure:
- `domain`: Aggregate, Entity, Value Objects, Domain Services.
- `application`: UseCase interfaces (Input Ports), Port interfaces (Output Ports), and Service implementations.
- `adapter`: Web controllers (In), WebSocket handlers (In), Persistence repositories (Out), and External API clients (Out).

## Building and Running
- **Build:** `./gradlew build`
- **Create Executable JAR:** `./gradlew bootJar`
- **Run Tests:** `./gradlew test`
- **Local Infrastructure:** `docker-compose up -d` (Starts MySQL, Redis, etc.)

## Development Conventions

### 1. Hexagonal Architecture Rules
- **Strict Layering:** Dependencies must flow inwards. Domain should not depend on Application; Application should not depend on Adapter.
- **Port Interfaces:** All communication between layers and contexts must happen through Ports (Input/Output).
- **ISP for UseCases:** Split UseCases into `ClientCommandUseCase`, `InternalCommandUseCase`, `ClientQueryUseCase`, and `InternalQueryUseCase` to satisfy the Interface Segregation Principle.

### 2. Implementation Patterns
- **Transactional Outbox:** Used for notification reliability. Events are saved to the `notification_outbox` table within the same transaction as the business logic.
- **Async Event Handling:** Use `@TransactionalEventListener(phase = AFTER_COMMIT)` combined with `@Async` for non-blocking external API calls (e.g., FCM).
- **Redis Write-Behind:** For high-frequency updates like chat "read receipts," data is buffered in Redis and flushed to the DB periodically using Lua scripts.
- **QueryDSL No-Offset:** Use cursor-based pagination for large datasets (e.g., board list) to avoid performance degradation.

### 3. Coding Style
- Follow standard Java naming conventions and Spring Boot best practices.
- Use Lombok to reduce boilerplate.
- Use `ErrorCode` and `BaseException` for consistent error handling.
- Ensure all business logic remains in the `domain` or `application` layer, never in the `adapter`.

### 4. Testing
- Implement unit tests for domain logic and integration tests for adapters.
- Use `OutboxRetryReliabilityTest` patterns for testing reliability mechanisms.
- Leverage `AsyncEventPerformanceTest` for validating performance improvements.
