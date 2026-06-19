---
trigger: glob
description: Backend Coding Conventions (Naming, Error Handling, Entity Mapping, Transaction)
globs: "**/*.java"
---

# Backend Coding Conventions

## 1. 네이밍 규칙

### 클래스 네이밍
| 유형 | 패턴 | 예시 | 패키지 |
|:---|:---|:---|:---|
| Controller | `{Domain}Controller` | `BoardController` | `{ctx}.adapter.in.web.controller` |
| WebSocket Listener | `{Domain}WebSocketSessionEventListener` | `ChatWebSocketSessionEventListener` | `{ctx}.adapter.in.websocket` |
| Input Port (UseCase) | `{Domain}{Client/Internal/Admin}{Command/Query}UseCase` | `BoardClientCommandUseCase` | `{ctx}.application.port.in` |
| Service (UseCase 구현) | `{Domain}{Client/Internal/Admin?}{Command/Query?}Service` | `BoardClientCommandService` | `{ctx}.application.service` |
| Aggregate Service (서브) | `{Aggregate}Service` | `ChatRoomService`, `BlockService` | `{ctx}.application.service` |
| Event | `{Domain}{Action}Event` | `EnrollNotificationEvent` | `{ctx}.application.event` |
| Event Listener | `{Domain}{Action}EventListener` | `EnrollNotificationEventListener` | `{ctx}.application.event` |
| Command | `{Domain}{Action}Command` | `BoardCreateCommand` | `{ctx}.application.dto.command` |
| Response | `{Domain}{Detail?}Response` | `BoardDetailResponse` | `{ctx}.application.dto.response` |
| Request | `{Domain}{Action}Request` | `BoardCreateRequest` | `{ctx}.adapter.in.web.dto.request` |
| Output Port (Repository) | `{Domain}Repository` | `BoardRepository` | `{ctx}.application.port.out` |
| Output Port (Cross-context) | `{Source}FetchPort` | `UserFetchPort` | `{ctx}.application.port.out` |
| Repository Impl | `{Domain}RepositoryImpl` | `BoardRepositoryImpl` | `{ctx}.adapter.out.persistence.repository` |
| JPA Repository | `Jpa{Domain}Repository` | `JpaBoardRepository` | `{ctx}.adapter.out.persistence.repository` |
| QueryDSL Repository | `QueryDsl{Domain}Repository` | `QueryDslBoardRepository` | `{ctx}.adapter.out.persistence.repository` |
| JPA Entity | `{Domain}Entity` | `BoardEntity` | `{ctx}.adapter.out.persistence.entity` |
| Domain Model | `{Domain}` | `Board` | `{ctx}.domain.model` |
| Fetch Adapter | `{OwnCtx}{Source}FetchAdapter` | `BoardUserFetchAdapter` | `{ctx}.adapter.out.external` |

**금지 접미사**: `Orchestrator`, `ApplicationService`, `Facade`. UseCase 구현은 모두 `{Ctx}Service`.

### 메서드 네이밍
| 용도 | 패턴 |
|:---|:---|
| 단건 조회 (없으면 예외) | `get{Domain}(id)` |
| 단건 조회 (Optional) | `find{Domain}(id)` |
| 목록 조회 | `get{Domain}List(...)` |
| 생성 | `create{Domain}(...)` |
| 수정 | `update{Domain}(...)` |
| 삭제 | `delete{Domain}(...)` |

UseCase 메서드 (Response 반환) 와 같은 컨텍스트의 entity getter (도메인 모델 반환) 가 같은 이름+시그니처로 충돌하면 entity getter 를 `{name}Entity` 로 표기하세요 (e.g., `getInquiryEntity(Long)`).

## 2. 예외 처리

- **모든 예외**는 `BaseException` + `ErrorCode` enum.
- 절대 `catch (Exception ignored) {}` 또는 빈 catch 금지.
- 예상된 null 케이스는 `Optional` 반환.

```java
// ✅
public Board getBoard(Long id) {
    return boardRepository.findById(id)
            .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
}

// ✅
public Optional<Enroll> findEnroll(Long id) {
    return enrollRepository.findById(id);
}
```

## 3. Entity ↔ Domain ↔ DTO 변환

### Entity ↔ Domain
- JPA Entity (`{ctx}.adapter.out.persistence.entity`) ↔ Domain 모델 (`{ctx}.domain.model`) 분리.
- 변환: `Entity.toModel()`, `Entity.from(domain)` — Entity 내부.

### Domain ↔ DTO (Response)
- DTO(record) 의 `from(Domain)` 정적 팩토리에서 **자기 컨텍스트의 도메인 모델** import 는 허용합니다. Service 의 private 매퍼로 강제 분리하지 않습니다.
- **다른 컨텍스트의 도메인 모델은 DTO 에 import 금지** ([#6 Cross-Context DTO 격리](#) 참조 — Anti-Corruption Layer).

```java
// ✅ 자기 컨텍스트 도메인 매핑은 DTO 안에서 OK
public record BoardDetailResponse(Long boardId, String title, ...) {
    public static BoardDetailResponse from(Board board) {
        return new BoardDetailResponse(board.getId(), board.getTitle(), ...);
    }
}

// 또는 Service 안에서 매핑해도 OK — 취향
@Service
public class BoardClientQueryService implements BoardClientQueryUseCase {
    @Override
    public BoardDetailResponse getBoard(Long userId, Long id) {
        Board board = boardReader.getBoard(id);
        return BoardDetailResponse.from(board);
    }
}
```

## 4. 트랜잭션 경계

- `{Ctx}Service` 클래스 레벨 기본값: `@Transactional(readOnly = true)`.
- 쓰기 메서드만 `@Transactional` 오버라이드.
- `Propagation.REQUIRES_NEW` 는 **별도 Bean** 호출 필수.

```java
@Service
@Transactional(readOnly = true)
public class BoardService implements BoardUseCase {

    @Override
    public BoardDetailResponse getBoard(Long userId, Long id) { ... }  // readOnly 상속

    @Override
    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand cmd) { ... }
}

@Service
public class NotificationOutboxUpdater {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusSuccess(Long outboxId) { ... }
}
```

## 5. Null 처리 / 도메인 의도 노출

- `Character Y/N` 같은 표현은 도메인 모델에서 boolean 메서드로 래핑.

```java
// ✅
public boolean isEnrollAlarmEnabled() { return 'Y' == this.enrollAlarm; }
if (!recipient.isEnrollAlarmEnabled()) return;

// ❌
if (recipient.getEnrollAlarm() != 'Y') return;
```

## 6. 하드코딩 금지

Magic number / 딜레이 / 재시도는 `application.yml` + `@Value`.

```java
@Value("${notification.outbox.max-retry-count:5}")
private int maxRetryCount;

@Scheduled(fixedDelayString = "${notification.outbox.scheduler-delay-ms:60000}")
public void processPendingPush() { ... }
```

## 7. Service 필드 순서 (반드시 통일)

`{Ctx}Service` 의 `private final` 필드는 항상 아래 그룹 순으로 선언하고, 그룹 안에서는 **타입 이름 알파벳순**으로 정렬합니다. 그룹 사이는 빈 줄 한 줄.

```java
public class BoardService implements BoardUseCase {

    // 1. own Repository
    private final BoardRepository boardRepository;

    // 2. own 다른 output port (XxxPort, 예: TokenProvider, ImageUploaderPort)
    private final TokenProvider tokenProvider;

    // 3. 같은 컨텍스트 협력 Service (multi-aggregate 컨텍스트만 해당)
    private final ChatRoomService chatRoomService;

    // 4. Cross-context Fetch Port (알파벳순)
    private final BlockFetchPort blockFetchPort;
    private final BookmarkFetchPort bookmarkFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final EnrollFetchPort enrollFetchPort;
    private final GameFetchPort gameFetchPort;
    private final UserFetchPort userFetchPort;

    // 5. Spring / Lombok infra (ApplicationEventPublisher, ObjectMapper)
    private final ApplicationEventPublisher applicationEventPublisher;
}
```

## 8. import 방향성 체크리스트

| 작성 중인 파일 | import 가능 | import 금지 |
|:---|:---|:---|
| `{ctx}.adapter.in.web.*` | `{ctx}.application.port.in.*`, `{ctx}.application.dto.*` | `{ctx}.application.service.*` (구현체), 다른 `{other}.*` |
| `{ctx}.application.service.*` | `{ctx}.application.port.{in,out}.*`, `{ctx}.domain.*`, `common.*` | `{ctx}.adapter.*`, 다른 `{other}.*` (Fetch Port를 거치세요) |
| `{ctx}.domain.*` | 같은 `{ctx}.domain.*`, `common.*` | Spring, JPA, `{ctx}.application.*`, `{ctx}.adapter.*` |
| `{ctx}.adapter.out.persistence.*` | `{ctx}.application.port.out.*`, `{ctx}.domain.model.*`, JPA / QueryDSL | `{ctx}.application.service.*`, `{ctx}.adapter.in.*` |
| `{ctx}.adapter.out.external.*` | `{ctx}.application.port.out.*FetchPort`, 다른 `{other}.application.service.*` (Fetch Adapter 내부에서 래핑할 때만) | `{ctx}.application.service.*` |
