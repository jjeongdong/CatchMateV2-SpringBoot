---
trigger: glob
description: Backend Common Patterns (Events, Outbox, AOP, QueryDSL, Redis)
globs: "**/*.java"
---

# Backend Common Patterns

> 이벤트 / Outbox / AOP / QueryDSL / Redis 패턴의 **단일 출처(SSOT)**. cross-context 동기 호출(Fetch Port) 상세는 `backend-architecture.md` 참조.

## 이벤트 패키지 & 의존성 규칙 (⭐⭐)

이벤트는 컨텍스트 간 **유일하게 허용되는 비동기 정문**. UseCase 가 동기 published contract 라면 이벤트 클래스는 비동기 published contract.

### 1. 이벤트 3종류 구분

| 종류 | 목적 | 위치 |
|:---|:---|:---|
| Domain Event | "도메인에서 X 가 일어났다"는 사실 (Aggregate 가 raise) | `{ctx}/domain/event/` |
| Application Event | 한 컨텍스트 내부 비동기 후처리 트리거 | `{ctx}/application/event/` |
| Integration Event | 컨텍스트 경계를 넘는 알림 (Cross-context) | `{ctx}/application/event/` (publisher 소유) |

### 2. 소유권은 publisher, 구독은 subscriber 에

```
publisher:  {publisher-ctx}/application/event/{Fact}Event.java
subscriber: {subscriber-ctx}/adapter/in/event/{Subscriber}{Fact}EventListener.java
```

의존 방향: ✅ subscriber → publisher 의 Event 클래스 / ❌ publisher → subscriber 의 무엇이든.
리스너는 `adapter/in/event/` 에 (이벤트 record 와 같은 디렉토리에 두지 말 것).

### 3. 허용되는 cross-context import 는 딱 2가지

| import | 허용? | 이유 |
|:---|:---:|:---|
| `other.application.port.in.XxxUseCase` | ✅ | 동기 정문 |
| `other.application.event.XxxEvent` | ✅ | 비동기 정문 |
| `other.domain.model.*` / `other.application.service.*` / `other.adapter.*` | ❌ | 내부 구현 |

0-import 정책의 진짜 의미 = **"다른 컨텍스트의 도메인/내부 구현 import 0건"** (모든 cross-context import 0건이 아님).

### 4. 이벤트 = published contract 답게 (페이로드 경량화)

이벤트 record 의 import·필드는 **자기 컨텍스트 + `common` + JDK** 만. 담을 수 있는 것: 식별자(`Long userId`), 식별자 리스트(`List<Long>`), 자기 컨텍스트 primitive(`String noticeTitle`). 넣지 말 것: 다른 컨텍스트 도메인 모델/enum/템플릿, `List<User>`(→ `List<Long>`).

```java
// ✅ 공개 계약다운 이벤트
public record NoticeCreatedEvent(Long noticeId, String noticeTitle) {}

// ❌ 위장된 도메인 노출 — subscriber 가 import 하는 순간 결합 전염
public record NoticeCreatedEvent(Notice notice, NoticeStatus status, List<NoticeItem> items) {}
```

### 5. 발행 컨텍스트는 비즈니스 사실만 (구독자 관심사 금지) ⭐

이벤트는 "무슨 일이 일어났다"만. "누구한테 어떻게 알릴지"는 구독자 책임.

```java
// ❌ publisher 가 알림 대상자 결정 (notification 책임 누수)
public record NoticeCreatedEvent(Long noticeId, String title, List<Long> recipientIds) {}

// ✅ publisher 는 사실만
public record NoticeCreatedEvent(Long noticeId, String noticeTitle) {}

// 구독자가 자기 책임으로 대상자 조회
@Component
public class NotificationNoticeCreatedEventListener {
    @EventListener
    public void onNoticeCreated(NoticeCreatedEvent event) {
        List<NotificationUserInfo> recipients = userFetchPort.getEventAlarmEnabledUsers(); // 알림 책임
        String title = NotificationTemplate.NOTICE_CREATED.getTitle();                     // 템플릿은 소유 모듈에서
        ...
    }
}
```

판단 기준: 페이로드 필드가 publisher 의 비즈니스 사실인가? `noticeId`·`noticeTitle` ✅ / `recipientIds`·`fcmTokens` ❌.

### 6. 트랜잭션 경계에 따른 2단계 리스너

```java
@EventListener                                                  // DB 쓰기/제약 검증 — 같은 트랜잭션
public void onSomethingHappened(SomethingHappenedEvent e) { ... }

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 외부 호출(FCM/메일/API)
@Async("taskExecutor")
public void onSomethingHappenedAsync(SomethingHappenedEvent e) { ... }
```

`BEFORE_COMMIT` 에 외부 호출 금지. 외부 호출 at-least-once 신뢰성은 Outbox 로 보강.

### 7. 리스너는 얇은 어댑터 — 비즈니스 로직 금지

```java
@EventListener
public void on(OrderPlacedEvent e) {
    pointInternalCommandUseCase.grantPoints(new GrantPointsCommand(e.buyerId(), ...)); // 트리거 → UseCase
}
```

### 8. 동기 호출 vs 이벤트

| 상황 | 선택 |
|:---|:---|
| 결과가 비즈니스 흐름의 일부 (실패하면 본흐름 취소) | 동기 호출 (FetchPort / CommandPort) |
| 부수효과·알림·분석·캐시 무효화 (실패해도 본흐름 성공) | 이벤트 |

---

## Transactional Outbox (알림) — ⚠️ 단순화 금지

1. `@EventListener` (커밋 전, 동기): `Notification` + `NotificationOutbox` 저장 (PENDING)
2. `@TransactionalEventListener(AFTER_COMMIT)` (커밋 후, 비동기): 실시간 STOMP + FCM 시도
3. `NotificationScheduler` (60초마다): `PENDING` / `FAILED` Outbox 재시도

알림 채널 분리:
- `NotificationDispatchPort.dispatch(Long, Map)` — 실시간 STOMP fan-out (RedisPublisher 구현)
- `OfflineFallbackPort.dispatchIfOffline(Long, NotificationOutbox)` — 오프라인 사용자 FCM


## QueryDSL 복잡 쿼리

단순 CRUD = Spring Data JPA, 복잡 조건 = QueryDSL Repository 분리. `BooleanExpression` 조합으로 동적 조건.

```java
private BooleanExpression eqSportType(SportType sportType) {
    return sportType != null ? board.sportType.eq(sportType) : null; // null 이면 조건 무시
}
```

## Soft Delete (엔티티 성격별 — "모든 엔티티" 아님)

soft-delete 는 **핵심 도메인 애그리거트에만** 적용한다. 조인/토글·토큰·아웃박스 엔티티는 물리 삭제가 정상이며, 오히려 soft-delete 가 유니크 제약 붕괴·보안 취약·무한 증가를 유발한다.

| 분류 | 엔티티 | 삭제 방식 |
|:---|:---|:---|
| 핵심 애그리거트 | `User`, `Board`, `ChatRoom`, `ChatMessage` | **soft** — `deletedAt`+`@SQLRestriction`, 도메인 `delete()`(deletedAt 세팅)→`save()` |
| 조인/토글 | `Bookmark`, `Block`, `Enroll`, `ChatRoomMember` | **hard** — 재생성(재북마크/재신청) 시 유니크 제약 충돌 방지 |
| 토큰/보안 | `RefreshToken` | **hard** — 폐기 토큰 잔존 금지 |
| 운영/아웃박스 | `NotificationOutbox`, `Notification` | **hard** — 고볼륨, 처리 후 소멸 |
| 참조/마스터 | `Game`, `Club`, `Notice`, `Report`, `Inquiry` | 삭제 거의 없음 (필요 시 개별 판단) |

규칙:
- **soft-delete 엔티티(@SQLRestriction 보유)에는 `deleteById`/`delete from` 등 물리 삭제 금지.** 반드시 도메인 `delete()`+`save()`.
- 프로젝트에 `@SQLDelete` 는 쓰지 않는다 — soft-delete 는 도메인 `delete()` 가 `deletedAt` 을 세팅하고 `save()` 로 UPDATE 하는 수동 방식.
- 새 엔티티 추가 시 성격으로 분류부터: 핵심 애그리거트면 soft, 조인/토큰/아웃박스면 hard.
- **명시적 예외**: soft-delete 엔티티라도 *미완성·일회성 하위 상태*는 물리 삭제 가능. 예) `Board` 의 임시저장(`completed=false`, draft) 폐기는 `BoardRepository.deleteTempBoard` 가 물리 삭제 — draft 는 임시저장마다 교체돼 고빈도라 soft-delete 시 dead row 누적. 완성 게시글 삭제는 soft. 같은 엔티티라도 **상태(완성 vs draft)로 삭제 정책이 갈린다.**
- 의도된 물리삭제 예외는 그 줄에 `// arch-audit:allow-hard-delete` 마커를 달아 자기문서화 + 감사 제외한다.
- 검증: `python3 .claude/hooks/arch-audit.py [컨텍스트]` 의 `[F]` 가 soft-delete 엔티티의 (마커 없는) 물리삭제만 잡는다.

## Redis Pub/Sub — 두 Publisher 의도적 분리 (⚠️ 합치지 말 것)

- `RedisPublisher` — 알림용, `NotificationDispatchPort` 구현
- `ChatMessageRedisPublisher` — 채팅용, `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`

둘을 합치면 인터페이스 없는 메서드가 JDK 동적 프록시에서 사라져 Spring 부팅이 깨집니다.

---

## 알림 타입 추가 순서

1. `notification/domain/enums/AlarmType` 에 enum
2. `notification/domain/model/NotificationTemplate` 에 템플릿
3. `{publisher-ctx}/application/event/{Fact}Event` — 발행 컨텍스트 소유, 페이로드는 식별자 + primitive 만
4. `notification/adapter/in/event/{Publisher}{Fact}EventListener` — 구독자(notification)에 위치, 2단계 패턴, 대상자 조회는 여기서
5. 발행 컨텍스트 Service 에서 `applicationEventPublisher.publishEvent(...)` — 사실만 발행
