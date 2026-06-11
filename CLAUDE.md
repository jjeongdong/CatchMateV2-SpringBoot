# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Build executable JAR
./gradlew bootJar

# Run all tests
./gradlew test

# Local development with Docker Compose
docker-compose up -d
```

## Architecture: Hexagonal + DDD (Single Module)

이 프로젝트는 **단일 Gradle 모듈** 안에서 Bounded Context 별로 패키지를 나눈 **Hexagonal Architecture (Ports & Adapters)** 구조입니다.

### 패키지 구조

```
com.back.catchmate
├── {context}/                  # e.g. board, chat, enroll, user, auth, oauth,
│                               #      notification, inquiry, report, admin,
│                               #      bookmark, club, game, notice
│   ├── domain/
│   │   ├── model/              # Aggregate, Entity, Value Object
│   │   ├── service/            # Domain Service (도메인 규칙 응집)
│   │   ├── event/              # Domain Event
│   │   ├── enums/              # 도메인 enum
│   │   └── dto/                # 도메인 내부에서 쓰는 값 객체 (e.g. SearchCondition)
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/             # ⭐ UseCase 인터페이스 (Input Port)
│   │   │   └── out/            # Repository, 외부 서비스 인터페이스 (Output Port)
│   │   ├── service/            # UseCase 구현체 (XxxApplicationService) + 얇은 도메인 Service
│   │   ├── event/              # Application 레이어 이벤트 리스너 (소비)
│   │   └── dto/                # Command / Response DTO
│   └── adapter/
│       ├── in/
│       │   ├── web/            # REST Controller + Request/Response DTO
│       │   └── websocket/      # STOMP / WebSocket 진입점
│       └── out/
│           ├── persistence/    # JPA Entity + Repository 구현체 (QueryDSL 포함)
│           └── external/       # FCM, S3, OAuth Client 등 외부 시스템 어댑터
├── global/                     # Cross-cutting 인프라
│   ├── config/                 # Spring 설정 (Security, WebSocket, Async, JPA…)
│   ├── authorization/          # AOP 권한 체크 (CheckXxxPermission)
│   ├── redis/                  # Redis Pub/Sub publisher/subscriber 인프라
│   ├── idempotency/            # 멱등성 처리
│   ├── scheduler/              # 스케줄러 (NotificationScheduler 등)
│   └── error/                  # 전역 예외 핸들러
└── common/                     # 모든 컨텍스트가 참조하는 공통 코드
    ├── error/                  # ErrorCode, BaseException, ErrorResponse
    ├── page/                   # 페이지네이션 모델
    └── orchestration/          # 페이지 응답 등 공통 DTO
```

### 의존성 방향

화살표는 “알아도 되는 방향”입니다. **반대 방향으로는 절대 의존하지 않습니다.**

```
                ┌────────────────────────────────────────────┐
                │  adapter/in/web (Controller)               │
                │  adapter/in/websocket                      │
                └──────────────┬─────────────────────────────┘
                               │ depends on
                               ▼
                ┌────────────────────────────────────────────┐
                │  application/port/in (UseCase 인터페이스)   │  ← Controller는 오직 이것만 안다
                └──────────────┬─────────────────────────────┘
                               │ implemented by
                               ▼
                ┌────────────────────────────────────────────┐
                │  application/service (ApplicationService)  │
                │   - UseCase 구현                            │
                │   - 트랜잭션 경계                            │
                │   - 도메인 Service들을 조합                  │
                │   - ApplicationEventPublisher 발행          │
                └──────┬──────────────────────┬──────────────┘
                       │ uses                 │ uses
                       ▼                      ▼
        ┌──────────────────────┐   ┌────────────────────────┐
        │ domain/model         │   │ application/port/out   │
        │ domain/service       │   │ (Repository / external │
        └──────────────────────┘   │  Port)                 │
                                   └───────────┬────────────┘
                                               │ implemented by
                                               ▼
                                   ┌────────────────────────┐
                                   │ adapter/out/persistence│
                                   │ adapter/out/external   │
                                   └────────────────────────┘
```

규칙:

- **Controller → UseCase**: Controller는 항상 `application.port.in.XxxUseCase`에만 의존합니다. `XxxApplicationService` 같은 구현체를 직접 import 하지 않습니다.
- **ApplicationService → Output Port**: 영속성·외부 시스템 호출은 `application.port.out.*` 인터페이스를 통해서만 합니다. 구현체(`XxxRepositoryImpl`, `FcmNotificationSender` 등)는 `adapter.out.*`에 있고, Spring DI 가 알아서 주입합니다.
- **Domain 순수성**: `domain/*` 패키지에는 Spring · JPA · 인프라 의존성을 넣지 않습니다. JPA `@Entity`는 `adapter.out.persistence.entity`에 두고, 도메인 모델로 `toModel()` / `from()` 변환합니다.
- **컨텍스트 간 호출**: 다른 컨텍스트의 기능이 필요하면 그 컨텍스트의 **UseCase** 또는 도메인 Service만 호출합니다. 다른 컨텍스트의 Repository / Adapter를 직접 호출하지 않습니다.

## 핵심 패턴

### UseCase + ApplicationService

```java
// application/port/in/BoardUseCase.java
public interface BoardUseCase {
    BoardCreateResponse createBoard(Long userId, BoardCreateCommand command);
    BoardDetailResponse getBoard(Long userId, Long boardId);
    void deleteBoard(Long userId, Long boardId);
}

// application/service/BoardApplicationService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardApplicationService implements BoardUseCase {
    private final BoardService boardService;        // 같은 컨텍스트의 도메인 Service
    private final UserUseCase userUseCase;          // 다른 컨텍스트는 UseCase 통해
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        User user = userUseCase.getUser(userId);
        Board board = boardService.createBoard(Board.createBoard(user, command));
        publisher.publishEvent(BoardCreatedEvent.of(board));
        return BoardCreateResponse.from(board);
    }
}

// adapter/in/web/controller/BoardController.java
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardUseCase boardUseCase;        // ⭐ 인터페이스에만 의존

    @PostMapping
    public ResponseEntity<BoardCreateResponse> createBoard(
            @AuthUser Long userId,
            @RequestBody @Valid BoardCreateRequest request) {
        return ResponseEntity.ok(boardUseCase.createBoard(userId, request.toCommand()));
    }
}
```

### Output Port (Repository) DIP

```java
// application/port/out/BoardRepository.java
public interface BoardRepository {
    Optional<Board> findById(Long id);
    Board save(Board board);
}

// adapter/out/persistence/repository/BoardRepositoryImpl.java
@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final JpaBoardRepository jpaBoardRepository;
    private final QueryDslBoardRepository queryDslBoardRepository;

    @Override
    public Optional<Board> findById(Long id) {
        return jpaBoardRepository.findById(id).map(BoardEntity::toModel);
    }
}
```

### Transactional Outbox (알림)

FCM 발송 신뢰성을 위한 **이중 단계 이벤트 리스너** 패턴. **단순화하지 마세요.**

1. `@EventListener` (커밋 전, 동기): `Notification` + `NotificationOutbox` 저장
2. `@TransactionalEventListener(AFTER_COMMIT)` (커밋 후, 비동기): 즉시 FCM 발송 시도
3. `NotificationScheduler` (60초마다): `PENDING` / `FAILED` Outbox 재시도

알림 채널 분리:
- `NotificationDispatchPort` → 실시간 STOMP 전송 (Redis Pub/Sub fan-out)
- `OfflineFallbackPort` → 오프라인 사용자에게 FCM 전송 (CompositeNotificationDispatcher)

### AOP 권한 체크

`global/authorization/` 의 어노테이션 + Aspect 가 메서드 진입 전에 권한을 확인합니다. Controller 메서드에 `@CheckBoardPermission` 등을 붙입니다.

### Soft Delete

모든 엔티티는 `deletedAt` 컬럼으로 소프트 삭제합니다. `@SQLRestriction("deleted_at IS NULL")` 으로 자동 필터링됩니다. **물리 삭제 쿼리를 작성하지 마세요.**

### Entity ↔ Domain 모델 변환

- JPA Entity: `adapter/out/persistence/entity/`
- Domain 모델: `domain/model/`
- 변환: `Entity.toModel()`, `Entity.from(domain)`
- 변환 로직은 반드시 **Entity 클래스 내부**에 작성

## Configuration Profiles

| Profile | 파일 | 용도 |
|---|---|---|
| `local` | `application-local.yml` | 로컬 개발용 하드코딩 값 |
| `dev` | `application-dev.yml` | CI/CD 환경변수 주입 |

활성 프로파일은 `src/main/resources/application.yml`.

`dev` 시크릿은 GitHub Secrets → `deploy.yml`에서 `application-dev.yml` / `firebase-adminsdk.json` 생성.

## Technology Stack

- **Java 21**, Spring Boot 3.4.2, Jakarta EE
- **ORM**: Spring Data JPA + QueryDSL 5.0 (Jakarta)
- **DB**: MySQL on AWS RDS (HikariCP)
- **Cache**: Redis (Lettuce / Spring Data Redis)
- **Push**: Firebase Admin SDK 9.3 (FCM)
- **Storage**: AWS SDK v2 (S3)
- **Auth**: JWT (jjwt 0.11.5) + Spring Security
- **WebSocket**: Spring STOMP + Redis Pub/Sub
- **Docs**: springdoc-openapi 2.8.5

## Deployment

EC2 단일 인스턴스에서 Nginx Blue/Green. `.github/workflows/deploy.yml` 이 `main` 푸시 시 Docker 이미지를 빌드/푸시하고 SSH 로 `deploy.sh` 실행.

---

## 코딩 규칙 (반드시 준수)

### 예외 처리
- **`catch (Exception ignored) {}` 절대 금지.** 예상된 null 케이스는 `Optional`로 처리합니다.
- 모든 비즈니스 예외는 `BaseException(ErrorCode.XXX)` 사용.
- 새 에러 케이스는 `ErrorCode` enum에 추가.

### 트랜잭션
- `ApplicationService` 클래스 레벨 기본값: `@Transactional(readOnly = true)`.
- 쓰기 메서드만 `@Transactional`로 오버라이드.
- `Propagation.REQUIRES_NEW`는 **반드시 별도 Bean**에서 호출 (같은 클래스 내 호출 시 프록시 무시).

### 하드코딩 금지
- Magic number, 딜레이, 재시도 횟수 등은 `application.yml` + `@Value`로 주입.

### 도메인 모델 순수성
- `domain/*` 에 Spring / Infrastructure 의존성을 넣지 않습니다.
- `char Y/N` 같은 표현은 boolean 메서드(`isEnrollAlarmEnabled()`)로 래핑.

### 변경하지 말아야 할 패턴
- **얇은 도메인 Service** (`BoardService`, `UserService` 등): Aggregate 단위 영속성 어댑터/도메인 Service 역할. ApplicationService 와 혼동해서 삭제하지 마세요.
- **이중 단계 이벤트 리스너**: Transactional Outbox의 올바른 구현.

---

## 새 기능 추가 가이드

### API 엔드포인트 추가
1. `{ctx}/domain/model` — Aggregate 변경 / 새 모델
2. `common/error/ErrorCode` — 새 에러 케이스
3. `{ctx}/application/port/out` — 새 Output Port (Repository 메서드 추가 등)
4. `{ctx}/adapter/out/persistence` — JPA Entity, Repository 구현체
5. `{ctx}/application/dto` — Command / Response DTO
6. `{ctx}/application/port/in/{Ctx}UseCase` — UseCase 인터페이스에 메서드 추가
7. `{ctx}/application/service/{Ctx}ApplicationService` — 구현 + 트랜잭션
8. `{ctx}/adapter/in/web/controller` — Controller, Request DTO

### 알림 타입 추가
1. `notification/domain/enums/AlarmType` 에 enum 추가
2. `notification/domain/model/NotificationTemplate` 에 템플릿 추가
3. `{ctx}/application/event/Xxx{Notification}Event` 생성
4. `{ctx}/application/event/Xxx{Notification}EventListener` 생성 — **이중 단계 패턴 준수**
5. 해당 컨텍스트 ApplicationService 에서 `applicationEventPublisher.publishEvent(...)`

### 권한 체크 추가
1. `global/authorization/annotation` 에 새 어노테이션
2. `global/authorization/finder` 에 `DomainFinder` 구현체
3. `DataPermissionAspect` 의 `@Before` 절에 어노테이션 추가
4. Controller 메서드에 어노테이션 적용

---

## 주요 파일 위치

| 목적 | 경로 |
|---|---|
| 에러 코드 | `common/error/ErrorCode.java` |
| 전역 예외 핸들러 | `global/error/GlobalExceptionHandler.java` |
| JWT 인증 필터 | `global/config/security/JwtAuthenticationFilter.java` |
| 비동기 설정 | `global/config/infrastructure/AsyncConfig.java` |
| FCM 발신 | `notification/adapter/out/sender/FcmNotificationSender.java` |
| 알림 스케줄러 | `global/scheduler/NotificationScheduler.java` |
| Outbox 상태 관리 | `notification/application/service/NotificationOutboxUpdater.java` |
| 알림 템플릿 | `notification/domain/model/NotificationTemplate.java` |
| Redis Pub/Sub | `global/redis/` |
| WebSocket 설정 | `global/config/WebSocketConfig.java` |
