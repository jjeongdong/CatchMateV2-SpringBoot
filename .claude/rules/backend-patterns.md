---
trigger: glob
description: Backend Common Patterns (UseCase Flow, Events, Notification Outbox, Permission)
globs: "**/*.java"
---

# Backend Common Patterns

## 1. UseCase 흐름 패턴

Controller 는 Request → Command 변환 후 **UseCase 인터페이스**에 위임합니다. ApplicationService 가 유일한 트랜잭션 경계입니다.

```java
// Controller
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardUseCase boardUseCase;   // ⭐ 인터페이스만 의존

    @PostMapping("/boards")
    public ResponseEntity<BoardCreateResponse> createBoard(
            @AuthUser Long userId,
            @RequestBody @Valid BoardCreateRequest request) {
        return ResponseEntity.ok(boardUseCase.createBoard(userId, request.toCommand(userId)));
    }
}

// application/port/in
public interface BoardUseCase {
    BoardCreateResponse createBoard(Long userId, BoardCreateCommand command);
    BoardDetailResponse getBoard(Long userId, Long boardId);
}

// application/service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardApplicationService implements BoardUseCase {
    private final BoardService boardService;
    private final UserUseCase userUseCase;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        User user = userUseCase.getUser(command.getUserId());
        Board board = boardService.createBoard(Board.createBoard(user, command));
        publisher.publishEvent(BoardCreatedEvent.of(board));
        return BoardCreateResponse.from(board);
    }
}
```

## 2. 도메인 이벤트 패턴

이벤트 발행은 **ApplicationService**, 소비는 **`{ctx}/application/event/` 리스너**.

```java
// 이벤트 발행 (ApplicationService)
applicationEventPublisher.publishEvent(EnrollNotificationEvent.of(enroll, board, sender));

// 이벤트 소비 (Listener — 이중 단계)
@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {

    // Phase 1: 트랜잭션 내 동기 실행 — DB 저장
    @EventListener
    public void saveNotification(EnrollNotificationEvent event) {
        notificationService.saveNotification(...);
        notificationRetryService.saveOutbox(...);
    }

    // Phase 2: 커밋 후 비동기 실행 — 실시간 STOMP 전송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleNotification(EnrollNotificationEvent event) {
        notificationDispatchPort.dispatch(recipientId, payload);
    }
}
```

## 3. Transactional Outbox 패턴 (알림 신뢰성)

FCM 발송 실패에 대비해 반드시 Outbox 패턴을 사용합니다.

```
[ApplicationService]
  → publishEvent(XxxNotificationEvent)
    → @EventListener (동기, 트랜잭션 내)
      → Notification 저장 (status=PENDING)
      → NotificationOutbox 저장 (status=PENDING)
    → @TransactionalEventListener AFTER_COMMIT (비동기)
      → NotificationDispatchPort.dispatch(...) — Redis Pub/Sub → STOMP
      → OfflineFallbackPort.dispatchIfOffline(...) — 오프라인 시 FCM

[NotificationScheduler — 60초마다]
  → PENDING / FAILED Outbox 재시도 (max-retry-count 초과 시 FAILED 종료)
```

알림 채널 Port:
- `NotificationDispatchPort.dispatch(Long userId, Map<String,String> payload)` — 실시간 fan-out (RedisPublisher 구현)
- `OfflineFallbackPort.dispatchIfOffline(Long userId, NotificationOutbox outbox)` — 오프라인 fallback (CompositeNotificationDispatcher → FcmNotificationSender)

새 알림 타입 추가:
1. `notification/domain/enums/AlarmType` enum 추가
2. `notification/domain/model/NotificationTemplate` 에 템플릿 추가
3. `{ctx}/application/event/Xxx{Notification}Event` 생성
4. `{ctx}/application/event/Xxx{Notification}EventListener` 생성 — 이중 단계 패턴 준수
5. 해당 컨텍스트 ApplicationService 에서 이벤트 발행

## 4. AOP 권한 체크 패턴

`global/authorization/` 에 어노테이션 + Finder 를 세트로 만듭니다.

```java
// 1. 어노테이션 (global/authorization/annotation)
@CheckDataPermission(finder = BoardPermissionFinder.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckBoardPermission {}

// 2. Finder (global/authorization/finder)
@Component
@RequiredArgsConstructor
public class BoardPermissionFinder implements DomainFinder {
    private final BoardUseCase boardUseCase;
    @Override
    public ResourceOwnership find(Long resourceId) {
        return boardUseCase.getBoard(null, resourceId);
    }
}

// 3. Controller 사용
@DeleteMapping("/boards/{boardId}")
public ResponseEntity<Void> deleteBoard(
        @AuthUser Long userId,
        @CheckBoardPermission @PermissionId @PathVariable Long boardId) {
    boardUseCase.deleteBoard(userId, boardId);
    return ResponseEntity.noContent().build();
}
```

`DataPermissionAspect` 의 `@Before` 절에 새 어노테이션도 추가합니다.

## 5. QueryDSL 복잡 쿼리 패턴

단순 CRUD 는 `JpaXxxRepository` (Spring Data JPA), 복잡 조건은 `QueryDslXxxRepository` 로 분리합니다.

```java
// adapter/out/persistence/repository/BoardRepositoryImpl.java
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

## 6. Soft Delete 패턴

모든 엔티티는 물리 삭제 대신 `deletedAt` 컬럼으로 소프트 삭제합니다.

```java
@Entity
@SQLRestriction("deleted_at IS NULL")  // 자동으로 삭제 필터 적용
public class BoardEntity extends BaseEntity {
    private LocalDateTime deletedAt;
}
```

- `@SQLRestriction` 으로 모든 조회에서 자동 필터링
- 삭제 메서드는 `deletedAt = LocalDateTime.now()` 설정 후 저장

## 7. Redis Pub/Sub (실시간 채팅 / 알림)

WebSocket / 알림 메시지는 멀티 인스턴스 지원을 위해 Redis Pub/Sub 으로 브로드캐스트.

```
[ChatApplicationService]
  → publishEvent(ChatMessageEvent)
    → RedisPublisher.publishChat(...) — AFTER_COMMIT @Async
      → 모든 인스턴스의 RedisSubscriber.onMessage()
        → SimpMessagingTemplate.convertAndSendToUser()
```

`global/redis/RedisPublisher` 가 `NotificationDispatchPort` 의 구현체이기도 합니다.

채널 네이밍: `/topic/chatroom/{chatRoomId}`
