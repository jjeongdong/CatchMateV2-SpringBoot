---
name: concurrency-review
description: >-
  catchmate 백엔드를 동시성·트랜잭션 관점에서 리뷰한다 — 트랜잭션 경계와 커밋 타이밍, 이벤트 리스너
  phase(BEFORE_COMMIT / AFTER_COMMIT + REQUIRES_NEW)와 알림 Outbox, 스프링 프록시 self-invocation 함정,
  @Async 스레드풀, Redis Pub/Sub 브로드캐스트의 중복·유실, 멱등성 저장소·락, 다중 인스턴스에서 도는
  스케줄러, Redis write-behind 버퍼 flush, STOMP 세션 생명주기. 다음 상황에 사용한다 — "동시성 리뷰",
  "트랜잭션 경계 봐줘", "레이스 있나", "커밋 전에 발송되는 거 아냐", 이벤트 리스너·스케줄러·Redis·
  WebSocket 코드를 고친 뒤. 전방위 리뷰는 review-board 오케스트레이터가 호출한다.
---

# Concurrency Review (실행 시점·순서·원자성)

이 스킬은 **"언제 실행되고, 그때 무엇이 보이는가"** 만 본다. 구조·계약은 `architecture-review`,
쿼리 비용은 `persistence-perf-review` 담당이다.

이 축의 결함은 **테스트가 통과해도 운영에서만 터진다.** 그래서 판정 기준은 "규칙 문서와 다른가"가
아니라 **"어떤 순서로 실행되면 깨지는지 시나리오를 쓸 수 있는가"** 다. 시나리오를 못 쓰면 위반이
아니라 "확인 필요"다.

## 0. 스코프 잡기
1. 오케스트레이터가 준 스코프를 그대로 쓴다. 없으면 `git diff --name-only`(+ staged, 없으면 `main...HEAD`).
2. **호출 경로를 역추적한다.** 변경된 Service 가 발행하는 이벤트를 누가 어떤 phase 로 구독하는지,
   그 리스너가 무엇을 호출하는지 Grep 으로 끝까지 따라간다. 이 축은 파일 하나로 판정되지 않는다.
3. 이미 측정·튜닝된 경로면 `docs/*bench*.md`, `docs/*loadtest*.md` 의 결론과 어긋나는 제안을 하지 않는다.

## 이 프로젝트의 현재 구조 (판정 기준선)
- **기본 리스너**: `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`.
  수신 측 실패가 발행 측을 롤백하지 않는다.
- **알림 Outbox 3단계**: `Notification*Listener` 가 `BEFORE_COMMIT` 에서 같은 트랜잭션으로 outbox 저장
  → `AFTER_COMMIT` + `@Async("notificationDispatchExecutor")` 로 `OutboxDispatcher` 발송
  → `NotificationScheduler` 가 재시도·복구. 선점은 `NotificationOutboxJpaRepository` 의
  `PESSIMISTIC_WRITE` + SKIP LOCKED, 상태 전이는 `OutboxStateTransitioner`(`REQUIRES_NEW`).
- **BEFORE_COMMIT 선례**: `BoardEnrollAcceptedListener`(수락과 인원 증가 원자성), `EnrollAcceptExecutor` 주석 참조.
- **Redis**: 채팅 브로드캐스트 `RedisChatMessageBroadcaster` → `ChatRedisSubscriber`, 실시간 알림
  `RedisRealtimeNotificationPublisher` → `NotificationRedisSubscriber`. write-behind 버퍼
  `RedisReadSequenceBuffer`/`RedisChatRoomSequenceBuffer` 를 `ChatBufferFlushScheduler` 가 flush.
- **멱등성**: `EnrollAcceptIdempotencyStore` / `RedisEnrollAcceptIdempotencyStore`.

---

## 체크리스트

### A. 트랜잭션 경계와 커밋 타이밍
- 외부 호출(FCM·HTTP·S3·OpenAI)이 **트랜잭션 안**에 있지 않은가? 커넥션을 외부 지연만큼 점유한다.
  시나리오 예: "FCM 응답 3초 × 동시 15건 = 인스턴스 pool(15) 고갈, 전 요청 대기".
- 트랜잭션 안에서 **Redis 쓰기·발행**을 하지 않는가? DB 가 롤백돼도 Redis 는 롤백되지 않는다 → 유령 메시지.
- `@Async` 로 넘긴 작업이 호출자의 트랜잭션·영속성 컨텍스트를 쓸 것처럼 작성되지 않았는가?
  (별도 스레드엔 전파되지 않는다 → `LazyInitializationException`, 커밋 전 데이터 조회)

### B. 리스너 phase · Outbox (⚠️ 단순화 금지)
- 위 Outbox 3단계가 **그대로 유지**되는가? 단계를 합치거나 지우면 즉시 🔴.
- `BEFORE_COMMIT` 리스너에서 예외가 나면 **발행 측 트랜잭션 전체가 롤백**된다. 그게 의도인가?
  (알림 저장 실패가 채팅 메시지 저장을 롤백시켜도 되는가?)
- `AFTER_COMMIT` 리스너가 `REQUIRES_NEW` 없이 DB 를 쓰지 않는가? 이미 끝난 트랜잭션에 얹혀 조용히 유실된다.
- 발송 실패가 Outbox 상태로 남는가? 예외를 삼켜 재시도 대상에서 빠지면 알림 영구 유실 → 🔴.
- `AFTER_COMMIT` 발송과 스케줄러 재시도가 **같은 outbox 행을 동시에** 집을 수 있지 않은가?

### C. 스프링 프록시 함정 (정적 분석이 못 잡는 구간)
- `@Transactional`/`@Async` 메서드를 **같은 클래스 안에서** 호출하지 않는가? self-invocation 은 프록시를
  우회해 어노테이션이 조용히 무시된다.
- `REQUIRES_NEW` 를 별도 Bean 에서 호출하는가? 프록시 대상 메서드가 `private`/`final` 이 아닌가?

### D. Redis Pub/Sub 브로드캐스트 (다중 인스턴스)
- 발행이 **커밋 후**인가?
- 구독 측이 **자기 인스턴스에 붙은 STOMP 세션에만** 전달하는가? 시나리오를 "A 인스턴스 발행 / B 인스턴스에
  세션" 형태로 구체적으로 쓴다 (중복 전송 또는 유실).
- 구독 콜백이 예외를 던지면 그 메시지는 조용히 사라진다. 로깅·복구 경로가 있는가?
- 구독 콜백이 동기 DB 쓰기·외부 호출 같은 무거운 일을 하지 않는가? (Redis 리스너 스레드는 소수다)
- 직렬화 형식이 바뀌었다면 **롤링 배포 중 구·신 인스턴스 간 호환성**을 반드시 지적한다.

### E. 락 · 멱등성 · 중복 실행
- 멱등성 키에 **TTL** 이 있는가? 해제 시점이 커밋 이후인가? (커밋 전 해제 → 다른 요청이 커밋 전 상태를 보고 중복 처리)
- `@Scheduled` 가 **여러 인스턴스에서 동시에** 돈다는 전제가 반영됐는가? (SKIP LOCKED·상태 전이·분산락 중
  하나가 없으면 중복 실행) → 없으면 🔴.
- "조회 후 없으면 insert" 로 중복을 막고 있지 않은가? (TOCTOU — DB 유니크 제약이 최종 방어선인가)
- read-modify-write 갱신(정원 증가, 읽음 시퀀스, 버퍼 flush)이 **동시 갱신 시 덮어쓰기**되지 않는가?

### F. WebSocket / STOMP 세션 생명주기
- CONNECT/DISCONNECT 에서 정리할 상태(포커스 방, 구독, 캐시)가 **비정상 종료**에서도 정리되는가?
  (`RedisChatFocusRoomStore` 등 TTL 로 정리하는지 확인)
- 세션 상태를 인스턴스 로컬 메모리에 두지 않는가? 재연결 시 다른 인스턴스에 붙으면 유실된다.

---

## 리포트 형식
- **확실한 위반만** 보고한다 (오탐 0 목표). 애매하면 "확인 필요"로 분리.
- 각 항목: `[동시성] 파일:라인` · **실패 시나리오 한 줄**(어떤 순서로 실행되면 깨지는가) · 제안 수정.
- 심각도: **🔴** Outbox 단계 훼손 · 데이터 유실 · 중복 발송/처리 · 풀 고갈 > **🟡** 프록시 함정 · 정리 누락
  > **🟢** 방어 강화 제안.
- 끝에 한 줄 총평.
