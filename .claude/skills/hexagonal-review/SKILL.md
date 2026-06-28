---
name: hexagonal-review
description: >-
  catchmate 백엔드 변경분을 헥사고날·DDD 규칙의 '의미론' 관점에서 리뷰한다. PostToolUse 훅과
  ./gradlew archCheck 가 잡는 import 방향·0-import·예외삼킴(구문) 위에서, 정적 분석이 못 잡는
  intent 레벨을 본다 — 정문 목적 분리(Internal/Client/Admin), 이벤트 페이로드 누수(구독자 관심사),
  2단계 리스너/트랜잭션 경계, soft delete, 네이밍, Command 반환 타입. 다음 상황에 사용한다 —
  한 컨텍스트의 Java 파일 여러 개를 수정/생성한 뒤, "아키텍처 리뷰", "헥사고날 점검", "이거 규칙 맞아?",
  service/listener/event/usecase 작업을 마무리할 때. 구문 위반은 훅이 이미 보고하므로 중복 보고하지 않는다.
---

# Hexagonal Review (의미론 리뷰)

이 스킬은 **사람이 읽어야 판단되는** 규칙만 본다. import 방향·도메인 순수성·cross-context import·
예외삼킴은 `validate-java-architecture.py`(PostToolUse 훅)와 `./gradlew archCheck` 가 결정론적으로
이미 막으므로 **여기서 중복 지적하지 않는다.** 의도/계약/경계만 검토한다.

## 0. 스코프 잡기
1. `git diff --name-only` 로 바뀐 `*.java` 추림 (없으면 워킹트리 + staged 모두).
2. `./gradlew archCheck` 한 번 실행 — 구문 위반은 여기서 걸리니, 통과를 전제로 의미론에 집중.
3. 바뀐 파일이 속한 레이어(service/listener/event/usecase/adapter)별로 아래 체크리스트 적용.

---

## 의미론 체크리스트 (훅이 못 잡는 것만)

### A. 정문(UseCase) 목적 분리 — 규칙 architecture #2, conventions #1
- Cross-context 진입이 **목적에 맞는 Internal 계열**인가? 읽기=`{T}InternalQueryUseCase`, 쓰기=`{T}InternalCommandUseCase`, admin 컨텍스트만 `{T}AdminQueryUseCase`.
  - ❌ FetchAdapter 가 Controller 전용 `{T}ClientCommandUseCase` 로 진입 (훅은 port.in import 라 통과시킴 — 의미론으로만 잡힘).
- UseCase 구현 클래스명이 `{Domain}{Client/Internal/Admin}{Command/Query}Service` 형태인가? **금지 접미사** `Orchestrator`/`Facade`/`ApplicationService` 없는가?

### B. 이벤트 = published contract — 규칙 patterns #4·#5 (가장 자주 새는 곳)
- 이벤트 record 필드가 **publisher 의 비즈니스 사실만**인가?
  - ❌ `recipientIds`, `fcmTokens`, `List<User>`, 다른 컨텍스트 enum/템플릿 → 구독자(notification) 책임이 발행자로 누수.
  - ✅ `noticeId`, `noticeTitle` 같은 식별자 + 자기 컨텍스트 primitive.
- "누구에게 어떻게 알릴지" 결정이 **구독자(리스너)** 에 있는가? 대상자 조회를 발행 컨텍스트가 하고 있지 않은가?
- 이벤트 클래스가 publisher 의 `application/event/` 에 있고, 리스너는 subscriber 의 `adapter/in/event/` 에 있는가? (같은 디렉토리에 두지 말 것)

### C. 2단계 리스너 / 트랜잭션 경계 — 규칙 patterns #6, conventions #5 (⚠️ 단순화 금지)
- 외부 호출(FCM/메일/HTTP)이 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 에만 있는가? `@EventListener`/`BEFORE_COMMIT` 에서 외부 호출하면 ❌.
- 발송(dispatch) service 에 클래스 레벨 `@Transactional` 이 **붙어있지 않은가**? (외부 호출 동안 커넥션 점유 → ❌)
- `QueryService` = `@Transactional(readOnly = true)`, `CommandService` = `@Transactional` 인가?
- `Propagation.REQUIRES_NEW` 가 **별도 Bean** 에서 호출되는가? (같은 클래스 self-invocation 이면 프록시 무시 → 무효)
- 리스너가 **얇은가**? 비즈니스 로직 없이 UseCase 호출만 하는가?

### D. Command/조회 계약 — 규칙 conventions #2·#3
- `create*` 가 생성된 ID(또는 ID 포함 record)를 반환하는가? `delete*` 는 `void` 인가?
- 단건 조회: 없으면 예외 → `get*`, Optional → `find*` 네이밍 일치하는가?

### E. 도메인 순수성(의미) — 규칙 conventions #8
- `char Y/N` 등 원시 표현을 외부에 노출하지 않고 boolean 도메인 메서드(`isEnrollAlarmEnabled()`)로 감쌌는가?
- 도메인 모델이 불변 지향인가? (상태 변경이 setter 난사가 아니라 도메인 메서드/새 객체)

### F. Soft Delete / Redis — 규칙 patterns
- 물리 삭제 쿼리(`delete from`, `repository.delete...` 로 hard delete)를 추가하지 않았는가? 엔티티에 `deletedAt` + `@SQLRestriction` 있는가?
- `RedisPublisher` 와 `ChatMessageRedisPublisher` 를 합치거나 한쪽 인터페이스 메서드를 지우지 않았는가?

### G. 예외·하드코딩 — 규칙 conventions #4·#7
- 비즈니스 예외가 `BaseException(ErrorCode.XXX)` 인가? (generic `RuntimeException`/문자열 throw ❌, 새 케이스는 `ErrorCode` 추가)
- magic number/딜레이/재시도 횟수가 `@Value` + `application.yml` 로 빠졌는가? (인라인 상수 ❌)

### H. DTO 격리(의미) — 규칙 conventions #9·#10
- Cross-context FetchPort 출력이 **자기 컨텍스트 record** 인가? 상대 `XxxInternalResponse` 를 FetchAdapter 밖으로 흘리지 않았는가?
- DTO `from(Domain)` 정적 팩토리가 **자기 컨텍스트 도메인만** import 하는가?

---

## 리포트 형식
- **확실한 위반만** 보고한다 (오탐 0 목표). 애매하면 "확인 필요"로 분리.
- 각 항목: `파일:라인` · 위반 내용 · 위반한 규칙(예: `patterns #5`) · 제안 수정.
- 심각도 분류: **🔴 불변 규칙 위반**(2단계/Outbox/Redis/트랜잭션 경계) > **🟡 계약·네이밍** > **🟢 가독성**.
- 끝에 한 줄 요약 + `./gradlew archCheck` 결과(구문 게이트 통과 여부) 명시.

## 규칙 SSOT
- `.claude/ondemand-rules/backend-architecture.md` (정문·Fetch Port·의존성 방향)
- `.claude/ondemand-rules/backend-coding-conventions.md` (네이밍·예외·트랜잭션·0-import)
- `.claude/ondemand-rules/backend-patterns.md` (이벤트 2단계·Outbox·AOP·QueryDSL·Redis)

관련 생성 스킬: `cross-context-access`(Fetch Port 배선), `add-notification`(Outbox 2단계 추가).
