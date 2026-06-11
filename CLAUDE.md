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
│   │   └── dto/                # 도메인 내부에서 쓰는 값 객체
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/             # ⭐ UseCase 인터페이스 (Input Port)
│   │   │   └── out/            # Repository + 다른 컨텍스트용 FetchPort (Output Port)
│   │   ├── service/            # XxxService — UseCase 구현 + own Repository 직접 호출
│   │   ├── event/              # Application 레이어 이벤트 리스너 (소비)
│   │   └── dto/                # Command / Response DTO
│   └── adapter/
│       ├── in/
│       │   ├── web/            # REST Controller + Request/Response DTO
│       │   └── websocket/      # STOMP / WebSocket 진입점
│       └── out/
│           ├── persistence/    # JPA Entity + Repository 구현체 (QueryDSL 포함)
│           └── external/       # FCM, S3, OAuth Client + ⭐ 다른 컨텍스트 FetchPort 구현
├── global/                     # Cross-cutting 인프라
│   ├── config/                 # Spring 설정 (Security, WebSocket, Async, JPA…)
│   ├── authorization/          # AOP 권한 체크
│   ├── redis/                  # Redis Pub/Sub
│   ├── idempotency/            # 멱등성 처리
│   ├── scheduler/              # 스케줄러 (NotificationScheduler 등)
│   └── error/                  # 전역 예외 핸들러
└── common/                     # 모든 컨텍스트가 참조하는 공통 코드
    ├── error/                  # ErrorCode, BaseException, ErrorResponse
    └── page/                   # 페이지네이션 모델
```

### 핵심 원칙

#### 1. Controller → UseCase 인터페이스

```java
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardUseCase boardUseCase;        // ⭐ 구현체 모름

    @PostMapping("/boards")
    public ResponseEntity<BoardCreateResponse> create(
            @AuthUser Long userId,
            @RequestBody @Valid BoardCreateRequest req) {
        return ResponseEntity.ok(boardUseCase.createBoard(userId, req.toCommand()));
    }
}
```

#### 2. 한 컨텍스트 = 한 Service (UseCase 구현)

`XxxService implements XxxUseCase`. 도메인의 핵심 비즈니스 로직과 트랜잭션 경계가 여기 있습니다.

**같은 컨텍스트 내부**는 자기 Repository를 **직접** 호출합니다. thin Service 한 겹을 더 두지 않습니다.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService implements BoardUseCase {

    private final BoardRepository boardRepository;          // 자기 Repository — 직접

    // 다른 컨텍스트의 데이터는 board가 정의한 Fetch Port를 통해서만
    private final UserFetchPort userFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;
    // ...

    @Override
    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        User user = userFetchPort.getUser(userId);
        Club cheerClub = clubFetchPort.getClub(command.getCheerClubId());
        Board savedBoard = boardRepository.save(Board.createBoard(...));
        return BoardCreateResponse.of(savedBoard.getId());
    }

    // 자기 도메인 entity getter (다른 컨텍스트 Adapter / AOP가 쓸 수 있음)
    public Board getBoard(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
    }
}
```

#### 3. 다른 컨텍스트 호출은 Fetch Port를 통해 (⭐ 중요)

다른 컨텍스트의 Service나 Repository를 **직접 주입하지 않습니다**. 자기 컨텍스트의 `application/port/out/` 에 필요한 모양의 인터페이스를 정의하고, `adapter/out/external/` 에서 어댑터가 다른 컨텍스트의 Service를 래핑합니다.

```java
// board/application/port/out/UserFetchPort.java
public interface UserFetchPort {
    User getUser(Long userId);
}

// board/adapter/out/external/BoardUserFetchAdapter.java
@Component
@RequiredArgsConstructor
public class BoardUserFetchAdapter implements UserFetchPort {
    private final UserService userService;          // 다른 컨텍스트 의존은 여기 한 곳에만 격리

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }
}
```

이 패턴의 이점:
- BoardService 는 User context의 존재를 모릅니다.
- 나중에 user를 별도 서비스로 분리해도 Adapter 만 HTTP 클라이언트로 바꾸면 끝.
- 테스트 시 Fetch Port 만 Mock 하면 됩니다.

모든 컨텍스트에 이 패턴이 적용되어 있습니다. cross-context 호출이 필요하면 자기 `application/port/out/` 에 `XxxFetchPort` 인터페이스를 정의하고 `adapter/out/external/` 에 어댑터를 만드세요. **절대 다른 컨텍스트의 `Service` 를 직접 주입하지 않습니다.**

#### 4. Output Port DIP (Repository)

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
    private final JpaBoardRepository jpa;
    private final QueryDslBoardRepository qdsl;
    @Override
    public Optional<Board> findById(Long id) {
        return jpa.findById(id).map(BoardEntity::toModel);
    }
}
```

### 컨텍스트 안에 Aggregate가 여러 개일 때 (chat, user 등)

한 컨텍스트에 독립된 Aggregate가 여러 개면 각자의 Service를 둡니다 (e.g., chat에 `ChatRoomService`, `ChatMessageService`, `ChatRoomMemberService`). UseCase 구현체인 `ChatService`가 이들을 조율합니다. 이 내부 Service들은 같은 컨텍스트 안의 협력자입니다 — 따라서 Fetch Port를 거치지 않고 직접 호출해도 됩니다.

## 주요 패턴

### Transactional Outbox (알림)

FCM 발송 신뢰성을 위한 **이중 단계 이벤트 리스너**. **단순화하지 마세요.**

1. `@EventListener` (커밋 전, 동기): `Notification` + `NotificationOutbox` 저장
2. `@TransactionalEventListener(AFTER_COMMIT)` (커밋 후, 비동기): 실시간 STOMP 전송 + FCM 시도
3. `NotificationScheduler` (60초마다): `PENDING` / `FAILED` Outbox 재시도

알림 채널 분리:
- `NotificationDispatchPort.dispatch(Long, Map)` — 실시간 STOMP fan-out (RedisPublisher 구현)
- `OfflineFallbackPort.dispatchIfOffline(Long, NotificationOutbox)` — 오프라인 사용자 FCM

### AOP 권한 체크

`global/authorization/` 의 어노테이션 + Aspect가 Controller 메서드 진입 전 권한 확인.

### Soft Delete

모든 엔티티는 `deletedAt` 컬럼 + `@SQLRestriction("deleted_at IS NULL")`. 물리 삭제 쿼리 금지.

### Entity ↔ Domain 변환

- JPA Entity: `{ctx}/adapter/out/persistence/entity/`
- Domain 모델: `{ctx}/domain/model/`
- 변환 메서드: `Entity.toModel()`, `Entity.from(domain)` — Entity 클래스 내부.

## Configuration Profiles

| Profile | 파일 | 용도 |
|---|---|---|
| `local` | `application-local.yml` | 로컬 개발용 하드코딩 값 |
| `dev` | `application-dev.yml` | CI/CD 환경변수 주입 |

`dev` 시크릿은 GitHub Secrets → `deploy.yml`에서 `src/main/resources/` 에 주입.

## Technology Stack

- **Java 21** (Gradle toolchain 자동 프로비저닝), Spring Boot 3.4.2
- **ORM**: Spring Data JPA + QueryDSL 5.0 (Jakarta)
- **DB**: MySQL on AWS RDS (HikariCP)
- **Cache**: Redis (Lettuce / Spring Data Redis)
- **Push**: Firebase Admin SDK 9.3 (FCM)
- **Storage**: AWS SDK v2 (S3)
- **Auth**: JWT (jjwt 0.11.5) + Spring Security
- **WebSocket**: Spring STOMP + Redis Pub/Sub
- **Docs**: springdoc-openapi 2.8.5

## Deployment

EC2 단일 인스턴스 Nginx Blue/Green. `.github/workflows/deploy.yml` 이 `main` 푸시 시 Docker 이미지 빌드/푸시 후 SSH 로 `deploy.sh` 실행.

---

## 코딩 규칙 (반드시 준수)

### 예외 처리
- **`catch (Exception ignored) {}` 절대 금지.** 예상된 null 케이스는 `Optional`.
- 모든 비즈니스 예외는 `BaseException(ErrorCode.XXX)`.
- 새 에러 케이스는 `ErrorCode` enum 에 추가.

### 트랜잭션
- `XxxService` 클래스 레벨 기본값: `@Transactional(readOnly = true)`.
- 쓰기 메서드만 `@Transactional` 로 오버라이드.
- `Propagation.REQUIRES_NEW` 는 반드시 **별도 Bean** 에서 호출 (같은 클래스 내 호출 시 프록시 무시).

### 하드코딩 금지
- Magic number / 딜레이 / 재시도 횟수는 `application.yml` + `@Value` 주입.

### 도메인 모델 순수성
- `domain/*` 에 Spring / Infrastructure 의존성 금지.
- `char Y/N` 같은 표현은 boolean 메서드(`isEnrollAlarmEnabled()`)로 노출.

### 절대 변경 금지
- **이중 단계 이벤트 리스너** (Transactional Outbox): 단순화 X.
- **RedisPublisher 분리**: `RedisPublisher`(NotificationDispatchPort 구현) 와 `ChatMessageRedisPublisher`(@TransactionalEventListener) 는 의도적으로 분리됨. 합치면 Spring proxy 충돌.

---

## 새 기능 추가 가이드

### API 엔드포인트 추가
1. `{ctx}/domain/model/` — 도메인 변경
2. `common/error/ErrorCode` — 필요한 에러
3. `{ctx}/application/port/out/` — Repository 메서드 추가; 다른 컨텍스트 호출이 필요하면 `XxxFetchPort` 정의
4. `{ctx}/adapter/out/persistence/` — JPA Entity, Repository 구현
5. `{ctx}/adapter/out/external/` — Fetch Port 구현 (다른 컨텍스트 Service 래핑)
6. `{ctx}/application/dto/` — Command / Response DTO
7. `{ctx}/application/port/in/{Ctx}UseCase` — UseCase 인터페이스 메서드 추가
8. `{ctx}/application/service/{Ctx}Service` — 구현 + 트랜잭션
9. `{ctx}/adapter/in/web/controller/` — Controller, Request DTO

### 알림 타입 추가
1. `notification/domain/enums/AlarmType` 에 enum
2. `notification/domain/model/NotificationTemplate` 에 템플릿
3. `{ctx}/application/event/Xxx{Notification}Event` 생성
4. `{ctx}/application/event/Xxx{Notification}EventListener` — 이중 단계 패턴 준수
5. 해당 컨텍스트 Service 에서 `applicationEventPublisher.publishEvent(...)`

### 권한 체크 추가
1. `global/authorization/annotation` 에 새 어노테이션
2. `global/authorization/finder` 에 `DomainFinder` 구현체
3. `DataPermissionAspect` 의 `@Before` 절에 어노테이션 추가
4. Controller 메서드에 적용

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
| Fetch Port 예시 | `board/application/port/out/*FetchPort.java`, `board/adapter/out/external/Board*FetchAdapter.java` |
