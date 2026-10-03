---
name: architecture-review
description: >-
  catchmate 백엔드 변경분을 DDD 4계층·BC 경계 규칙의 '의미론' 관점에서 리뷰한다. ArchUnit
  (`com.back.catchmate.architecture.*`)이 잡는 import 방향·접미사 위치·클래스 레벨 @Transactional
  같은 구문 위반 위에서, 정적 분석이 못 잡는 의도를 본다 — 비즈니스 규칙이 엔티티에 있는가, Service 가
  흐름만 하는가, 타 BC 상태를 이벤트로만 바꾸는가, 이벤트 페이로드가 발행 측 사실만 담는가, DTO 흐름
  (Request → Command → Result), 에러 코드·예외 설계, URL·상태 코드·페이징 계약. 다음 상황에 사용한다 —
  "아키텍처 리뷰", "DDD 규칙 맞아?", "이거 규칙 맞아?", "BC 경계 봐줘", Service·엔티티·이벤트·
  QueryApi·Controller 작업을 마무리할 때. 전방위 리뷰는 review-board 오케스트레이터가 호출한다.
---

# Architecture Review (DDD 의미론)

이 스킬은 **사람이 읽어야 판단되는** 규칙만 본다. 아래는 테스트가 이미 결정적으로 막으므로
**중복 지적하지 않는다** — 의도·계약·경계만 검토한다.

| 이미 자동 검사됨 | 위치 |
|---|---|
| 계층 의존 방향, Domain 의 spring web/http/data import, 타 BC 내부 접근, QueryApi 의 타 BC 의존, global → BC 의존 | `ArchitectureRules` |
| 접미사별 패키지 위치 (`*Command`, `*Event`, `*Exception` …) | `ArchitectureRules.locationRules` |
| 클래스 레벨·Application 밖 `@Transactional`, 필드 주입, `System.out`, `Optional.get()`, Domain 의 `now()`, `Date`/`Calendar` | `ArchitectureRules.codingRules` |
| 금지 Lombok, `var`, `TODO`, 와일드카드 import | `SourceConventionRules` |

## 0. 스코프 잡기
1. 오케스트레이터가 준 스코프와 아키텍처 테스트 결과를 그대로 쓴다. 단독 실행이면
   `git diff --name-only`(+ staged, 없으면 `main...HEAD`)로 바뀐 `*.java` 를 추리고,
   `./gradlew test --tests 'com.back.catchmate.architecture.*'` 를 한 번 돌린다.
2. 바뀐 파일의 계층(presentation/application/domain/infrastructure)별로 아래 체크리스트를 적용한다.
3. 판단 근거는 `CLAUDE.md` 의 절대 규칙과 `.claude/rules/*.md` 다. 지적마다 근거 문서를 적는다.

---

## 체크리스트

### A. 비즈니스 규칙의 위치 — `domain.md` 비즈니스 규칙, `application.md` 역할
- Service 에 `if (...) throw` 형태의 규칙(권한·상태 전이·정원)이 있지 않은가? → 엔티티 메서드로.
  타 BC 값이 필요하면 `QueryApi` 로 조회해 **도메인 메서드 인자**로 넘기는 것이 정답이다.
- 엔티티 상태 변경이 의미를 드러내는 메서드(`accept`, `liftUp`)인가? `update()`, `changeStatus(status)`,
  public setter 성격의 메서드는 위반이다.
- 생성은 `create(...)` 정적 팩토리 + `private` 생성자이고, 생성 검증이 `create()` 안에 있는가?
- Controller 에 로직이 있지 않은가? 허용되는 것은 `request.toCommand()` → Service 호출 1회 → 응답뿐.

### B. BC 경계의 의미 — 절대 규칙 1·2, `architecture.md` BC 경계
- 타 BC **상태 변경**을 이벤트가 아닌 방법(QueryApi 에 쓰기 메서드 추가, 우회 호출)으로 하지 않는가?
  QueryApi 에 상태를 바꾸는 메서드가 생겼다면 🔴.
- `QueryApi` 가 엔티티가 아닌 `dto/api/{Domain}Info` record 를 반환하고, public 메서드에 Javadoc 이 있는가?
- 자기 Controller 용 `CommandService`/`QueryService` 를 타 BC 가 호출하지 않는가?
- 네이티브 SQL·문자열 JPQL 로 **타 BC 테이블을 JOIN** 하지 않는가? (import 가 없어 ArchUnit 이 못 잡는다.)
  예외라면 사유·측정 근거 주석 + `BoundedContexts.CROSS_CONTEXT_ALLOWLIST` 등록이 있는가?
- BC 간 `@ManyToOne`/`@OneToOne` 대신 ID 필드로 참조하는가?

### C. 이벤트 = 발행 측의 계약 — `domain.md` 도메인 이벤트, `application.md` 이벤트 리스너
- 이벤트 record 필드가 **발행 측의 비즈니스 사실**(ID·원시값)뿐인가?
  - ❌ `recipientIds`, `fcmTokens`, 알림 문구·템플릿 → 수신 측(notification) 책임이 발행 측으로 샌 것.
- 발행이 Application Service 에서 일어나는가? (엔티티 안 발행 금지)
- 리스너 본문이 **자기 BC Service 호출 한 줄**인가? 리스너 안에 분기·조회·규칙이 있으면 위반.
- 기본 리스너는 `AFTER_COMMIT` + `REQUIRES_NEW` 다. `BEFORE_COMMIT` 이면 **사유 주석**이 있는가?

### D. 트랜잭션 배치 — 절대 규칙 4, `application.md` 트랜잭션
- 조회 메서드에 `readOnly = true` 가 있는가? 쓰기 메서드에 `@Transactional` 이 빠지지 않았는가?
- QueryService 가 타 BC 데이터를 `getInfos(ids)` 로 **한 번에** 모으는가? (반복문 안 호출은 성능 축과 겹치지만
  규칙 위반이기도 하다 — 자체 검열하지 말고 보고)

### E. DTO 흐름 — 절대 규칙 5, `application.md` DTO
- Request → Command → Result 흐름을 지키는가? Result 를 엔티티에서 바로 만들 때 `from`/`of` 를 쓰는가?
- Mapper 클래스·MapStruct 를 새로 만들지 않았는가?
- 이름이 `{Domain}{Action}Command/Result` 규칙을 따르고, 메서드명이 동의어(`register`, `save`, `modify`,
  `fetch` …)를 쓰지 않는가?

### F. 에러 설계 — 절대 규칙 6, `domain.md` 에러 코드와 예외
- 새 에러 상황마다 **전용 예외 클래스** + `{Bc}ErrorCode` 상수가 있는가? 범용 예외 재사용 금지.
- 상수 이름이 `{DOMAIN}_{SITUATION}` 이고, `ErrorType` 이 상황에 맞는가? (권한 없음을 `INVALID` 로 쓰는 등)
- 예외를 던지면서 로그도 남기지 않는가? (`code-style.md` 로깅 4)

### G. API 계약 — `presentation.md`
- URL(복수 명사, kebab-case, `{도메인Id}`, `/me`, 중첩 2단계), 상태 코드(생성 201·삭제 204), 페이징
  (누적 목록은 커서, 페이지 화면은 오프셋), 응답 타입(`CursorPageResult`/`OffsetPageResult`)이 맞는가?
- 기존 API 의 경로·응답 형태를 바꿨다면 **클라이언트 호환성**을 지적한다 (🟡, 의도 확인 필요).

---

## 리포트 형식
- **확실한 위반만** 보고한다 (오탐 0 목표). 애매하면 "확인 필요"로 분리.
- 각 항목: `[아키텍처] 파일:라인` · 위반 내용 · 근거 규칙(예: `domain.md 비즈니스 규칙`) · 제안 수정.
- 심각도: **🔴** 절대 규칙 위반(BC 경계·상태 변경 경로·규칙 위치) > **🟡** 계약·이름·에러 설계 > **🟢** 가독성.
- 끝에 한 줄 총평 + 아키텍처 테스트 결과.
