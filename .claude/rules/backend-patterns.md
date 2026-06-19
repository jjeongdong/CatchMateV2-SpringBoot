---
trigger: glob
description: Backend Common Patterns (UseCase Flow, Fetch Port, Events, Outbox)
globs: "**/*.java"
---

# Backend Common Patterns

## 1. UseCase 흐름 패턴

Controller → Request → Command → UseCase 인터페이스 → ApplicationService 구현.

```java
// Controller
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardClientCommandUseCase boardClientCommandUseCase;   // 인터페이스만

    @PostMapping("/boards")
    public ResponseEntity<BoardCreateResponse> createBoard(
            @AuthUser Long userId,
            @RequestBody @Valid BoardCreateRequest request) {
        return ResponseEntity.ok(boardClientCommandUseCase.createBoard(userId, request.toCommand(userId)));
    }
}

// application/port/in
public interface BoardClientCommandUseCase {
    BoardCreateResponse createBoard(Long userId, BoardCreateCommand command);
}
public interface BoardClientQueryUseCase {
    BoardDetailResponse getBoard(Long userId, Long boardId);
}

// application/service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardClientCommandService implements BoardClientCommandUseCase {
    private final BoardRepository boardRepository;
    private final UserFetchPort userFetchPort;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        User user = userFetchPort.getUser(userId);
        Board board = boardRepository.save(Board.createBoard(user, command));
        publisher.publishEvent(BoardCreatedEvent.of(board));
        return BoardCreateResponse.from(board);
    }
}
```

## 2. Cross-context Fetch Port 패턴 (⭐)

다른 컨텍스트의 데이터/동작이 필요하면 자기 `application/port/out/` 에 인터페이스 정의 + `adapter/out/external/` 에 어댑터 구현.

```java
// 1. board/application/port/out/UserFetchPort.java
public interface UserFetchPort {
    User getUser(Long userId);
}

// 2. board/adapter/out/external/BoardUserFetchAdapter.java
@Component
@RequiredArgsConstructor
public class BoardUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;          // 다른 컨텍스트 의존은 여기 하나만
    @Override
    public User getUser(Long userId) {
        return userInternalQueryUseCase.getUser(userId);
    }
}

// 3. board/application/service/BoardClientCommandService.java
@Service
@RequiredArgsConstructor
public class BoardClientCommandService implements BoardClientCommandUseCase {
    private final UserFetchPort userFetchPort;     // 인터페이스에만 의존
    // ...
}
```

**왜 이 패턴인가?**
- BoardService 는 user 컨텍스트 구현체를 모름 → 결합도 낮음
- 나중에 user를 별도 서비스로 분리해도 어댑터만 HTTP 클라이언트로 바꾸면 끝
- 테스트 시 Fetch Port 만 Mock

**같은 컨텍스트 안의 협력자**(예: chat의 ChatRoomService, ChatMessageService)는 Fetch Port 거치지 않고 직접 호출 가능합니다.

## 3. 도메인 이벤트 패턴

이벤트 발행은 **Service** (UseCase 구현), 소비는 **`{ctx}/application/event/` 리스너**.

```java
// 이벤트 발행 (Service)
applicationEventPublisher.publishEvent(EnrollNotificationEvent.of(enroll, board, sender));

// 이벤트 소비 (Listener — 이중 단계)
@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {

    @EventListener  // Phase 1: 동기, 트랜잭션 내 — DB 저장
    public void saveNotification(EnrollNotificationEvent event) {
        notificationService.createNotification(...);
        notificationRetryService.saveOutbox(...);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleNotification(EnrollNotificationEvent event) {  // Phase 2: 비동기, 커밋 후
        notificationDispatchPort.dispatch(recipientId, payload);
    }
}
```

## 4. Transactional Outbox 패턴 (알림)

FCM 신뢰성 보장을 위한 Outbox.

```
[Service]
  → publishEvent(XxxNotificationEvent)
    → @EventListener (동기, 트랜잭션 내)
      → Notification + NotificationOutbox 저장 (PENDING)
    → @TransactionalEventListener AFTER_COMMIT (비동기)
      → NotificationDispatchPort.dispatch(...) — STOMP
      → OfflineFallbackPort.dispatchIfOffline(...) — FCM

[NotificationScheduler — 60초마다]
  → PENDING / FAILED Outbox 재시도
```

알림 Port:
- `NotificationDispatchPort.dispatch(Long, Map<String,String>)` — 실시간 fan-out (RedisPublisher)
- `OfflineFallbackPort.dispatchIfOffline(Long, NotificationOutbox)` — 오프라인 fallback (CompositeNotificationDispatcher → FcmNotificationSender)

## 5. AOP 권한 체크 패턴

`global/authorization/` 에 어노테이션 + Finder 세트.

```java
// 1. 어노테이션
@CheckDataPermission(finder = BoardPermissionFinder.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckBoardPermission {}

// 2. Finder
@Component
@RequiredArgsConstructor
public class BoardPermissionFinder implements DomainFinder {
    private final BoardService boardService;   // own context는 직접 OK
    @Override
    public ResourceOwnership find(Long resourceId) {
        return boardService.getBoard(resourceId);
    }
}

// 3. Controller
@DeleteMapping("/boards/{boardId}")
public ResponseEntity<Void> deleteBoard(
        @AuthUser Long userId,
        @CheckBoardPermission @PermissionId @PathVariable Long boardId) {
    boardUseCase.deleteBoard(userId, boardId);
    return ResponseEntity.noContent().build();
}
```

`DataPermissionAspect.@Before` 절에 새 어노테이션 추가도 필요.

## 6. QueryDSL 복잡 쿼리 패턴

단순 CRUD = JPA Spring Data, 복잡 조건 = QueryDSL Repository 분리.

```java
@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final JpaBoardRepository jpaBoardRepository;
    private final QueryDslBoardRepository queryDslBoardRepository;

    @Override
    public DomainPage<Board> findAllByCondition(BoardSearchCondition cond, DomainPageable pageable) {
        return queryDslBoardRepository.findAllByCondition(cond, pageable);
    }
}

// QueryDSL — BooleanExpression 조합
private BooleanExpression eqSportType(SportType sportType) {
    return sportType != null ? board.sportType.eq(sportType) : null;
}
```

## 7. Soft Delete 패턴

모든 엔티티는 `deletedAt` 컬럼으로 소프트 삭제.

```java
@Entity
@SQLRestriction("deleted_at IS NULL")  // 자동 필터
public class BoardEntity extends BaseEntity {
    private LocalDateTime deletedAt;
}
```

## 8. Redis Pub/Sub (실시간 채팅 / 알림)

멀티 인스턴스 지원을 위해 Redis Pub/Sub 으로 fan-out.

```
[ChatService]
  → publishEvent(ChatMessageEvent)
    → ChatMessageRedisPublisher.publishChat(...)   — AFTER_COMMIT @Async
      → 모든 인스턴스 RedisSubscriber.onMessage()
        → SimpMessagingTemplate.convertAndSendToUser()
```

`ChatMessageRedisPublisher` (채팅용) 와 `RedisPublisher` (알림용, `NotificationDispatchPort` 구현) 는 **의도적으로 분리**. 합치면 인터페이스 없는 메서드가 JDK 동적 프록시에서 사라져 Spring 부팅이 깨집니다.
