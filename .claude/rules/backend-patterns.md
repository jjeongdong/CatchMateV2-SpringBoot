---
trigger: glob
description: Backend Common Patterns (Orchestrator Flow, Events, Notification Outbox, Permission)
globs: "**/*.java"
---

# Backend Common Patterns

## 1. Orchestrator 흐름 패턴

Controller는 Request → Command 변환 후 Orchestrator에 위임합니다. Orchestrator가 유일한 트랜잭션 경계입니다.

```java
// Controller
@PostMapping
public ResponseEntity<BoardResponse> createBoard(
        @AuthUser Long userId,
        @RequestBody @Valid BoardCreateRequest request) {
    return ResponseEntity.ok(boardOrchestrator.createBoard(request.toCommand(userId)));
}

// Orchestrator
@Transactional
public BoardResponse createBoard(BoardCreateCommand command) {
    User user = userService.getUser(command.getUserId());
    Board board = boardService.createBoard(Board.createBoard(user, command));
    eventPublisher.publishEvent(SomeEvent.of(board));
    return BoardResponse.from(board);
}
```

## 2. 도메인 이벤트 패턴

이벤트 발행은 **Orchestrator**에서, 소비는 **Application 레이어 Listener**에서 합니다.

```java
// 이벤트 발행 (Orchestrator)
applicationEventPublisher.publishEvent(EnrollNotificationEvent.of(enroll, board, sender));

// 이벤트 소비 (Listener)
@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {

    // Phase 1: 트랜잭션 내 동기 실행 — DB 저장
    @EventListener
    public void saveNotification(EnrollNotificationEvent event) {
        notificationService.saveNotification(...);
        notificationRetryService.saveOutbox(...);
    }

    // Phase 2: 커밋 후 비동기 실행 — FCM 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleNotification(EnrollNotificationEvent event) {
        notificationRetryService.sendPendingOutboxImmediately(...);
    }
}
```

## 3. Transactional Outbox 패턴 (알림 신뢰성 보장)

FCM 발송 실패를 대비해 반드시 Outbox 패턴을 사용합니다.

```
[Orchestrator]
  → publishEvent(XxxNotificationEvent)
    → @EventListener (동기, 트랜잭션 내)
      → Notification 저장 (status=PENDING)
      → NotificationOutbox 저장 (status=PENDING)
    → @TransactionalEventListener AFTER_COMMIT (비동기)
      → 유저 온라인 시: 즉시 FCM 발송 시도
      → 오프라인 시: Outbox가 60초 후 스케줄러에서 처리

[NotificationScheduler - 60초마다]
  → PENDING 상태 Outbox 조회
  → FCM 발송 시도
  → 성공 시 SUCCESS, 실패 시 retry 카운트 증가 → max 초과 시 FAILED
```

새 알림 타입 추가 시:
1. `AlarmType` enum에 타입 추가 (`catchmate-common`)
2. `NotificationTemplate` enum에 메시지 템플릿 추가 (`catchmate-domain`)
3. 이벤트 클래스 생성 (`catchmate-application`)
4. 이벤트 리스너 생성 (위 패턴 참고)
5. Orchestrator에서 이벤트 발행

## 4. AOP 권한 체크 패턴

새 권한 체크 추가 시 `catchmate-authorization` 모듈에 어노테이션 + Aspect + Finder를 세트로 만듭니다.

```java
// 1. 어노테이션 정의
@CheckDataPermission(finder = BoardPermissionFinder.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckBoardPermission {}

// 2. Finder 구현
@Component
public class BoardPermissionFinder implements DomainFinder {
    @Override
    public ResourceOwnership find(Long resourceId) {
        return boardService.getBoard(resourceId);
    }
}

// 3. Controller에서 사용
@DeleteMapping("/{boardId}")
public ResponseEntity<Void> deleteBoard(
        @AuthUser Long userId,
        @CheckBoardPermission @PermissionId @PathVariable Long boardId) {
    boardOrchestrator.deleteBoard(userId, boardId);
    return ResponseEntity.noContent().build();
}
```

## 5. QueryDSL 복잡 쿼리 패턴

단순 CRUD는 `JpaXxxRepository` (Spring Data JPA), 복잡 조건 조회는 `QueryDslXxxRepository`로 분리합니다.

```java
// Repository 구현체에서 위임
@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final JpaBoardRepository jpaBoardRepository;
    private final QueryDslBoardRepository queryDslBoardRepository;

    @Override
    public DomainPage<Board> findAllByCondition(BoardSearchCondition condition, DomainPageable pageable) {
        return queryDslBoardRepository.findAllByCondition(condition, pageable);
    }
}

// QueryDSL에서 BooleanExpression으로 조건 조합
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

- `@SQLRestriction`으로 모든 조회에서 자동 필터링됩니다.
- 삭제 메서드는 `deletedAt = LocalDateTime.now()` 설정 후 저장합니다.

## 7. Redis Pub/Sub (실시간 채팅)

WebSocket 메시지는 여러 서버 인스턴스를 지원하기 위해 Redis Pub/Sub으로 브로드캐스트합니다.

```
[ChatOrchestrator]
  → publishEvent(ChatMessageEvent)
    → RedisPublisher.publish(channel, message)
      → 모든 서버 인스턴스의 RedisSubscriber.onMessage()
        → SimpMessagingTemplate.convertAndSendToUser()
```

채널 네이밍: `/topic/chatroom/{chatRoomId}`
