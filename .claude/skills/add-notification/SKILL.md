---
name: add-notification
description: >-
  catchmate 백엔드에 새 푸시/실시간 알림을 Transactional Outbox + 2단계 이벤트 리스너
  패턴으로 추가한다. 다음 상황에 사용한다 — "X 알림 추가해줘", "푸시 알림 보내야 해",
  "FCM 알림", "특정 동작이 일어나면 알림", 새 NotificationTemplate/이벤트 리스너 추가,
  알림 발송 흐름 작업. @EventListener(커밋 전 Outbox 저장) + @TransactionalEventListener
  (AFTER_COMMIT FCM 발송) + NotificationScheduler(재시도) 의 의도적 분리를 절대 합치지 않고
  보존한다. (CLAUDE.md "절대 변경 금지" 1순위 패턴)
---

# Add Notification (Transactional Outbox + 2단계 이벤트)

새 알림 = **"소스 컨텍스트에서 이벤트 발행 → notification 컨텍스트가 저장/발송"** 흐름을 추가하는 것.
아래 구조를 **절대 단순화하지 않는다**. 합치면 롤백된 데이터로 FCM 이 나가거나 Spring 부팅이 깨진다.

```
[소스 컨텍스트 service @Transactional]
   publishEvent({Source}XxxEvent)                         ← 비즈니스 트랜잭션 안에서 발행
        │
        ├─ @EventListener (동기, 커밋 '전')               → save: Notification + Outbox row 저장
        │     {Source}NotificationUseCase.saveOnXxx()        (Transactional Outbox: 비즈니스 tx 와 원자적)
        │
        └─ @Async + @TransactionalEventListener(AFTER_COMMIT) → dispatch: STOMP + FCM 즉시 시도
              {Source}NotificationDispatchUseCase.dispatchOnXxx()  (커밋 후에만 발송)

[NotificationScheduler] 60초마다 PENDING Outbox 재시도   ← 제네릭. 새 알림 추가 시 손대지 않음.
```

## 불변 규칙 (절대 변경 금지)

1. **저장은 `@EventListener`** (동기, 원본 트랜잭션 안, 커밋 전). async/after-commit 으로 바꾸지 않는다 → Outbox 가 비즈니스 데이터와 원자적으로 커밋되어야 함.
2. **발송은 `@Async("notificationDispatchExecutor") @TransactionalEventListener(AFTER_COMMIT)`**. 커밋 후에만 FCM → 롤백된 데이터로 푸시 방지. async → FCM 지연(수백 ms)이 호출자를 막지 않음.
3. **저장 service = `@Transactional`, 발송 service = 트랜잭션 없음** (Outbox claim/update 는 `OutboxStateTransitioner` 의 REQUIRES_NEW 짧은 tx 가 담당). 발송 service 에 `@Transactional` 붙이지 않는다.
4. 저장과 발송을 한 리스너/한 메서드로 **합치지 않는다**.
5. `NotificationScheduler`, `OutboxSaver/Dispatcher/StateTransitioner`, `RedisPublisher`/`ChatMessageRedisPublisher` 분리는 건드리지 않는다.

---

## 작업 절차

### 1. 템플릿 추가 — `notification/domain/model/NotificationTemplate.java`
enum 상수 추가. 동적 값은 `%s` (`formatTitle`/`formatBody` 로 바인딩).
```java
XXX_HAPPENED("%s님이 ...", "'%s' 에 ... 안내입니다."),
```
필요하면 `notification/domain/model/AlarmType` 에 알맞은 타입 재사용 또는 신규 상수 추가.

### 2. 도메인 이벤트 — `{source}/application/event/{Source}XxxEvent.java`
원시 식별자만 담는 record + static 팩토리. (다른 컨텍스트 도메인/enum 노출 금지 = 0-import)
```java
public record {Source}XxxEvent(Long referenceId, Long boardId, Long actorId, Long recipientId) {
    public static {Source}XxxEvent of(Long referenceId, Long boardId, Long actorId, Long recipientId) {
        return new {Source}XxxEvent(referenceId, boardId, actorId, recipientId);
    }
}
```

### 3. 이벤트 발행 — 소스의 `{Source}ClientCommandService` (`@Transactional` 안)
```java
private final ApplicationEventPublisher applicationEventPublisher;
// ... 비즈니스 로직으로 엔티티 저장 후, 같은 트랜잭션 안에서:
applicationEventPublisher.publishEvent({Source}XxxEvent.of(saved.getId(), boardId, actorId, recipientId));
```

### 4. 저장측 — UseCase + Service (notification 컨텍스트, `@Transactional`)
`notification/application/port/in/{Source}NotificationUseCase.java`
```java
public interface {Source}NotificationUseCase {
    void saveOnXxx(Long referenceId, Long boardId, Long actorId, Long recipientId);
}
```
`notification/application/service/{Source}NotificationService.java`
```java
@Slf4j @Service @Transactional @RequiredArgsConstructor
public class {Source}NotificationService implements {Source}NotificationUseCase {
    private final UserFetchPort userFetchPort;        // 수신자 fcmToken/alarmEnabled 조회
    private final BoardFetchPort boardFetchPort;      // 필요 시
    private final OutboxSaveUseCase outboxSaveUseCase;
    private final NotificationInternalCommandUseCase notificationInternalCommandUseCase;

    @Override
    public void saveOnXxx(Long referenceId, Long boardId, Long actorId, Long recipientId) {
        String title = NotificationTemplate.XXX_HAPPENED.formatTitle(/* 동적값 */);
        String body  = NotificationTemplate.XXX_HAPPENED.formatBody(/* 동적값 */);

        // (1) 인앱 알림 항상 저장
        notificationInternalCommandUseCase.createNotification(
                recipientId, actorId, boardId, title, AlarmType.XXX, referenceId);

        // (2) 수신자 설정·토큰 확인 후 Outbox 저장 (커밋 전 — Transactional Outbox)
        NotificationUserInfo recipient = userFetchPort.getUser(recipientId);
        if (recipient.xxxAlarmEnabled() && recipient.fcmToken() != null) {
            outboxSaveUseCase.saveOutbox(recipient.userId(), recipient.fcmToken(), title, body,
                    Map.of("type", "XXX", "boardId", String.valueOf(boardId), "title", title, "body", body));
        }
    }
}
```

### 5. 발송측 — UseCase + Service (notification 컨텍스트, **트랜잭션 없음**)
`notification/application/port/in/{Source}NotificationDispatchUseCase.java`
```java
public interface {Source}NotificationDispatchUseCase {
    void dispatchOnXxx(Long referenceId, Long boardId, Long actorId, Long recipientId);
}
```
`notification/application/service/{Source}NotificationDispatchService.java` — **@Transactional 금지**
```java
@Slf4j @Service @RequiredArgsConstructor      // ← 클래스 레벨 트랜잭션 없음 (의도적)
public class {Source}NotificationDispatchService implements {Source}NotificationDispatchUseCase {
    private final UserFetchPort userFetchPort;
    private final BoardFetchPort boardFetchPort;          // 필요 시
    private final OutboxDispatchUseCase outboxDispatchUseCase;
    private final NotificationDispatchUseCase notificationDispatchUseCase;

    @Override
    public void dispatchOnXxx(Long referenceId, Long boardId, Long actorId, Long recipientId) {
        NotificationUserInfo recipient = userFetchPort.getUser(recipientId);
        if (!recipient.xxxAlarmEnabled()) return;        // 수신 거부 시 발송 중단

        String title = NotificationTemplate.XXX_HAPPENED.formatTitle(/* 동적값 */);
        String body  = NotificationTemplate.XXX_HAPPENED.formatBody(/* 동적값 */);
        Map<String, String> payload = Map.of("type", "XXX", "boardId", String.valueOf(boardId),
                "title", title, "body", body);

        notificationDispatchUseCase.dispatch(recipient.userId(), payload);          // STOMP 실시간
        outboxDispatchUseCase.sendPendingOutboxImmediately(recipient.userId());     // FCM 즉시 시도
    }
}
```

### 6. 2단계 리스너 — `notification/adapter/in/event/{Source}NotificationEventListener.java`
한 리스너 클래스에 저장(@EventListener) + 발송(@Async @TransactionalEventListener) 메서드를 **둘 다** 둔다.
```java
@Component @RequiredArgsConstructor
public class {Source}NotificationEventListener {
    private final {Source}NotificationUseCase {source}NotificationUseCase;
    private final {Source}NotificationDispatchUseCase {source}NotificationDispatchUseCase;

    @EventListener                                                       // 커밋 '전' 저장
    public void onSaveXxx({Source}XxxEvent e) {
        {source}NotificationUseCase.saveOnXxx(e.referenceId(), e.boardId(), e.actorId(), e.recipientId());
    }

    @Async("notificationDispatchExecutor")                              // 커밋 '후' 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchXxx({Source}XxxEvent e) {
        {source}NotificationDispatchUseCase.dispatchOnXxx(e.referenceId(), e.boardId(), e.actorId(), e.recipientId());
    }
}
```

### 7. Cross-context 데이터
저장/발송에서 user(`NotificationUserInfo`: fcmToken, alarmEnabled)·board(`NotificationBoardInfo`) 정보는
notification 의 기존 `UserFetchPort`/`BoardFetchPort` 로 가져온다. 새 소스 정보가 필요하면 Fetch Port 를
추가한다 → `cross-context-access` 스킬 참고. (상대 도메인/Entity 직접 import 금지)

---

## 완료 검증 (필수)
1. 저장 메서드는 `@EventListener`, 발송 메서드는 `@Async("notificationDispatchExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)` 인지 확인.
2. 발송 service 에 `@Transactional` 이 **없는지** 확인.
3. 스케줄러/Outbox 헬퍼/RedisPublisher 를 건드리지 않았는지 확인.
4. `./gradlew archCheck` 통과 (이벤트 record·리스너의 0-import 검출).

## 실제 레퍼런스 (이 레포에 존재)
- 템플릿: `notification/domain/model/NotificationTemplate`
- 이벤트: `enroll/application/event/EnrollRequestedEvent`
- 발행: `enroll/application/service/EnrollClientCommandService`
- 저장: `notification/application/service/EnrollNotificationService` (@Transactional)
- 발송: `notification/application/service/EnrollNotificationDispatchService` (비트랜잭션)
- 리스너: `notification/adapter/in/event/EnrollNotificationEventListener`
- 재시도(무수정): `global/scheduler/NotificationScheduler`

상세 패턴 SSOT: `.claude/ondemand-rules/backend-patterns.md` (이벤트 2단계·Outbox).
