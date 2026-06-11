# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build entire project
./gradlew build

# Build executable JAR (catchmate-boot module)
./gradlew :catchmate-boot:bootJar

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :catchmate-application:test

# Local development with Docker Compose
docker-compose up -d
```

## Module Architecture

This is an 8-module Gradle project with a layered, domain-driven design. Dependencies flow inward:

```
catchmate-api → catchmate-orchestration → catchmate-application → catchmate-domain
                                       ↘ catchmate-authorization
catchmate-infrastructure → catchmate-domain
catchmate-boot (assembles all modules)
catchmate-common (shared by all)
```

| Module | Role |
|---|---|
| `catchmate-api` | REST controllers, request/response DTOs, OpenAPI annotations |
| `catchmate-orchestration` | Facade/Orchestrator services that coordinate across domains; also contains command objects |
| `catchmate-authorization` | AOP aspects + annotations (`@CheckEnrollHostPermission`, etc.) for permission enforcement |
| `catchmate-application` | Use-case services, domain event listeners |
| `catchmate-domain` | JPA entities, repository interfaces, domain business logic |
| `catchmate-infrastructure` | Repository implementations (QueryDSL), JPA config, Redis, Firebase, S3, async config |
| `catchmate-common` | Enums (AlarmType, MessageType, OutboxStatus), ErrorCode, ErrorResponse, BaseException |
| `catchmate-boot` | Spring Boot entry point (`CatchmateApplication`), resource files, profiles |

## Key Patterns

### Orchestrator Facade
Controllers call Orchestrators (in `catchmate-orchestration`), never application services directly. Orchestrators coordinate multiple services, build response DTOs, and publish domain events.

### AOP Permission Checks
Method-level annotations intercept calls before business logic runs. To add a new permission check, create an annotation in `catchmate-authorization` and a corresponding `@Aspect` class.

### Transactional Outbox for Notifications
Reliable FCM delivery uses two event listener phases:
1. `@EventListener` (default, before commit) — saves `Notification` + `NotificationOutbox` in the same transaction
2. `@TransactionalEventListener(AFTER_COMMIT)` (async) — sends FCM immediately after commit
3. A scheduler retries `PENDING`/`FAILED` outbox records

**이 이중 단계 패턴은 의도된 설계입니다. 하나의 리스너로 합치거나 단순화하지 마세요.**

### Domain Events
`ApplicationEventPublisher` decouples cross-cutting concerns. Events (e.g., `EnrollNotificationEvent`) are published by orchestrators and consumed by listeners in `catchmate-application`.

### Async Processing
`AsyncConfig` configures a `ThreadPoolTaskExecutor` (10 core / 50 max / 100 queue, `CallerRunsPolicy`). Annotate methods with `@Async` to run on this pool.

### Soft Delete
모든 엔티티는 `deletedAt` 컬럼으로 소프트 삭제합니다. `@SQLRestriction("deleted_at IS NULL")`이 자동 필터를 적용합니다. 물리 삭제 쿼리를 작성하지 마세요.

### Entity ↔ Domain Model 분리
- JPA Entity는 `catchmate-infrastructure`의 `/persistence/{domain}/entity/` 에 위치
- Domain 모델은 `catchmate-domain`에 위치
- 변환: `Entity.toModel()` (Entity → Domain), `Entity.from(domain)` (Domain → Entity)
- 변환 로직은 반드시 Entity 클래스 내부에 작성

## Configuration Profiles

| Profile | File | Usage |
|---|---|---|
| `local` | `application-local.yml` | Hardcoded DB/Redis/S3/JWT for local dev |
| `dev` | `application-dev.yml` | Reads from environment variables (used in CI/CD) |

Active profile is set in `catchmate-boot/src/main/resources/application.yml`.

Secrets for `dev` are injected via GitHub Secrets → `deploy.yml` generates `application-dev.yml` and `firebase-adminsdk.json` at deploy time.

## Technology Stack

- **Java 17**, Spring Boot 3.4.2, Jakarta EE
- **ORM:** Spring Data JPA + QueryDSL 5.0 (Jakarta variant) for complex queries
- **DB:** MySQL on AWS RDS (HikariCP, max 50 connections)
- **Cache:** Redis with Lettuce (Spring Data Redis)
- **Push:** Firebase Admin SDK 9.3.0 (FCM)
- **Storage:** AWS SDK v2 (S3)
- **Auth:** JWT (jjwt 0.11.5) + Spring Security
- **WebSocket:** Spring Stomp + Redis Pub/Sub (멀티 인스턴스 지원)
- **Docs:** springdoc-openapi 2.8.5 (Swagger UI)

## Deployment

Blue/Green deployment via Nginx on a single EC2 instance. CI/CD (`deploy.yml`) builds and pushes a Docker image on every push to `main`, then SSHes into EC2 to run `deploy.sh`.

---

## 코딩 규칙 (반드시 준수)

### 예외 처리
- **`catch (Exception ignored) {}` 절대 금지.** 예상된 null 케이스는 `Optional`로 처리합니다.
- 모든 비즈니스 예외는 `BaseException(ErrorCode.XXX)`를 사용합니다.
- 새 에러 케이스가 필요하면 `ErrorCode` enum에 추가합니다.

### 트랜잭션
- Orchestrator 클래스 레벨에 `@Transactional(readOnly = true)` 기본 설정.
- 쓰기 메서드만 `@Transactional`로 오버라이드.
- `Propagation.REQUIRES_NEW`는 **반드시 별도 Bean**에서 호출 (같은 클래스 내 호출 시 프록시가 무시됨).

### 하드코딩 금지
- Magic number, 딜레이 값, 재시도 횟수 등은 `application.yml`에 정의하고 `@Value`로 주입합니다.

### 도메인 모델 순수성
- Domain 모델(`catchmate-domain`)에 Spring/Infrastructure 의존성을 넣지 않습니다.
- 알람 설정 같은 `Character Y/N` 필드는 도메인 모델에서 boolean 메서드(`isEnrollAlarmEnabled()`)로 래핑합니다.
- 서비스/리스너에서 직접 `!= 'Y'` 비교를 작성하지 않습니다.

### 변경하지 말아야 할 패턴
- `BoardOrchestrator`의 여러 의존성: 의도된 크로스 도메인 조율
- 얇은 Service 레이어: Port/Adapter 패턴에서 올바른 역할
- 이중 단계 이벤트 리스너: Transactional Outbox 올바른 구현

---

## 새 기능 추가 가이드

### API 엔드포인트 추가 순서
1. `catchmate-domain` — 도메인 모델, Repository 인터페이스 수정
2. `catchmate-common` — 필요한 Enum, ErrorCode 추가
3. `catchmate-application` — Service 메서드 추가
4. `catchmate-infrastructure` — JPA Entity, QueryDSL 구현체 수정
5. `catchmate-orchestration` — Command/Response DTO, Orchestrator 메서드 추가
6. `catchmate-api` — Controller, Request DTO 추가

### 알림 타입 추가 순서
1. `AlarmType` enum 추가 (`catchmate-common`)
2. `NotificationTemplate` enum에 메시지 템플릿 추가 (`catchmate-domain`)
3. 이벤트 클래스 생성 (`catchmate-application`)
4. 이벤트 리스너 생성 — 이중 단계(@EventListener + @TransactionalEventListener) 패턴 준수
5. Orchestrator에서 `applicationEventPublisher.publishEvent(...)` 호출

### 권한 체크 추가 순서
1. `catchmate-authorization`에 어노테이션 생성
2. `DomainFinder` 구현체 생성
3. `DataPermissionAspect`가 자동으로 처리함
4. Controller 메서드에 어노테이션 적용

---

## 주요 파일 위치

| 목적 | 경로 |
|---|---|
| 에러 코드 | `catchmate-common/.../common/exception/ErrorCode.java` |
| 전역 예외 핸들러 | `catchmate-api/.../global/error/GlobalExceptionHandler.java` |
| JWT 인증 필터 | `catchmate-api/.../global/config/security/JwtAuthenticationFilter.java` |
| 비동기 설정 | `catchmate-infrastructure/.../config/AsyncConfig.java` |
| FCM 발신 | `catchmate-infrastructure/.../notification/sender/FcmNotificationSender.java` |
| 알림 스케줄러 | `catchmate-api/.../global/scheduler/NotificationScheduler.java` |
| Outbox 상태 관리 | `catchmate-application/.../notification/service/NotificationOutboxUpdater.java` |
| 알림 템플릿 | `catchmate-domain/.../notification/model/NotificationTemplate.java` |
| Redis Pub/Sub | `catchmate-infrastructure/.../redis/` |
| WebSocket 설정 | `catchmate-api/.../global/config/WebSocketConfig.java` |
