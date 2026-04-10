---
trigger: glob
description: Backend Coding Conventions (Naming, Error Handling, Entity Mapping, Transaction)
globs: "**/*.java"
---

# Backend Coding Conventions

## 1. 네이밍 규칙

### 클래스 네이밍
| 유형 | 패턴 | 예시 |
|:---|:---|:---|
| Controller | `{Domain}Controller` | `BoardController` |
| Orchestrator | `{Domain}Orchestrator` | `BoardOrchestrator` |
| Service | `{Domain}Service` | `BoardService` |
| Event | `{Domain}{Action}Event` | `EnrollNotificationEvent` |
| Event Listener | `{Domain}{Action}EventListener` | `EnrollNotificationEventListener` |
| Command | `{Domain}{Action}Command` | `BoardCreateCommand` |
| Response | `{Domain}{Detail?}Response` | `BoardDetailResponse` |
| Repository (Port) | `{Domain}Repository` | `BoardRepository` |
| Repository (Impl) | `{Domain}RepositoryImpl` | `BoardRepositoryImpl` |
| JPA Repository | `Jpa{Domain}Repository` | `JpaBoardRepository` |
| QueryDSL Repository | `QueryDsl{Domain}Repository` | `QueryDslBoardRepository` |
| JPA Entity | `{Domain}Entity` | `BoardEntity` |

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

- **모든 예외**는 `BaseException`을 사용하고 `ErrorCode` enum으로 분류합니다.
- 절대로 `catch (Exception ignored) {}` 또는 빈 catch 블록을 작성하지 않습니다.
- 예상된 null 케이스는 `Optional`로 처리합니다.

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

// ❌ Bad — 예외 삼키기
try {
    status = enrollService.getEnroll(id).getAcceptStatus();
} catch (Exception ignored) {}
```

## 3. Entity ↔ Domain 모델 변환

- JPA Entity와 Domain 모델을 **분리**합니다.
- 변환 메서드: `Entity.toModel()` (Entity → Domain), `Entity.from(domain)` (Domain → Entity)
- 변환 로직은 **Entity 클래스 내부**에 위치합니다.

```java
// ✅ Entity 내부에 변환 메서드
@Entity
public class BoardEntity extends BaseEntity {

    public Board toModel() {
        return Board.builder()
                .id(this.id)
                .title(this.title)
                .build();
    }

    public static BoardEntity from(Board board) {
        BoardEntity entity = new BoardEntity();
        entity.title = board.getTitle();
        return entity;
    }
}

// ✅ Repository 구현체에서 변환
@Override
public Optional<Board> findById(Long id) {
    return jpaBoardRepository.findById(id)
            .map(BoardEntity::toModel);
}
```

## 4. 트랜잭션 경계

- `@Transactional(readOnly = true)`를 Orchestrator 클래스 레벨에 기본 설정합니다.
- 쓰기 작업은 해당 메서드에 `@Transactional`을 오버라이드합니다.
- `Propagation.REQUIRES_NEW`는 반드시 **별도 Bean**에서 호출해야 합니다 (같은 클래스 내 호출 시 프록시 무시됨).

```java
// ✅ Orchestrator
@Transactional(readOnly = true)
public class BoardOrchestrator {

    public BoardDetailResponse getBoard(Long id) { ... }  // readOnly 상속

    @Transactional  // 쓰기는 오버라이드
    public BoardResponse createBoard(BoardCreateCommand command) { ... }
}

// ✅ REQUIRES_NEW는 별도 Bean
@Service
public class NotificationOutboxUpdater {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusSuccess(Long outboxId) { ... }
}
```

## 5. Null 처리

- 메서드 파라미터와 반환값에 대한 null 여부를 명확히 설계합니다.
- 도메인 모델의 boolean 속성은 `Character Y/N` 대신 래핑 메서드로 접근합니다.
- 직접적인 `char` 비교(`!= 'Y'`)를 서비스/리스너 코드에 노출하지 않습니다.

```java
// ✅ Domain 모델에서 의도를 드러내는 메서드 제공
public boolean isEnrollAlarmEnabled() { return 'Y' == this.enrollAlarm; }

// ✅ 사용 측은 의미 있는 메서드 호출
if (!recipient.isEnrollAlarmEnabled()) return;

// ❌ Bad — 구현 상세 노출
if (recipient.getEnrollAlarm() != 'Y') return;
```

## 6. 하드코딩 금지

Magic number와 문자열 상수는 코드에 직접 작성하지 않습니다.

```java
// ❌ Bad
@Scheduled(fixedDelay = 60000)
private static final int MAX_RETRY = 5;

// ✅ Good — application.yml + @Value
@Value("${notification.outbox.max-retry-count:5}")
private int maxRetryCount;

@Scheduled(fixedDelayString = "${notification.outbox.scheduler-delay-ms:60000}")
public void processPendingPush() { ... }
```
