---
trigger: glob
description: Backend Architecture Guide (Hexagonal + DDD, Single Module)
globs: "**/*.java"
---

# Backend Architecture (Hexagonal + DDD)

> Java 작업 시 적용되는 아키텍처 **단일 출처(SSOT)**. 항상 로드되는 개요는 `CLAUDE.md` 참조.

## 의존성 방향 (바깥 → 안쪽 한 방향)

```
adapter/in/web (Controller)
   └─→ application/port/in (UseCase 인터페이스)        ◀ Input Port
            ↑ (implements)
       application/service ({Ctx}Service)
            ├─→ application/port/out (own Repository)  ◀ Output Port
            │        ↑ (implements)
            │       adapter/out/persistence
            │
            └─→ application/port/out (XxxFetchPort)    ◀ Cross-context Output Port
                     ↑ (implements)
                    adapter/out/external (Fetch Adapter → 상대 컨텍스트의 UseCase 인터페이스)
```

**금지된 의존성**: ❌ Controller → Service 구현체 / ❌ Service → 다른 컨텍스트의 Service·Reader·Repository·Entity / ❌ Application → Adapter 구현체 / ❌ Domain → Spring·JPA / ❌ Common → 다른 패키지.

## 계층별 책임

| 계층 | 패키지 | 책임 |
|:---|:---|:---|
| Adapter In (Web) | `{ctx}.adapter.in.web` | HTTP 수신, Request → Command, UseCase 호출 |
| Adapter In (WebSocket) | `{ctx}.adapter.in.websocket` | STOMP 수신 → UseCase 호출 |
| Adapter In (Event) | `{ctx}.adapter.in.event` | 다른 컨텍스트 이벤트 구독 → UseCase 호출 |
| Input Port | `{ctx}.application.port.in` | UseCase 인터페이스 (interface only) |
| Service | `{ctx}.application.service` | UseCase 구현, 트랜잭션 경계, own Repository/Reader, Fetch Port, 이벤트 발행 |
| Domain Model | `{ctx}.domain.model` | Aggregate, Entity, VO, 도메인 이벤트 |
| Output Port | `{ctx}.application.port.out` | Repository + cross-context `XxxFetchPort` 인터페이스 |
| Adapter Out (Persistence) | `{ctx}.adapter.out.persistence` | JPA Entity, Repository 구현 |
| Adapter Out (External) | `{ctx}.adapter.out.external` | FCM/S3/OAuth + Fetch Port 구현 |

---

## 1. Controller → UseCase 인터페이스 (Command / Query 분리)

```java
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardClientCommandUseCase boardClientCommandUseCase;   // ⭐ 인터페이스
    private final BoardClientQueryUseCase   boardClientQueryUseCase;     // ⭐ 인터페이스

    @PostMapping("/boards")
    public ResponseEntity<BoardCreateResponse> create(
            @AuthUser Long userId, @RequestBody @Valid BoardCreateRequest req) {
        return ResponseEntity.ok(boardClientCommandUseCase.createBoard(userId, req.toCommand()));
    }

    @GetMapping("/boards/{boardId}")
    public ResponseEntity<BoardDetailResponse> get(@AuthUser Long userId, @PathVariable Long boardId) {
        return ResponseEntity.ok(boardClientQueryUseCase.getBoard(userId, boardId));
    }
}
```

## 2. UseCase = Command / Query + 호출자별 분리 (ISP, ⭐ 강제)

한 컨텍스트의 Input Port(UseCase) 는 **반드시 Command 와 Query 로 분리**하고, 다시 **호출자 목적별로 ISP 분리**합니다.

```
{ctx}/application/port/in/
├── {Ctx}ClientCommandUseCase       # 웹/앱 컨트롤러의 상태 변경 요청
├── {Ctx}InternalCommandUseCase     # 다른 컨텍스트에서 오는 상태 변경 요청
│
├── {Ctx}ClientQueryUseCase         # 웹/앱 컨트롤러 진입 — 화면 전용 Response DTO 반환
├── {Ctx}InternalQueryUseCase       # 다른 컨텍스트 FetchAdapter / 인프라 진입 — 자체 Response record 반환
└── {Ctx}AdminQueryUseCase          # 관리자 / 대시보드 / 통계 진입 — 페이징 / 집계 / Map / count
```

**왜 갈라지나 (ISP)**: 같은 "조회/변경"이라도 호출자 목적이 다르면 반환 타입·보안 수준·결합도가 다릅니다. 하나에 섞으면 컨트롤러가 자기와 무관한 내부용 메서드를 의존하게 됨. 갈라 두면 Mocking 깔끔, 변경 영향 격리.

### Service 구현 — 호출자 그룹별로 분리

```
{ctx}/application/service/
├── {Ctx}ClientCommandService   implements {Ctx}ClientCommandUseCase
├── {Ctx}ClientQueryService     implements {Ctx}ClientQueryUseCase
├── {Ctx}InternalCommandService implements {Ctx}InternalCommandUseCase
└── {Ctx}InternalQueryService   implements {Ctx}InternalQueryUseCase, {Ctx}AdminQueryUseCase
```

`AdminQueryUseCase` 는 성격이 유사한 `InternalQueryService` 에서 함께 구현.

### Service 내부 조회 협력자 — `{Ctx}Reader`

같은 Repository 호출 + "없으면 throw" 반복을 `{Ctx}Reader`(@Component)로 분리.
- Reader 는 **Repository 만 의존**. 단건 조회의 `orElseThrow` 책임을 가짐 (`getUser(id)` — 없으면 `BaseException`).
- `XxxQueryService` 는 Repository 직접 의존 X, **Reader 만** 의존 → Service 는 DTO 변환·cross-context 조합에 집중.

**⚠️ 두 종류의 Reader 구분**:
- ✅ 내부 협력자 Reader: `{Ctx}Service` 가 **같은 컨텍스트 안에서만** 주입.
- ❌ 외부 정문 Reader (anti-pattern): 다른 컨텍스트 FetchAdapter 가 `XxxReader` 직접 주입 — UseCase 우회. 절대 금지.

```java
// application/port/in/UserInternalQueryUseCase.java
public interface UserInternalQueryUseCase {
    UserInternalResponse getUser(Long userId);              // 자체 record 반환
    List<UserInternalResponse> getUsers(List<Long> userIds);
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
}

// application/service/UserInternalQueryService.java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserInternalQueryService implements UserInternalQueryUseCase, UserAdminQueryUseCase {
    private final UserReader userReader;        // ⭐ Repository 직접 의존 X
    private final ClubFetchPort clubFetchPort;  // cross-context 는 Port 로
    @Override public UserInternalResponse getUser(Long id) { return toInternalResponse(userReader.getUser(id)); }
}
```

**금지**:
- 하나의 `XxxService` 가 `Client + Internal` **동시 구현** (그룹 분리 깨짐)
- 하나의 `XxxService` 가 `Command + Query` **동시 구현** (CQRS 분리 깨짐)
- 하나의 인터페이스에 Client/Internal/Admin 메서드 **섞기** (ISP 위반)
- 호출자 0명인 UseCase (→ Service private 메서드 / Reader 로 흡수)

## 3. Cross-Domain 호출 = Strict 5-Hop Chain (⭐⭐ 절대 우회 금지)

다른 컨텍스트의 Service / Reader / Repository / Entity 를 **직접 주입 X**. 반드시:

```
[A Service] → [A Out Port: XxxFetchPort] ←impl [A Out Adapter: AXxxFetchAdapter]
   → [B In Port: XxxQueryUseCase / XxxCommandUseCase 인터페이스] ←impl [B Service]
```

A 의 FetchAdapter 는 B 의 `XxxService` 구체 클래스 / `XxxReader` 가 아니라 **호출자 목적에 맞는 UseCase 인터페이스**를 주입:

| A 컨텍스트의 정체 | 주입할 B 의 UseCase |
|:---|:---|
| 일반 도메인 (board, enroll, chat 등) — 읽기 | `BInternalQueryUseCase` |
| 일반 도메인 — 쓰기 | `BInternalCommandUseCase` |
| admin 컨텍스트 | `BAdminQueryUseCase` (+ 필요 시 `BInternalCommandUseCase`) |

```java
@Component
@RequiredArgsConstructor
public class BoardUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase; // ⭐ Internal 정문
    @Override public RegisteredUserSummary getUser(Long userId) {
        UserInternalResponse res = userInternalQueryUseCase.getUser(userId);
        return new RegisteredUserSummary(res.id(), res.nickName());  // 자기 record 로 매핑
    }
}
```

**금지 (PR 리뷰 reject)**:
```java
private final UserQueryService userQueryService;     // ❌ 상대 Service 구체 클래스
private final UserReader userReader;                 // ❌ 상대 Reader 두 번째 정문
private final UserRepository userRepository;         // ❌ 상대 Repository
private final BoardUserFetchAdapter ...;             // ❌ 자기 Service 가 Adapter 직주입 (Port 건너뜀)
private final UserAdminQueryUseCase ...;             // ❌ 정문 오선택 (board 가 admin 통계 정문)
```

## 4. Output Port DIP (Repository)

```java
public interface BoardRepository {            // application/port/out
    Optional<Board> findById(Long id);
    Board save(Board board);
}
@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {  // adapter/out/persistence
    private final JpaBoardRepository jpa;
    private final QueryDslBoardRepository qdsl;
    @Override public Optional<Board> findById(Long id) { return jpa.findById(id).map(BoardEntity::toModel); }
}
```

## 5. UseCase = 유일한 정문

| 호출자 | 위치 | 진입 정문 |
|:---|:---|:---|
| Web/App Controller | `{ctx}/adapter/in/web/controller/` | `{Ctx}ClientQueryUseCase` / `{Ctx}CommandUseCase` |
| WebSocket Listener | `{ctx}/adapter/in/websocket/` | 보통 `{Ctx}InternalQueryUseCase` |
| 다른 컨텍스트 FetchAdapter (일반) | `{other}/adapter/out/external/` | `{Ctx}InternalQueryUseCase` / `{Ctx}CommandUseCase` |
| admin FetchAdapter | `admin/adapter/out/external/` | `{Ctx}AdminQueryUseCase` (+ 필요 시 Command) |
| Scheduler / AOP Finder | `global/...` | 보통 `{Ctx}InternalQueryUseCase` |

`XxxService` 구체 클래스 / `XxxRepository` / `XxxReader` / JPA `XxxEntity` 를 외부에서 import 하면 정문 우회 — reject.

## 6. Cross-Context DTO 격리 (안티-커럽션, ⭐⭐ Strict, 0-import)

다른 컨텍스트의 도메인 모델(`User`, `Board`, `AuthToken` 등) 자체를 Service / Port 시그니처에 import 하면 결합이 새 나갑니다. 외부 도메인은 어댑터 한 곳에서만 만지고, 나머지 레이어는 자기 컨텍스트 record DTO 만 압니다.

| 위치 | 외부 도메인 import | 외부 enum import |
|:---|:---:|:---:|
| `domain/*`, `application/service/*`, `port/in/*`, `port/out/*`, `application/event/*` | ❌ | ❌ |
| `application/dto/*` | ❌ | ⚠️ 가급적 String |
| `adapter/out/external/*` (FetchAdapter) | ❌ (소유 모듈 record 만) | ❌ |
| `adapter/in/event/*` (Listener) | ❌ (record 만) | ❌ |

- 외부 데이터는 **자기 컨텍스트 record DTO** 로 (`RegisteredUserSummary`, `IssuedTokenPair`, `NoticeSnapshot` 등).
- 외부 enum(`Authority`, `Provider`, `NotificationTemplate`)도 **String 으로 전달**.
- **소유 모듈의 정적 팩토리(`User.createUser(...)`)는 소유 모듈 안에서만.** InternalCommandUseCase 는 DTO 입출력.

### 정문별로 record 도 분리 (ISP 연장)

| 정문 | 반환 record (예) | 누가 받나 |
|:---|:---|:---|
| `{Ctx}ClientQueryUseCase` | `{Ctx}Response`, `{Ctx}DetailResponse` | Controller |
| `{Ctx}InternalQueryUseCase` | `{Ctx}InternalResponse` | 다른 컨텍스트 FetchAdapter / Scheduler / AOP |
| `{Ctx}AdminQueryUseCase` | `{Ctx}View` | admin FetchAdapter |

필드가 지금 같아도 **타입은 다르게** — 한 record 를 셋이 공유하면 한쪽 변화가 모든 호출자를 깨뜨림(ISP 위반). DTO `from(Domain)` 매퍼에서 **자기 컨텍스트** 도메인 import 는 허용.

## 7. 컨텍스트 안에 Aggregate 가 여러 개일 때 (chat, user 등)

각 Aggregate 마다 Command/Query UseCase + Service 쌍. 같은 컨텍스트 협력자이므로 Fetch Port 없이 직접 호출 가능 — 단 호출 대상은 여전히 **UseCase 인터페이스** (Service 구체 클래스 직주입 금지).

---

## 새 API 엔드포인트 추가 순서

1. `{ctx}/domain/model/` — 도메인 모델 (순수 Java, 어노테이션 X)
2. `common/error/ErrorCode` — 새 에러 케이스
3. `{ctx}/application/port/out/` — Repository 메서드 + 필요 시 `XxxFetchPort` (시그니처는 자기 record DTO, 외부 도메인/enum 노출 X)
4. `{ctx}/adapter/out/persistence/` — JPA Entity, Repository 구현
5. `{ctx}/adapter/out/external/` — Fetch Port 구현 (호출자 목적에 맞는 UseCase 주입, 외부↔자체 record 매핑은 여기서만)
6. `{ctx}/application/dto/` — Command / Response DTO + FetchPort 용 자체 record
7. `{ctx}/application/port/in/` — 호출자로 정문 선택 (Controller=Client / 다른 컨텍스트=Internal / admin=Admin / 변경=Command)
8. `{ctx}/application/service/` — Command/Query/그룹별 Service + `{Ctx}Reader`
9. `{ctx}/adapter/in/web/controller/` — Controller 는 Client 정문 + Command 정문만 주입
