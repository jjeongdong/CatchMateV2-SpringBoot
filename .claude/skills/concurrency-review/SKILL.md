---
name: concurrency-review
description: >-
  catchmate 백엔드를 동시성·트랜잭션 관점에서 리뷰한다 — 트랜잭션 경계와 커밋 타이밍,
  2단계 이벤트 리스너(@EventListener / @TransactionalEventListener AFTER_COMMIT)와 Outbox,
  스프링 프록시 self-invocation 함정, @Async 스레드풀 포화, Redis Pub/Sub 팬아웃의 중복·유실,
  멱등성 락과 스케줄러의 다중 인스턴스 중복 실행, STOMP 세션 생명주기, ALB 뒤 EC2 2대 환경의
  인스턴스 간 레이스. 다음 상황에 사용한다 — "동시성 리뷰", "트랜잭션 경계 봐줘", "레이스 있나",
  "커밋 전에 발송되는 거 아냐", 이벤트 리스너·스케줄러·Redis 퍼블리셔·WebSocket 코드를 수정한 뒤.
  단발 요청이면 이 스킬만 쓰고, 전방위 리뷰는 review-board 오케스트레이터가 호출한다.
---

# Concurrency Review (동시성·트랜잭션 의미론)

이 스킬은 **"언제 실행되고, 그때 무엇이 보이는가"** 만 본다. import 방향·0-import·예외삼킴 같은
구문 위반은 `posttooluse-validate-arch.py` 와 `./gradlew archCheck` 가 이미 결정론적으로 막고,
구조·계약·네이밍은 `hexagonal-review` 가 본다. 여기서 중복 지적하지 않는다.

이 축의 결함은 **테스트가 통과해도 운영에서만 터진다.** 그래서 판정 기준은 "규칙 문서에 어긋나는가"가
아니라 **"어떤 순서로 실행되면 깨지는지 시나리오를 쓸 수 있는가"** 다. 시나리오를 못 쓰면 위반이
아니라 "확인 필요"다.

## 0. 스코프 잡기
1. 오케스트레이터가 준 스코프를 그대로 쓴다. 없으면 `git diff --name-only`(+ staged, 없으면 워킹트리).
2. `./gradlew archCheck` 를 한 번 돌려 구문 게이트 상태를 확인한다 (통과 전제로 의미론에 집중).
3. **호출 경로를 역추적한다.** 변경된 서비스가 발행하는 이벤트를 누가 구독하는지, 그 리스너가
   무엇을 호출하는지 Grep 으로 끝까지 따라간다. 이 축은 파일 단독으로 판정되지 않는다.

---

## 체크리스트

### A. 트랜잭션 경계와 커밋 타이밍
- 외부 호출(FCM·HTTP·S3·메일)이 **트랜잭션 안**에 있지 않은가? 있으면 커넥션을 외부 지연만큼
  점유한다 → 풀 고갈. 실패 시나리오: "FCM 응답 3초 × 동시 15건 = pool(15) 고갈, 전 요청 대기".
- 발송(dispatch) 성격의 service 에 클래스 레벨 `@Transactional` 이 붙지 않았는가?
- `QueryService` 는 `@Transactional(readOnly = true)`, `CommandService` 는 `@Transactional` 인가?
- 트랜잭션 안에서 **Redis 쓰기/발행**을 하고 있지 않은가? DB 롤백돼도 Redis 는 롤백되지 않는다
  → 유령 메시지. (그래서 `ChatMessageRedisPublisher` 가 `AFTER_COMMIT` 이다.)
- `@Async` 로 넘긴 작업이 **호출자의 트랜잭션·영속성 컨텍스트를 쓸 것처럼** 작성되지 않았는가?
  별도 스레드엔 트랜잭션도 세션도 전파되지 않는다 → LazyInitializationException / 커밋 전 조회.

### B. 2단계 리스너 · Outbox (⚠️ 단순화 금지 — CLAUDE.md 1순위 불변 규칙)
- `@EventListener`(커밋 전 Outbox DB 저장) → `@TransactionalEventListener(AFTER_COMMIT)`(커밋 후
  발송) → `NotificationScheduler`(재시도) 3단계가 **그대로 유지**되는가? 합치거나 단계를 지우는
  변경이면 즉시 🔴.
- `AFTER_COMMIT` 리스너에서 **새 DB 쓰기**를 하고 있지 않은가? 이미 커밋된 트랜잭션에 얹으면
  참여되지 않아 조용히 유실된다. 써야 하면 `REQUIRES_NEW` 로 명시적 새 트랜잭션인가?
- 발송 실패가 **Outbox 상태로 남는가**? 예외를 삼켜 재시도 대상에서 사라지면 알림 영구 유실 → 🔴.
- 스케줄러 재시도와 `AFTER_COMMIT` 발송이 **동시에 같은 outbox 행**을 집을 수 있지 않은가?
  (skip locked / 상태 전이로 방어되는지 `OutboxStateTransitioner` 로 확인)

### C. 스프링 프록시 함정 (정적 분석이 절대 못 잡는 구간)
- `@Transactional` / `@Async` 메서드를 **같은 클래스 안에서 직접 호출**하지 않는가?
  self-invocation 은 프록시를 우회해 어노테이션이 **통째로 무시**된다 (예외 없이 조용히).
- `Propagation.REQUIRES_NEW` 가 **별도 Bean** 에서 호출되는가?
- 프록시 대상 메서드가 `private`/`final`, 클래스가 `final` 이 아닌가? → 어노테이션 무효.
- 인터페이스 메서드를 지워 JDK 동적 프록시에서 메서드가 사라지지 않는가?
  (`RedisNotificationPublisher` ↔ `ChatMessageRedisPublisher` 분리는 이 이유로 **합치기 금지** — 🔴)

### D. Redis Pub/Sub 팬아웃 (ALB 뒤 EC2 2대)
- 발행이 **커밋 후**인가? (A 참조)
- 구독 측이 **자기 인스턴스에 붙은 STOMP 세션에만** relay 하는가? 전 인스턴스가 무조건 전송하면
  같은 메시지가 2번 간다. 반대로 세션이 다른 인스턴스에 있는데 로컬만 보면 유실이다.
  실패 시나리오를 "A 인스턴스 발행 / B 인스턴스에 세션" 형태로 구체적으로 쓴다.
- 구독 콜백에서 **예외를 던지면** 그 메시지만 조용히 사라진다 — 로깅·복구 경로가 있는가?
- 구독 콜백이 무거운 작업(동기 DB 쓰기·외부 호출)을 하고 있지 않은가? Redis 리스너 스레드는 소수다.
- 직렬화 계약: 발행 측 payload 타입과 구독 측 역직렬화 타입이 일치하는가?
  (`RedisConfig` 의 serializer 변경은 **배포 중 롤링 구간에서 구/신 인스턴스 간 호환성**이 깨질 수
  있다 — 변경됐다면 반드시 지적)

### E. 락 · 멱등성 · 중복 실행
- 멱등성 락(`IdempotencyPort` / `RedisIdempotencyAdapter`)에 **TTL 이 있는가**? 없으면 프로세스가
  죽을 때 락이 영구히 남아 해당 동작이 영구 차단된다.
- 락 획득 → 처리 → **해제 시점**이 커밋 이후인가? 커밋 전에 풀면 다른 요청이 커밋 전 상태를 보고
  중복 처리한다. (`fix(enroll): 수락 완료 시 멱등성 락 즉시 해제` 커밋의 의도를 확인하고 판정)
- `@Scheduled` 가 **인스턴스 2대에서 동시에** 돈다는 전제가 반영됐는가? (skip locked·상태 전이·
  분산락 중 하나가 없으면 중복 발송) → 없으면 🔴.
- DB 유니크 제약 대신 "조회 후 없으면 insert" 로 중복을 막고 있지 않은가? (TOCTOU)
- 카운터·시퀀스 갱신이 read-modify-write 인가? (`ReadSequenceFlushScheduler`,
  `ChatRoomSequenceFlushScheduler` 처럼 flush 하는 값은 **동시 갱신 시 덮어쓰기** 여부를 본다)

### F. WebSocket / STOMP 세션 생명주기
- CONNECT/DISCONNECT 시 정리해야 할 상태(온라인 여부·구독 목록·in-memory map)가 **예외 경로와
  비정상 종료(네트워크 끊김)** 에서도 정리되는가? 정상 DISCONNECT 만 가정하면 stale 상태가 쌓인다.
- 하트비트 설정(`WebSocketConfig`)에 의존하는 정리 로직이 있으면, 하트비트 주기와 정리 타임아웃이
  모순되지 않는가?
- 세션 스코프 상태를 **인스턴스 로컬 메모리**에 두고 있지 않은가? stickiness 로 세션은 고정되지만,
  재연결 시 다른 인스턴스에 붙을 수 있다 → 상태 유실.

---

## 리포트 형식
- **확실한 위반만** 보고한다 (오탐 0 목표). 애매하면 "확인 필요"로 분리.
- 각 항목: `[동시성] 파일:라인` · **실패 시나리오 한 줄**(어떤 순서로 실행되면 깨지는가) · 제안 수정.
- 심각도: **🔴** 불변 규칙 위반(2단계 리스너/Outbox/Redis 분리) · 데이터 유실 · 중복 발송 · 풀 고갈
  > **🟡** 프록시 함정·정리 누락 > **🟢** 방어 강화 제안.
- 끝에 한 줄 총평 + `./gradlew archCheck` 통과 여부.

## 규칙 SSOT
- `.claude/ondemand-rules/backend-patterns.md` — 2단계 리스너 · Outbox · Redis 두 퍼블리셔 분리
- `.claude/ondemand-rules/backend-coding-conventions.md` #5 — 트랜잭션 경계
- 실측 근거: `docs/chat-delivery-loadtest-*.md`, `docs/chat-read-write-behind-analysis.md`
