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
│   │   ├── service/            # XxxCommandService + XxxQueryService(3개 정문 implements) + XxxReader(내부 조회 협력자)
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

#### 1. Controller → UseCase 인터페이스 (Command / Query 분리)

```java
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardCommandUseCase boardCommandUseCase;   // ⭐ 인터페이스
    private final BoardQueryUseCase   boardQueryUseCase;     // ⭐ 인터페이스

    @PostMapping("/boards")
    public ResponseEntity<BoardCreateResponse> create(
            @AuthUser Long userId,
            @RequestBody @Valid BoardCreateRequest req) {
        return ResponseEntity.ok(boardCommandUseCase.createBoard(userId, req.toCommand()));
    }

    @GetMapping("/boards/{boardId}")
    public ResponseEntity<BoardDetailResponse> get(
            @AuthUser Long userId, @PathVariable Long boardId) {
        return ResponseEntity.ok(boardQueryUseCase.getBoard(userId, boardId));
    }
}
```

#### 2. UseCase = Command / Query + 호출자별 분리 (ISP, ⭐ 강제)

한 컨텍스트의 Input Port(UseCase) 는 **반드시 Command 와 Query 로 분리**하고, 이를 다시 **호출자 목적별로 ISP 분리**합니다.

```
{ctx}/application/port/in/
├── {Ctx}ClientCommandUseCase       # 웹/앱 컨트롤러의 상태 변경 요청
├── {Ctx}InternalCommandUseCase     # 다른 컨텍스트에서 오는 상태 변경 요청
│
├── {Ctx}ClientQueryUseCase         # 웹/앱 컨트롤러 진입 — 화면 전용 Response DTO 반환
├── {Ctx}InternalQueryUseCase       # 다른 컨텍스트 FetchAdapter / 인프라 진입 — 도메인 모델 반환
└── {Ctx}AdminQueryUseCase          # 관리자 / 대시보드 / 통계 진입 — 페이징 / 집계 / Map / count
```

##### 왜 이렇게 많이 갈라지나? (ISP)

같은 "조회"나 "변경"이라도 호출자 목적이 다르면 반환 타입, 요구되는 보안 수준, 결합도가 다릅니다.
- **Client**: 웹/앱 전용 DTO를 사용하며 권한 체크가 엄격함.
- **Internal**: 다른 컨텍스트와 협력하며 순수 도메인 모델이나 내부 record를 사용.
- **Admin**: 통계, 대량 조회, 관리 기능에 집중.

하나의 인터페이스에 모든 호출자가 섞여 있으면, 컨트롤러가 자기와 무관한 내부용 메서드를 강제로 의존하게 되어 ISP를 위반합니다. 호출자별로 갈라 두면 Mocking이 깔끔해지고 변경 영향도가 격리됩니다.

##### Service 구현 — 호출자 그룹별로 Service 분리 (⭐)

클래스 폭증을 막기 위해 모든 것을 하나로 합치지 않고, **호출자 그룹(Client vs Internal/Admin) 및 행위(Command vs Query)에 따라 서비스를 분리**하여 구현합니다.

- **Client**: 웹/앱 컨트롤러용 서비스 (`ClientCommandService`, `ClientQueryService`)
- **Internal/Admin**: 다른 컨텍스트 및 관리자용 서비스 (`InternalCommandService`, `InternalQueryService`)
  - `AdminQueryUseCase`는 성격이 유사한 `InternalQueryService`에서 함께 구현합니다.

```
{ctx}/application/service/
├── {Ctx}ClientCommandService   implements {Ctx}ClientCommandUseCase
├── {Ctx}ClientQueryService     implements {Ctx}ClientQueryUseCase
├── {Ctx}InternalCommandService implements {Ctx}InternalCommandUseCase
└── {Ctx}InternalQueryService   implements {Ctx}InternalQueryUseCase,
                                            {Ctx}AdminQueryUseCase
```

##### Service 내부의 조회 협력자 — `{Ctx}Reader`

Query 메서드 수가 많아질수록 Service 안에서 같은 Repository 호출 + 같은 "없으면 throw" 패턴이 반복됩니다. 이걸 **`{Ctx}Reader` 라는 내부 컴포넌트로 분리**합니다.

```
{ctx}/application/service/
└── {Ctx}Reader     # @Component — Repository 단순 위임 + 단건 조회 시 예외 throw
```

- Reader 는 **Repository 만 의존**하고 다른 어떤 것도 모릅니다.
- Reader 가 단건 조회의 `orElseThrow` 책임을 가져갑니다 (`getUser(id)` — 없으면 `BaseException`).
- `XxxQueryService` 는 Repository 를 **직접 의존하지 않고** Reader 만 의존 → Service 는 DTO 변환과 cross-context 조합에만 집중.

**⚠️ 주의 — 두 종류의 "Reader" 를 구분**:
- ✅ **내부 협력자 Reader**: `{Ctx}Service` 가 **같은 컨텍스트 안에서만** 주입 — 정문 아님, OK.
- ❌ **외부 정문 Reader (anti-pattern)**: 다른 컨텍스트의 FetchAdapter 가 `XxxReader` 를 직접 주입 — UseCase 를 우회한 **두 번째 정문**. 절대 금지. ([#3](#3-cross-domain-호출--strict-5-hop-chain--절대-우회-금지) 의 금지 사항)

##### 예시 — Service 한 개가 3개 implements + Reader 위임

```java
// application/port/in/UserClientQueryUseCase.java
public interface UserClientQueryUseCase {
    UserResponse getUserProfile(Long userId);
    UserNicknameCheckResponse getUserNicknameAvailability(String nickName);
}

// application/port/in/UserInternalQueryUseCase.java
public interface UserInternalQueryUseCase {
    User getUser(Long userId);
    List<User> getUsers(List<Long> userIds);
    Optional<User> findByProviderId(String providerId);
}

// application/port/in/UserAdminQueryUseCase.java
public interface UserAdminQueryUseCase {
    Page<User> getUsersByClub(String clubName, Pageable pageable);
    Map<String, Long> getUserCountByClub();
    long getTotalUserCount();
}

// application/service/UserReader.java  ← Service 내부 협력자
@Component
@RequiredArgsConstructor
public class UserReader {
    private final UserRepository userRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }
    public List<User> getUsers(List<Long> userIds) { return userRepository.findAllByIds(userIds); }
    public Map<String, Long> countByClub() { return userRepository.countUsersByClub(); }
    // ... 나머지 read 메서드 위임
}

// application/service/UserInternalQueryService.java  ← Internal/Admin 그룹 구현
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserInternalQueryService implements
        UserInternalQueryUseCase,
        UserAdminQueryUseCase {

    private final UserReader userReader;        // ⭐ Repository 직접 의존 X — Reader 만
    private final ClubFetchPort clubFetchPort;  // cross-context 는 Port 로

    // ── InternalQueryUseCase ──
    @Override public User getUser(Long userId) { return userReader.getUser(userId); }
    @Override public List<User> getUsers(List<Long> ids) { return userReader.getUsers(ids); }

    // ── AdminQueryUseCase ──
    @Override public Map<String, Long> getUserCountByClub() { return userReader.countByClub(); }
    // ...
}
```

##### Command 도 호출자별로 분리

Command 역시 호출자 그룹별로 서비스를 분리합니다 (`ClientCommandService`, `InternalCommandService`). 웹 컨트롤러는 `ClientCommandUseCase`를 구현한 `ClientCommandService`만 의존하고, 다른 컨텍스트의 어댑터는 `InternalCommandUseCase`를 구현한 `InternalCommandService`만 의존함으로써 변경의 전파를 막습니다.

**금지**:
- 하나의 `XxxService` 가 `Client + Internal` 유스케이스를 **동시 구현** (그룹 분리 깨짐)
- 하나의 `XxxService` 가 `Command + Query` 유스케이스를 **동시 구현** (CQRS 분리 깨짐)
- 하나의 Query/Command 인터페이스에 Client/Internal/Admin 메서드 **섞기** (ISP 위반)
- Query/Command 인터페이스가 **호출자 0명** (정문이 아님 → Service private 메서드 / Reader 로 흡수)

```java
@Service
@RequiredArgsConstructor
@Transactional
public class BoardClientCommandService implements BoardClientCommandUseCase {

    private final BoardRepository boardRepository;          // own — write 는 Repository 직접 OK
    private final BoardReader boardReader;                  // own — read 는 Reader 협력자

    // 다른 컨텍스트의 데이터/동작은 board 가 정의한 Fetch Port 를 통해서만
    private final UserFetchPort userFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;

    @Override
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        User user = userFetchPort.getUser(userId);          // 다른 컨텍스트 → Fetch Port
        Club cheerClub = clubFetchPort.getClub(command.getCheerClubId());
        Board savedBoard = boardRepository.save(Board.createBoard(...));
        return BoardCreateResponse.of(savedBoard.getId());
    }
}
```

#### 3. Cross-Domain 호출 = Strict 5-Hop Chain (⭐⭐ 절대 우회 금지)

다른 컨텍스트의 Service / Reader / Repository / Entity 를 **직접 주입하지 않습니다**. 다른 컨텍스트가 필요하면 **반드시** 다음 체인을 거칩니다:

```
[A 컨텍스트 Service]
   └─→ [A 컨텍스트 Out Port: XxxFetchPort 인터페이스]
        ↑ (implements)
       [A 컨텍스트 Out Adapter: AXxxFetchAdapter]
        └─→ [B 컨텍스트 In Port: XxxQueryUseCase / XxxCommandUseCase 인터페이스]   ◀ 다른 컨텍스트 진입은 반드시 UseCase 인터페이스
              ↑ (implements)
             [B 컨텍스트 Service: XxxQueryService / XxxCommandService]
```

**핵심**: A 의 FetchAdapter 는 B 의 `XxxService` 구체 클래스 / `XxxReader` 가 아니라 **호출자 목적에 맞는 UseCase 인터페이스**를 주입해야 합니다.

| A 컨텍스트의 정체 | 주입할 B 의 UseCase 인터페이스 |
|:---|:---|
| 일반 도메인 (board, enroll, chat 등) | `BInternalQueryUseCase` (도메인 모델 반환) |
| admin 컨텍스트 | `BAdminQueryUseCase` (집계/통계) |
| 변경 동작 | `BCommandUseCase` |

UseCase 인터페이스가 컨텍스트의 **유일한 정문**이기 때문 ([#5](#5-usecase--유일한-정문)).

```java
// board/application/port/out/UserFetchPort.java
public interface UserFetchPort {
    User getUser(Long userId);
    List<User> getUsers(List<Long> userIds);
}

// board/adapter/out/external/BoardUserFetchAdapter.java
@Component
@RequiredArgsConstructor
public class BoardUserFetchAdapter implements UserFetchPort {

    // ⭐ Internal 정문 (도메인 모델 반환) — Service 구체 클래스 / Reader 직주입 X
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public User getUser(Long userId) {
        return userInternalQueryUseCase.getUser(userId);
    }

    @Override
    public List<User> getUsers(List<Long> userIds) {
        return userInternalQueryUseCase.getUsers(userIds);
    }
}
```

쓰기 동작이 필요하면 `XxxCommandUseCase` 를 같은 방식으로 주입합니다. 한 어댑터가 Query 와 Command 를 모두 필요로 하면 둘 다 주입해도 OK (각각 인터페이스).

**금지 사항 (build 실패가 아니라 PR 리뷰에서 reject)**:
```java
// ❌ 다른 컨텍스트의 Service 구체 클래스 직접 주입
private final UserQueryService userQueryService;

// ❌ 다른 컨텍스트의 Reader 를 두 번째 정문으로 직접 주입
//    Reader 는 같은 컨텍스트 Service 의 내부 협력자 — 외부 진입은 항상 UseCase 인터페이스
private final UserReader userReader;

// ❌ 다른 컨텍스트의 Repository 직접 주입
private final UserRepository userRepository;

// ❌ 자기 Service 에서 직접 Fetch Adapter 주입 (Port 를 건너뜀)
private final BoardUserFetchAdapter boardUserFetchAdapter;

// ❌ Application Service ↔ Application Service (cross-context)
//    BoardCommandService 가 UserCommandService 를 import 하면 잘못된 신호

// ❌ 정문 선택 실패 — 일반 컨텍스트가 Admin Query 진입
//    BoardUserFetchAdapter 가 UserAdminQueryUseCase 를 주입 (관리자 통계는 board 와 무관)
private final UserAdminQueryUseCase userAdminQueryUseCase;
```

이 패턴의 이점:
- BoardCommandService 는 User 컨텍스트의 구현 존재를 모릅니다.
- 나중에 user 를 별도 서비스로 분리해도 Adapter 만 HTTP 클라이언트로 바꾸면 끝.
- 테스트 시 Fetch Port 만 Mock. UseCase 의 시그니처가 안정적이어서 Mock 도 깨지지 않음.

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

#### 5. UseCase = 유일한 정문

`{Ctx}CommandUseCase` / `{Ctx}ClientQueryUseCase` / `{Ctx}InternalQueryUseCase` / `{Ctx}AdminQueryUseCase` 는 컨텍스트 외부가 들어오는 **유일한 정문(Single Entry Gate)** 입니다. 정문은 호출자 목적에 따라 갈라집니다:

| 호출자 | 위치 | 진입 정문 |
|:---|:---|:---|
| Web/App Controller | `{ctx}/adapter/in/web/controller/` | `{Ctx}ClientQueryUseCase` / `{Ctx}CommandUseCase` |
| WebSocket Listener | `{ctx}/adapter/in/websocket/` | 일반적으로 `{Ctx}InternalQueryUseCase` (도메인 모델 사용) |
| **다른 컨텍스트의 FetchAdapter (일반 도메인)** | `{other-ctx}/adapter/out/external/` | `{Ctx}InternalQueryUseCase` / `{Ctx}CommandUseCase` |
| **admin 컨텍스트의 FetchAdapter** | `admin/adapter/out/external/` | `{Ctx}AdminQueryUseCase` (+ 필요 시 `{Ctx}CommandUseCase`) |
| Scheduler / AOP Finder | `global/...` | 보통 `{Ctx}InternalQueryUseCase` |

**규칙**: 정문 외 다른 진입점을 만들지 않습니다. `XxxService` 구체 클래스 / `XxxRepository` / `XxxReader` / JPA `XxxEntity` 를 외부에서 import 하면 정문을 우회한 것입니다 — PR 리뷰에서 reject.

```java
// ✅ Web Controller → 화면 DTO 정문
@RestController
public class UserController {
    private final UserCommandUseCase userCommandUseCase;
    private final UserClientQueryUseCase userClientQueryUseCase;
}

// ✅ 일반 도메인 FetchAdapter → 도메인 모델 정문
@Component
public class BoardUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;
}

// ✅ admin FetchAdapter → 통계/집계 정문 (+ 필요 시 Command)
@Component
public class AdminUserFetchAdapter implements UserFetchPort {
    private final UserAdminQueryUseCase userAdminQueryUseCase;
    private final UserCommandUseCase userCommandUseCase;
}

// ❌ Service 구체 클래스 직주입
private final UserQueryService userQueryService;

// ❌ Reader 를 외부에서 두 번째 정문으로 직주입
private final UserReader userReader;

// ❌ 정문 종류 오선택 — board 컨텍스트가 admin 통계 정문으로 진입
//    (BoardUserFetchAdapter 는 UserInternalQueryUseCase 가 정문)
private final UserAdminQueryUseCase userAdminQueryUseCase;
```

#### 6. Cross-Context DTO 격리 (안티-커럽션 레이어, ⭐⭐ Strict)

UseCase 인터페이스로 호출을 격리하는 것만으로는 부족합니다. **다른 컨텍스트의 도메인 모델(`User`, `Board`, `AuthToken` 등) 자체를 Service / Port 시그니처에서 import 하면 결합도가 그대로 새 나갑니다.** 외부 도메인은 어댑터(안티-커럽션 레이어) 한 곳에서만 만지고, 나머지 레이어는 자기 컨텍스트의 자체 record DTO 만 압니다.

##### 어디에 외부 도메인 import 가 허용되나 (⭐ 0-import 정책)

타겟 상태: **어댑터/이벤트/UseCase 시그니처 모두 외부 도메인 import 0건**. 어댑터는 소유 모듈이 노출한 record 만 받아서 자기 record 로 매핑합니다.

| 위치 | 외부 도메인 모델 import | 외부 enum import |
|:---|:---:|:---:|
| `{ctx}/domain/*` | ❌ | ❌ |
| `{ctx}/application/service/*` | ❌ | ❌ |
| `{ctx}/application/port/in/*` (UseCase 시그니처) | ❌ | ❌ |
| `{ctx}/application/port/out/*` (Port 시그니처) | ❌ | ❌ |
| `{ctx}/application/dto/*` (자체 DTO) | ❌ | ⚠️ 가급적 String |
| `{ctx}/application/event/*` (이벤트) | ❌ | ❌ |
| `{ctx}/adapter/out/external/*` (FetchAdapter) | ❌ (record 만 import) | ❌ |
| `{ctx}/adapter/in/event/*` (Listener) | ❌ (record 만 import) | ❌ |

- 외부 데이터/응답은 **자기 컨텍스트의 record DTO** 로 받습니다 (`RegisteredUserSummary`, `CreatedUserSummary`, `IssuedTokenPair`, `NoticeSnapshot`, `ReportSnapshot`, `InquirySnapshot` 등).
- `Authority`, `Provider`, `NotificationTemplate` 같은 외부 enum 도 다른 컨텍스트로 새지 않도록 **String 으로 전달**.
- **어댑터는 외부 모듈의 record (`ReportInternalResponse` 등) 만 import 하고 도메인 (`Report` 등) 은 import 하지 않습니다.**

##### Port 시그니처는 자체 record 로

```java
// ... (생략)
```

##### DTO 매핑 위치

DTO record 의 `from(Domain)` 정적 팩토리에서 자기 컨텍스트 도메인 모델을 import 해 매핑하는 것은 허용합니다 (Service 의 private 매퍼로 강제 분리하지 않음). **다른 컨텍스트의 도메인** import 만 금지.

```java
// report/application/service/ReportInternalQueryService.java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportInternalQueryService implements ReportInternalQueryUseCase, ReportAdminQueryUseCase {
    private final ReportReader reportReader;

    @Override
    public ReportInternalResponse getReport(Long reportId) {
        Report report = reportReader.getReport(reportId);
        return toInternalResponse(report); // ⭐ 서비스 내부 private 매퍼 사용
    }

    private ReportInternalResponse toInternalResponse(Report report) {
        return new ReportInternalResponse(
            report.getId(),
            report.getReporterId(),
            report.getReportedUserId(),
            report.getReason().name(),
            report.getDescription(),
            report.getCreatedAt(),
            report.isCompleted()
        );
    }
}
```

##### Internal Command UseCase 도 DTO 입출력

다른 컨텍스트가 외부 모듈의 도메인을 짓는 책임을 가져가면 안 됩니다. **소유 모듈의 정적 팩토리(`User.createUser(...)` 등) 호출은 소유 모듈 안에 봉인**합니다.

```java
// ✅ Good — DTO 입력, DTO 출력
public interface UserInternalCommandUseCase {
    CreatedUserResponse createUser(CreateUserCommand command);   // 자체 record 입출력
    void markUserAsReported(Long userId);
    void clearFcmToken(Long userId);
}

@Service
@Transactional
public class UserInternalCommandService implements UserInternalCommandUseCase {
    @Override
    public CreatedUserResponse createUser(CreateUserCommand command) {
        User user = User.createUser(...);                 // ⭐ 정적 팩토리는 user 모듈 안에서만
        User saved = userRepository.save(user);
        return toCreatedUserResponse(saved);              // ⭐ 서비스 내부 private 매퍼 사용
    }

    private CreatedUserResponse toCreatedUserResponse(User user) {
        return new CreatedUserResponse(user.getId(), user.getAuthority().name(), user.getCreatedAt());
    }
}
```

소유 모듈은 노출용 record (`{owner}/application/dto/response/{Verb}Response`)를 제공하며, 매핑은 해당 모듈의 Service에서 수행합니다. 호출자(다른 컨텍스트 어댑터)는 자기 record 로 한 번 더 매핑할 수 있습니다. (2단계 매핑)

##### 정문별로 record 도 분리 (ISP 연장, ⭐⭐)

#2 에서 Query UseCase 를 호출자 목적별로 (`Client / Internal / Admin`) 분리했습니다. **같은 원칙을 반환 record 에도 적용** — 정문 1개 = record 1개. 필드가 지금은 똑같아도 계약이 다른 별도 타입으로 둡니다.

| 정문 | 반환 record (예) | 누가 받나 |
|:---|:---|:---|
| `{Ctx}ClientQueryUseCase` | `{Ctx}Response`, `{Ctx}DetailResponse` (화면용) | Controller |
| `{Ctx}InternalQueryUseCase` | `{Ctx}InternalResponse` | 다른 컨텍스트 FetchAdapter / Scheduler / AOP |
| `{Ctx}AdminQueryUseCase` | `{Ctx}View` (또는 `{Ctx}AdminView`) | admin FetchAdapter |

**왜 정문별로 record 를 분리하나?**
- 화면(Client) 은 표시용 가공 필드가 필요해질 수 있음 (`displayedAt`, `summary`).
- Internal 은 알림/이벤트 메타가 추가될 수 있음 (`recipientFcmToken`, `notifyAt`).
- Admin 은 집계/감사 필드가 붙을 수 있음 (`processedBy`, `flagCount`).
- 같은 record 를 셋이 공유하면 한쪽 변화가 모든 호출자를 깨뜨림 (ISP 위반).

**예시 — Report 컨텍스트**

```java
// report/application/service/ReportInternalQueryService.java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportInternalQueryService implements ReportInternalQueryUseCase, ReportAdminQueryUseCase {
    private final ReportReader reportReader;

    @Override
    public ReportInternalResponse getReport(Long reportId) {
        Report report = reportReader.getReport(reportId);
        return toInternalResponse(report); // ⭐ 정문별 전용 매퍼
    }

    @Override
    public Page<ReportView> getReportList(Pageable pageable) {
        return reportReader.getReportList(pageable).map(this::toReportView); // ⭐ 정문별 전용 매퍼
    }

    private ReportInternalResponse toInternalResponse(Report report) { ... }
    private ReportView toReportView(Report report) { ... }
}
```

**금지**:
- 한 record (`ReportView` 등) 를 Client / Internal / Admin 정문이 **공용** 으로 반환 — ISP 위반, 한쪽 필드 추가가 다른 호출자에게 새 나감.
- "지금 필드 같으니까 하나로 합치자" — 합치는 순간 정문 분리의 의미가 사라짐. 필드가 같아도 **타입은 다르게**.

### 컨텍스트 안에 Aggregate가 여러 개일 때 (chat, user 등)

한 컨텍스트에 독립된 Aggregate 가 여러 개면 각자의 Command/Query UseCase + Service 쌍을 둡니다 (e.g., user 컨텍스트의 `UserCommandUseCase`, `UserQueryUseCase`, `BlockCommandUseCase`, `BlockQueryUseCase`). 이 내부 Service 들은 같은 컨텍스트 안의 협력자이므로 Fetch Port 를 거치지 않고 서로 직접 호출 가능합니다 — 단 호출 대상은 여전히 **UseCase 인터페이스**여야 합니다 (Service 구체 클래스 직주입 금지).

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

---

## 이벤트 패키지 & 의존성 규칙 (⭐⭐)

이벤트는 컨텍스트 간 **유일하게 허용되는 비동기 정문**입니다. UseCase 인터페이스가 동기 호출의 published contract 라면, 이벤트 클래스는 비동기 알림의 published contract 입니다. import 가 허용되는 대신 엄격한 제약이 붙습니다.

### 1. 이벤트 3종류 명확히 구분

| 종류 | 목적 | 위치 |
|:---|:---|:---|
| **Domain Event** | "도메인에서 X 가 일어났다"는 사실 자체 (Aggregate 가 raise) | `{ctx}/domain/event/` |
| **Application Event** | 한 컨텍스트 내부 비동기 후처리 트리거 | `{ctx}/application/event/` |
| **Integration Event** | 컨텍스트 경계를 넘는 알림 (Cross-context) | `{ctx}/application/event/` (publisher 소유) |

세 종류를 한 클래스로 합치지 않습니다. 도메인 이벤트는 순수 POJO, 통합 이벤트는 외부 계약입니다.

### 2. 소유권은 publisher, 구독은 subscriber 에

> **이벤트 클래스 = 발생시키는 컨텍스트가 소유.**
> **리스너 = 반응하는 쪽 컨텍스트에 위치.**

```
publisher 컨텍스트:   {publisher-ctx}/application/event/{Fact}Event.java
subscriber 컨텍스트:  {subscriber-ctx}/adapter/in/event/{Subscriber}{Fact}EventListener.java
```

**의존 방향은 한 방향만**:
```
✅ subscriber → publisher 의 Event 클래스
❌ publisher → subscriber 의 무엇이든 (publisher 는 누가 듣는지 영원히 모름)
```

리스너 위치를 잘못 잡으면 publisher 패키지가 새 구독자가 생길 때마다 부풀고 결국 양방향 결합이 됩니다.

### 3. 허용되는 cross-context import 는 딱 2가지

[#6 Cross-Context DTO 격리](#6-cross-context-dto-격리-안티-커럽션-레이어--strict) 의 "0-import 정책" 은 도메인 import 만을 막습니다. 두 종류의 import 는 **명시적 계약**이므로 허용됩니다:

| import | 허용? | 이유 |
|:---|:---:|:---|
| `other.application.port.in.XxxUseCase` | ✅ | 동기 정문 (published contract) |
| `other.application.event.XxxEvent` | ✅ | 비동기 정문 (published contract) |
| `other.domain.model.*` | ❌ | 내부 구현 — 절대 import 금지 |
| `other.application.service.XxxService` | ❌ | 내부 구현 — 절대 import 금지 |
| `other.adapter.out.persistence.*` | ❌ | 내부 구현 — 절대 import 금지 |

즉 0-import 정책의 진짜 의미는 **"다른 컨텍스트의 도메인 / 내부 구현 import 0건"** 이지 **"모든 cross-context import 0건"** 이 아닙니다.

### 4. 이벤트 = published contract 답게 설계

import 을 허용하는 대신 이벤트 클래스에 엄격한 제약을 답니다. 페이로드 규칙은 [이벤트 페이로드 경량화](#이벤트-페이로드-경량화--0-import-정책) 참조.

```java
// ✅ 공개 계약다운 이벤트 — subscriber 가 import 해도 안전
public record NoticeCreatedEvent(
    Long noticeId,
    String noticeTitle,   // publisher 가 이미 가진 primitive
    Instant occurredAt
) {}

// ❌ 이건 이름만 Event 인 "위장된 도메인 노출"
public record NoticeCreatedEvent(
    Notice notice,                // 도메인 import 전염
    NoticeStatus status,          // 외부 enum 전염
    List<NoticeItem> items        // 도메인 컬렉션 전염
) {}
```

후자는 subscriber 가 import 하는 순간 Notice 의 모든 변경이 subscriber 까지 깨뜨립니다. 결국 [#6 0-import 정책](#6-cross-context-dto-격리-안티-커럽션-레이어--strict) 을 우회한 두 번째 정문이 됩니다.

### 5. 발행 컨텍스트는 비즈니스 사실만 (구독자 관심사 금지) ⭐

이벤트는 **"무슨 일이 일어났다"** 만 담습니다. **"누구한테 어떻게 알려야 한다"** 는 구독자의 책임입니다.

```java
// ❌ Bad — publisher 가 "누구한테 알림 가야 하나" 를 결정 (notification 책임 누수)
public record NoticeCreatedEvent(Long noticeId, String title, List<Long> recipientIds) {}

@Service
public class AdminCommandService {
    public void createNotice(...) {
        // ❌ admin 이 알림 대상자를 미리 조회 — 알림 도메인이 admin 으로 새 나감
        List<Long> recipientIds = userFetchPort.getEventAlarmEnabledUsers()...;
        publisher.publishEvent(NoticeCreatedEvent.of(id, title, recipientIds));
    }
}

// ✅ Good — publisher 는 사실만 알린다
public record NoticeCreatedEvent(Long noticeId, String noticeTitle) {}

@Service
public class AdminCommandService {
    public void createNotice(...) {
        publisher.publishEvent(NoticeCreatedEvent.of(id, title));  // ⭐ 사실만
    }
}

// 구독자가 자기 책임으로 대상자 조회
@Component
public class AdminNoticeCreateNotificationEventListener {
    @EventListener
    public void onNoticeCreated(NoticeCreatedEvent event) {
        List<NotificationUserInfo> recipients =
            userFetchPort.getEventAlarmEnabledUsers();   // ⭐ notification 의 책임
        ...
    }
}
```

**판단 기준**: 페이로드 필드가 publisher 컨텍스트의 비즈니스 사실인가? `noticeId`, `noticeTitle` = ✅ 사실. `recipientIds` = ❌ 알림 정책. `fcmTokens` = ❌ 인프라.

### 6. 트랜잭션 경계에 따른 2단계 리스너

```java
// 같은 트랜잭션 안에서 끝나야 하는 부수효과 (DB 쓰기, Outbox 저장)
@EventListener
public void onSomethingHappened(SomethingHappenedEvent e) { ... }

// 외부 호출 (FCM/메일/외부 API) — 커밋 후 + 비동기
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async("taskExecutor")
public void onSomethingHappenedAsync(SomethingHappenedEvent e) { ... }
```

- **DB 쓰기 / 제약 검증** → `@EventListener` (같은 트랜잭션)
- **외부 시스템 호출** → `AFTER_COMMIT` + `@Async` (롤백되면 외부 호출 X)
- 절대 `BEFORE_COMMIT` 에 외부 호출 두지 말 것.
- 외부 시스템 호출의 at-least-once 신뢰성은 [Transactional Outbox](#transactional-outbox-알림) 로 보강.

### 7. 리스너는 얇은 어댑터 — 비즈니스 로직 금지

```java
// ❌ Bad — 리스너에 비즈니스 로직 직접 작성
@EventListener
public void on(OrderPlacedEvent e) {
    var user = userRepository.findById(e.buyerId());  // Repository 직주입 — 정문 우회
    user.addPoints(...);
    userRepository.save(user);
}

// ✅ Good — 리스너는 "트리거 → UseCase 호출" 만
@EventListener
public void on(OrderPlacedEvent e) {
    pointInternalCommandUseCase.grantPoints(
        new GrantPointsCommand(e.buyerId(), ...)
    );
}
```

리스너의 비즈니스 로직은 UseCase 로 추출 → 테스트는 UseCase 에서. 리스너는 wiring 만 검증.

### 8. 동기 호출 vs 이벤트 — 판단 기준

이벤트를 남발하면 흐름이 추적 불가능해집니다.

| 상황 | 선택 |
|:---|:---|
| 결과가 비즈니스 흐름의 일부 (실패하면 본흐름 취소) | **동기 호출 (FetchPort / CommandPort)** |
| 부수효과, 알림, 분석, 캐시 무효화 (실패해도 본흐름 성공) | **이벤트** |

"본흐름이 성공/실패에 의존하는가?" 가 분기점입니다.

### 9. 패키지 구조 정리

```
{ctx}/
├── domain/
│   └── event/
│       └── {Fact}.java                    # 순수 도메인 이벤트 (POJO)
├── application/
│   └── event/
│       └── {Fact}Event.java               # 발행 — 자기 컨텍스트가 소유한 통합 이벤트
└── adapter/
    └── in/
        └── event/
            └── {Self}{Fact}EventListener.java   # 구독 — 다른 컨텍스트 이벤트 반응
```

**중요**: 리스너는 `adapter/in/event/` 에 둡니다 (애플리케이션 진입점이라는 의미). 이벤트 record 와 같은 디렉토리에 두면 발행/구독 방향이 한눈에 안 보입니다.

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

### Command 반환 타입 (⭐)
- **생성(Create)**: 생성된 리소스의 **ID를 반환**합니다 (Long 또는 ID를 포함한 record).
- **삭제(Delete)**: **void**를 반환합니다.
- **수정(Update)**: 일반적으로 **void**를 반환하나, 필요시 자체 Response record를 반환할 수 있습니다.

### 예외 처리
- **`catch (Exception ignored) {}` 절대 금지.** 예상된 null 케이스는 `Optional`.
- 모든 비즈니스 예외는 `BaseException(ErrorCode.XXX)`.
- 새 에러 케이스는 `ErrorCode` enum 에 추가.

### 트랜잭션
- `XxxQueryService` 클래스 레벨: `@Transactional(readOnly = true)`.
- `XxxCommandService` 클래스 레벨: `@Transactional` (쓰기).
- `Propagation.REQUIRES_NEW` 는 반드시 **별도 Bean** 에서 호출 (같은 클래스 내 호출 시 프록시 무시).

### 가독성
- **얼리 리턴(Early Return)** 우선. 깊은 중첩 `if/else` 대신 가드 절로 분기.
- **명확한 네이밍(Meaningful Naming)** — 추출한 private 메서드는 한 줄 주석 없이도 의도가 읽혀야 합니다 (`validateStateMatches`, `issueLoginTokens`, `issueSignupToken`, `toRegisterUserCommand`). 동사 + 목적어로 행위를 그대로 드러내세요.
- 분기 로직은 의도가 드러나는 private 메서드로 추출 — Service 진입 메서드 본문은 "흐름" 만 읽히도록.
- 비즈니스 검증은 Service 진입부에서 명시적으로 — null 검사 / 권한 / 상태 전이.

### 하드코딩 금지
- Magic number / 딜레이 / 재시도 횟수는 `application.yml` + `@Value` 주입.

### 도메인 모델 순수성
- `domain/*` 에 Spring / JPA / Jackson 등 인프라 어노테이션 절대 금지.
- 도메인 모델은 **불변(immutable) 지향** — 상태 변경은 새 객체 반환 또는 도메인 메서드 통해.
- `char Y/N` 같은 표현은 boolean 메서드(`isEnrollAlarmEnabled()`)로 노출.

### Cross-Domain 의존성 (⭐⭐ 0-import 정책)

타겟 상태: **자기 컨텍스트 어디에도 다른 컨텍스트의 `domain/*` import 0건** — Service / Port / Adapter / Event / Listener / DTO 모두.

- 다른 컨텍스트의 `Service` / `Reader` / `Repository` / `Entity` 직접 주입 **금지**.
- 다른 컨텍스트는 자기 `XxxFetchPort` → `XxxFetchAdapter` → 다른 컨텍스트의 `XxxQueryUseCase / XxxCommandUseCase` 체인으로만 호출.
- FetchAdapter 는 **UseCase 인터페이스**를 주입 (Service 구체 클래스 X).
- **Port 시그니처와 Service / Adapter / Event / Listener 어디에도 외부 도메인 모델(`User`, `Notice`, `Inquiry`, `Report`, `AuthToken` 등) 노출 금지.** 자기 컨텍스트의 record DTO 만 사용. 자세한 규칙은 [#6 Cross-Context DTO 격리](#6-cross-context-dto-격리-안티-커럽션-레이어--strict) 참조.
- **외부 enum(`Authority`, `Provider`, `NotificationTemplate` 등)도 Port / Event / Adapter 시그니처에 노출 금지** — `String` 으로 전달, 외부 enum 사용은 그 enum 을 소유한 모듈 안에서만.
- **소유 모듈의 정적 팩토리는 소유 모듈 안에서만 호출.** `User.createUser(...)` 같은 도메인 생성 책임이 호출자 컨텍스트로 새지 않도록 InternalCommandUseCase 는 DTO 입출력으로.
- **어댑터는 소유 모듈의 record (`{Ctx}InternalResponse`, `{Ctx}View`, `{Verb}Response`) 만 import.** 도메인 모델 import 0건.

#### 적용 상태 (점진 적용 중)
| 컨텍스트 | Service / Port / Event 격리 | 비고 |
|:---|:---:|:---|
| `oauth` | ✅ | `User`/`AuthToken`/`Authority` 모두 record/String 화 |
| `report` | ✅ | `ReportInternalResponse` / `ReportView` 분리 |
| `notice` | ✅ | `NoticeInternalResponse` / `NoticeView` + 이벤트 식별자만 |
| `inquiry` | ✅ | `InquiryInternalResponse` / `InquiryView` + 이벤트 식별자만 |
| 그 외 (`board`, `enroll`, `chat`, `notification` 등) | ⚠️ 추후 적용 | `User`/`Board` 등 도메인 import 남아있음 |

### 이벤트 페이로드 경량화 (⭐⭐ 0-import 정책)

> 전체 이벤트 설계 원칙(소유권, 패키지 위치, 동기 vs 이벤트 판단 기준 등)은 [이벤트 패키지 & 의존성 규칙](#이벤트-패키지--의존성-규칙-) 섹션 참조. 여기는 페이로드 필드 규칙만 다룹니다.

이벤트 record 의 import 와 필드는 모두 **자기 컨텍스트 + `common` + JDK** 만 허용. 다른 컨텍스트의 도메인 객체, 외부 enum, 외부 템플릿 클래스 어떤 것도 이벤트로 흘려보내지 마세요.

#### 페이로드에 담을 수 있는 것
- `Long userId`, `Long boardId` 같은 **식별자**
- `List<Long> recipientIds` 같은 **식별자 리스트**
- `String title`, `String noticeTitle` 같은 **자기 컨텍스트가 이미 가진 primitive 문자열**

#### 페이로드에 넣지 말 것
- 다른 컨텍스트의 도메인 모델 (`User`, `Notice`, `Inquiry` 등)
- 다른 컨텍스트의 enum / 템플릿 (`NotificationTemplate`, `Authority`, `Provider` 등)
- `List<User>` 같은 도메인 모델 컬렉션 — `List<Long>` 으로 변환해서 발행

#### 왜?
- ① 메시지 큐로 외부화하면 도메인 객체 직렬화가 깨짐
- ② 리스너가 발행 시점의 stale 한 상태를 보게 됨
- ③ 큰 객체 trip cost
- ④ 이벤트 클래스가 다른 컨텍스트 도메인을 import 하면 결합이 새 나감 (이벤트는 발행 컨텍스트 소유)

#### 리스너의 책임
- 식별자로 자기 FetchPort 를 통해 다시 조회
- 외부 템플릿 (`NotificationTemplate` 등) 사용은 **템플릿을 소유한 모듈의 리스너에서**. 발행 컨텍스트는 템플릿을 모름.

```java
// ❌ Bad — 도메인 객체 + 외부 enum 통째로 페이로드에 포함
public record AdminNoticeCreateNotificationEvent(
        Notice notice, List<User> recipients,
        String title, String body, String type   // ← NotificationTemplate 으로 계산
) {
    public static AdminNoticeCreateNotificationEvent of(Notice notice, List<User> recipients) {
        String title = NotificationTemplate.NOTICE_CREATED.getTitle();      // ← 외부 템플릿 import
        ...
    }
}

// ✅ Good — 식별자 + 자기 컨텍스트 primitive 만
public record AdminNoticeCreateNotificationEvent(
        Long noticeId,
        String noticeTitle,           // 발행 컨텍스트가 이미 가진 primitive
        List<Long> recipientIds       // 식별자 리스트
) {}

// 리스너에서 템플릿 사용 + recipient 조회
@EventListener
public void saveNoticeNotifications(AdminNoticeCreateNotificationEvent event) {
    String title = NotificationTemplate.NOTICE_CREATED.getTitle();
    String body  = NotificationTemplate.NOTICE_CREATED.formatBody(event.noticeTitle());
    List<User> recipients = userFetchPort.getUsers(event.recipientIds());
    ...
}
```

### 절대 변경 금지
- **이중 단계 이벤트 리스너** (Transactional Outbox): 단순화 X.
- **RedisPublisher 분리**: `RedisPublisher`(NotificationDispatchPort 구현) 와 `ChatMessageRedisPublisher`(@TransactionalEventListener) 는 의도적으로 분리됨. 합치면 Spring proxy 충돌.

---

## 새 기능 추가 가이드

### API 엔드포인트 추가
1. `{ctx}/domain/model/` — 도메인 모델 (순수 Java, 어노테이션 X)
2. `common/error/ErrorCode` — 필요한 에러 케이스
3. `{ctx}/application/port/out/` — Repository 메서드 추가; 다른 컨텍스트 호출이 필요하면 `XxxFetchPort` 정의 — **시그니처는 반드시 자기 컨텍스트의 record DTO** (외부 도메인/enum 노출 X, String 으로)
4. `{ctx}/adapter/out/persistence/` — JPA Entity, Repository 구현
5. `{ctx}/adapter/out/external/` — Fetch Port 구현 — **호출자 목적에 맞는 UseCase 인터페이스 주입** (일반 도메인 → `XxxInternalQueryUseCase`, admin → `XxxAdminQueryUseCase`, 변경 → `XxxCommandUseCase`). 외부 도메인 ↔ 자체 record 매핑은 **여기서만**.
6. `{ctx}/application/dto/` — Command / Response DTO
   - 자기 UseCase 입출력용 DTO
   - **FetchPort/CommandPort 가 받고 돌려줄 자체 record** (e.g., `RegisteredUserSummary`, `CreatedUserSummary`, `IssuedTokenPair`)
   - 다른 컨텍스트가 자기 모듈에 들어와 도메인을 짓게 하려면 **소유 모듈의 dto/command 에 `CreateXxxCommand` record + dto/response 에 `CreatedXxxResponse` record + `from(Domain)` 정적 매퍼** 제공
7. `{ctx}/application/port/in/` — 메서드의 **호출자가 누구인지** 로 정문을 고른다:

   | 호출자 | 반환 타입 | 정문 |
   |:---|:---|:---|
   | Controller | 화면 DTO | `{Ctx}ClientQueryUseCase` |
   | 다른 컨텍스트 FetchAdapter / WebSocket / Scheduler | 도메인 모델 (또는 자체 Response record) | `{Ctx}InternalQueryUseCase` |
   | admin FetchAdapter | `Page`, `Map`, `count` | `{Ctx}AdminQueryUseCase` |
   | 변경 동작 | **자체 Response record** (도메인 노출 X) | `{Ctx}CommandUseCase` / `{Ctx}InternalCommandUseCase` |

   - 한 인터페이스에 호출자가 섞이면 ISP 위반. 정문을 새로 만든다.
   - 정문을 거치는 외부 호출자가 0명이면 UseCase 가 아니다 — Service 의 private 메서드나 `{Ctx}Reader` 로 흡수.
8. `{ctx}/application/service/` — 구현 + 트랜잭션:
   - `{Ctx}CommandService implements {Ctx}CommandUseCase` (@Transactional)
   - `{Ctx}QueryService implements {Ctx}ClientQueryUseCase, {Ctx}InternalQueryUseCase, {Ctx}AdminQueryUseCase` (@Transactional(readOnly=true))
   - `{Ctx}Reader` (@Component) — Repository 위임 + 단건 조회 throw. Service 가 Repository 대신 Reader 만 의존.
9. `{ctx}/adapter/in/web/controller/` — Controller 는 `{Ctx}ClientQueryUseCase` + `{Ctx}CommandUseCase` 만 주입 (Internal/Admin 정문은 진입 X)

### 알림 타입 추가
1. `notification/domain/enums/AlarmType` 에 enum
2. `notification/domain/model/NotificationTemplate` 에 템플릿
3. `{publisher-ctx}/application/event/{Fact}Event` 생성 — **발행 컨텍스트가 소유**, 페이로드는 식별자 + primitive 만 ([이벤트 패키지 & 의존성 규칙](#이벤트-패키지--의존성-규칙-))
4. `notification/adapter/in/event/{Publisher}{Fact}EventListener` — **구독자(notification)에 위치**, 이중 단계 패턴 준수, 알림 대상자 조회는 여기서
5. 발행 컨텍스트 Service 에서 `applicationEventPublisher.publishEvent(...)` — 사실만 발행 (구독자 관심사 X)

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
