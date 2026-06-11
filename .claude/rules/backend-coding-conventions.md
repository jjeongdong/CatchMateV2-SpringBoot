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
| Input Port (UseCase) | `{Domain}UseCase` | `BoardUseCase` | `{ctx}.application.port.in` |
| Application Service | `{Domain}ApplicationService` | `BoardApplicationService` | `{ctx}.application.service` |
| Domain/Thin Service | `{Domain}Service` | `BoardService`, `GameService` | `{ctx}.application.service` or `{ctx}.domain.service` |
| Event | `{Domain}{Action}Event` | `EnrollNotificationEvent` | `{ctx}.application.event` |
| Event Listener | `{Domain}{Action}EventListener` | `EnrollNotificationEventListener` | `{ctx}.application.event` |
| Command | `{Domain}{Action}Command` | `BoardCreateCommand` | `{ctx}.application.dto.command` |
| Response | `{Domain}{Detail?}Response` | `BoardDetailResponse` | `{ctx}.application.dto.response` |
| Request | `{Domain}{Action}Request` | `BoardCreateRequest` | `{ctx}.adapter.in.web.dto.request` |
| Output Port (Repository) | `{Domain}Repository` | `BoardRepository` | `{ctx}.application.port.out` |
| Repository Impl | `{Domain}RepositoryImpl` | `BoardRepositoryImpl` | `{ctx}.adapter.out.persistence.repository` |
| JPA Repository | `Jpa{Domain}Repository` | `JpaBoardRepository` | `{ctx}.adapter.out.persistence.repository` |
| QueryDSL Repository | `QueryDsl{Domain}Repository` | `QueryDslBoardRepository` | `{ctx}.adapter.out.persistence.repository` |
| JPA Entity | `{Domain}Entity` | `BoardEntity` | `{ctx}.adapter.out.persistence.entity` |
| Domain Model | `{Domain}` | `Board` | `{ctx}.domain.model` |

**`Orchestrator` 명명 금지** — `ApplicationService` 로 통일합니다.

### 메서드 네이밍
| 용도 | 패턴 |
|:---|:---|
| 단건 조회 (없으면 예외) | `get{Domain}(id)` |
| 단건 조회 (Optional) | `find{Domain}(id)` |
| 목록 조회 | `get{Domain}List(...)` |
| 생성 | `create{Domain}(...)` |
| 수정 | `update{Domain}(...)` |
| 삭제 | `delete{Domain}(...)` |

## 2. 예외 처리

- **모든 예외**는 `BaseException` + `ErrorCode` enum 으로 분류.
- 절대로 `catch (Exception ignored) {}` 또는 빈 catch 블록 금지.
- 예상된 null 케이스는 `Optional` 반환.

```java
// ✅ Good — 예외 명시
public Board getBoard(Long id) {
    return boardRepository.findById(id)
            .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
}

// ✅ Good — Optional 반환
public Optional<Enroll> findEnroll(Long id) {
    return enrollRepository.findById(id);
}

// ❌ Bad
try { status = enrollService.getEnroll(id).getAcceptStatus(); } catch (Exception ignored) {}
```

## 3. Entity ↔ Domain 모델 변환

- JPA Entity (`{ctx}.adapter.out.persistence.entity`) ↔ Domain 모델 (`{ctx}.domain.model`) 을 **분리**.
- 변환: `Entity.toModel()` (Entity → Domain), `Entity.from(domain)` (Domain → Entity).
- 변환 로직은 **Entity 클래스 내부**에 위치.

```java
// ✅ {ctx}.adapter.out.persistence.entity.BoardEntity
@Entity
public class BoardEntity extends BaseEntity {
    public Board toModel() {
        return Board.builder()
                .id(this.id)
                .title(this.title)
                .build();
    }
    public static BoardEntity from(Board board) {
        BoardEntity e = new BoardEntity();
        e.title = board.getTitle();
        return e;
    }
}

// ✅ Repository 구현체에서 변환
@Override
public Optional<Board> findById(Long id) {
    return jpaBoardRepository.findById(id).map(BoardEntity::toModel);
}
```

## 4. 트랜잭션 경계

- `ApplicationService` 클래스 레벨 기본값: `@Transactional(readOnly = true)`.
- 쓰기 메서드만 `@Transactional` 로 오버라이드.
- `Propagation.REQUIRES_NEW` 는 반드시 **별도 Bean** 에서 호출해야 합니다 (같은 클래스 내 호출 시 프록시 무시).

```java
// ✅ ApplicationService
@Service
@Transactional(readOnly = true)
public class BoardApplicationService implements BoardUseCase {

    @Override
    public BoardDetailResponse getBoard(Long id) { ... }  // readOnly 상속

    @Override
    @Transactional  // 쓰기는 오버라이드
    public BoardCreateResponse createBoard(BoardCreateCommand cmd) { ... }
}

// ✅ REQUIRES_NEW는 별도 Bean
@Service
public class NotificationOutboxUpdater {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusSuccess(Long outboxId) { ... }
}
```

## 5. Null 처리

- 메서드 파라미터/반환값의 null 여부를 명확히 설계.
- 도메인 모델의 boolean 속성은 `Character Y/N` 대신 의미 있는 메서드로 노출.

```java
// ✅ 의미 있는 메서드
public boolean isEnrollAlarmEnabled() { return 'Y' == this.enrollAlarm; }
if (!recipient.isEnrollAlarmEnabled()) return;

// ❌ Bad — 구현 상세 노출
if (recipient.getEnrollAlarm() != 'Y') return;
```

## 6. 하드코딩 금지

Magic number, 딜레이, 재시도 횟수는 코드에 직접 작성하지 않고 `application.yml` + `@Value`.

```java
// ❌ Bad
@Scheduled(fixedDelay = 60000)
private static final int MAX_RETRY = 5;

// ✅ Good
@Value("${notification.outbox.max-retry-count:5}")
private int maxRetryCount;

@Scheduled(fixedDelayString = "${notification.outbox.scheduler-delay-ms:60000}")
public void processPendingPush() { ... }
```

## 7. import 방향성 체크리스트

코드 리뷰 / 작성 시 import 줄을 보면 의존성 방향이 보입니다.

| 작성 중인 파일 | import 가능 | import 금지 |
|:---|:---|:---|
| `{ctx}.adapter.in.web.*` | `{ctx}.application.port.in.*`, `{ctx}.application.dto.*` | `{ctx}.application.service.*` (구현체), `{ctx}.adapter.out.*` |
| `{ctx}.application.service.*` | `{ctx}.application.port.in.*`, `{ctx}.application.port.out.*`, `{ctx}.domain.*`, 다른 `{other}.application.port.in.*` | `{ctx}.adapter.*`, 다른 `{other}.adapter.*`, 다른 `{other}.application.service.*` |
| `{ctx}.domain.*` | 같은 `{ctx}.domain.*`, `common.*` | Spring, JPA, `{ctx}.application.*`, `{ctx}.adapter.*` |
| `{ctx}.adapter.out.persistence.*` | `{ctx}.application.port.out.*`, `{ctx}.domain.model.*`, JPA / QueryDSL | `{ctx}.application.service.*`, `{ctx}.adapter.in.*` |
