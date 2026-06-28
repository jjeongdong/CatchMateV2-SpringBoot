---
trigger: glob
description: Backend Coding Conventions (Naming, Error Handling, Transaction, 0-import)
globs: "**/*.java"
---

# Backend Coding Conventions

> Java 작업 시 적용되는 코딩 규칙 **단일 출처(SSOT)**. 아키텍처 규칙은 `backend-architecture.md`, 패턴은 `backend-patterns.md`.

## 1. 클래스 네이밍

| 유형 | 패턴 | 예시 | 패키지 |
|:---|:---|:---|:---|
| Controller | `{Domain}Controller` | `BoardController` | `{ctx}.adapter.in.web.controller` |
| WebSocket Listener | `{Domain}WebSocketSessionEventListener` | `ChatWebSocketSessionEventListener` | `{ctx}.adapter.in.websocket` |
| Input Port (UseCase) | `{Domain}{Client/Internal/Admin}{Command/Query}UseCase` | `BoardClientCommandUseCase` | `{ctx}.application.port.in` |
| Service (UseCase 구현) | `{Domain}{Client/Internal/Admin?}{Command/Query?}Service` | `BoardClientCommandService` | `{ctx}.application.service` |
| Aggregate Service (서브) | `{Aggregate}Service` | `ChatRoomService`, `BlockService` | `{ctx}.application.service` |
| Reader (내부 협력자) | `{Domain}Reader` | `BoardReader` | `{ctx}.application.service` |
| Event | `{Domain}{Action}Event` | `EnrollNotificationEvent` | `{ctx}.application.event` |
| Event Listener | `{Self}{Domain}{Action}EventListener` | `NotificationEnrollEventListener` | `{ctx}.adapter.in.event` |
| Command | `{Domain}{Action}Command` | `BoardCreateCommand` | `{ctx}.application.dto.command` |
| Response | `{Domain}{Detail?}Response` | `BoardDetailResponse` | `{ctx}.application.dto.response` |
| Request | `{Domain}{Action}Request` | `BoardCreateRequest` | `{ctx}.adapter.in.web.dto.request` |
| Output Port (Repository) | `{Domain}Repository` | `BoardRepository` | `{ctx}.application.port.out` |
| Output Port (Cross-context) | `{Source}FetchPort` | `UserFetchPort` | `{ctx}.application.port.out` |
| Repository Impl | `{Domain}RepositoryImpl` | `BoardRepositoryImpl` | `{ctx}.adapter.out.persistence.repository` |
| JPA Repository | `Jpa{Domain}Repository` | `JpaBoardRepository` | `{ctx}.adapter.out.persistence.repository` |
| QueryDSL Repository | `QueryDsl{Domain}Repository` | `QueryDslBoardRepository` | `{ctx}.adapter.out.persistence.repository` |
| JPA Entity | `{Domain}Entity` | `BoardEntity` | `{ctx}.adapter.out.persistence.entity` |
| Domain Model | `{Domain}` | `Board` | `{ctx}.domain.model` |
| Fetch Adapter | `{OwnCtx}{Source}FetchAdapter` | `BoardUserFetchAdapter` | `{ctx}.adapter.out.external` |

**금지 접미사**: `Orchestrator`, `ApplicationService`, `Facade`. UseCase 구현은 모두 `{Ctx}Service`.

## 2. 메서드 네이밍

| 용도 | 패턴 |
|:---|:---|
| 단건 조회 (없으면 예외) | `get{Domain}(id)` |
| 단건 조회 (Optional) | `find{Domain}(id)` |
| 목록 조회 | `get{Domain}List(...)` |
| 생성 / 수정 / 삭제 | `create / update / delete{Domain}(...)` |

UseCase 메서드(Response 반환)와 entity getter(도메인 모델 반환)가 충돌하면 getter 를 `{name}Entity` 로 (`getInquiryEntity(Long)`).

## 3. Command 반환 타입 (⭐)

- **생성(Create)**: 생성된 리소스의 **ID 반환** (Long 또는 ID 포함 record).
- **삭제(Delete)**: **void**.
- **수정(Update)**: 보통 **void**, 필요 시 자체 Response record.

## 4. 예외 처리

- 모든 비즈니스 예외는 `BaseException(ErrorCode.XXX)`. 새 케이스는 `ErrorCode` enum 에 추가.
- **`catch (Exception ignored) {}` 절대 금지.** 예상된 null 케이스는 `Optional`.

```java
public Board getBoard(Long id) {
    return boardRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
}
public Optional<Enroll> findEnroll(Long id) { return enrollRepository.findById(id); }
```

## 5. 트랜잭션 경계

- `XxxQueryService` 클래스 레벨: `@Transactional(readOnly = true)`.
- `XxxCommandService` 클래스 레벨: `@Transactional`. 쓰기 메서드만 오버라이드.
- `Propagation.REQUIRES_NEW` 는 **별도 Bean** 에서만 호출 (같은 클래스 내 호출 시 프록시 무시).

## 6. 가독성

- **얼리 리턴** 우선. 깊은 중첩 `if/else` 대신 가드 절.
- **명확한 네이밍** — 추출한 private 메서드는 주석 없이 의도가 읽혀야 함 (`validateStateMatches`, `issueLoginTokens`). 동사 + 목적어.
- 분기 로직은 의도가 드러나는 private 메서드로 추출 — 진입 메서드 본문은 "흐름"만.
- 비즈니스 검증(null/권한/상태 전이)은 Service 진입부에서 명시적으로.

## 7. 하드코딩 금지

Magic number / 딜레이 / 재시도는 `application.yml` + `@Value`.

```java
@Value("${notification.outbox.max-retry-count:5}") private int maxRetryCount;
@Scheduled(fixedDelayString = "${notification.outbox.scheduler-delay-ms:60000}")
public void processPendingPush() { ... }
```

## 8. 도메인 모델 순수성

- `domain/*` 에 Spring / JPA / Jackson 등 인프라 어노테이션 절대 금지.
- 불변(immutable) 지향 — 상태 변경은 새 객체 반환 또는 도메인 메서드.
- `char Y/N` 같은 표현은 boolean 메서드로 노출.

```java
public boolean isEnrollAlarmEnabled() { return 'Y' == this.enrollAlarm; } // ✅
if (recipient.getEnrollAlarm() != 'Y') return;                            // ❌
```

## 9. Entity ↔ Domain ↔ DTO 변환

- Entity ↔ Domain: Entity 내부 `toModel()` / `from(domain)`.
- DTO `from(Domain)` 정적 팩토리에서 **자기 컨텍스트** 도메인 import 는 허용. **다른 컨텍스트 도메인 import 금지** (→ `backend-architecture.md` #6).

## 10. Cross-Domain 0-import (⭐⭐)

타겟: **자기 컨텍스트 어디에도 다른 컨텍스트의 `domain/*` import 0건** — Service/Port/Adapter/Event/Listener/DTO 모두.
- 다른 컨텍스트 `Service`/`Reader`/`Repository`/`Entity` 직접 주입 금지 → `XxxFetchPort` → `XxxFetchAdapter` → 상대 `XxxUseCase` 체인.
- Port/Service/Adapter/Event/Listener 시그니처에 외부 도메인 모델·외부 enum 노출 금지 (record / String).
- 소유 모듈 정적 팩토리(`User.createUser(...)`)는 소유 모듈 안에서만. 어댑터는 소유 모듈 record 만 import.
- 점진 적용 중 — 적용 현황은 별도 추적 문서. 신규/수정 코드는 항상 0-import 타겟으로.

## 11. Service `private final` 필드 순서 (그룹 사이 빈 줄, 그룹 내 타입 알파벳순)

```java
public class BoardClientCommandService implements BoardClientCommandUseCase {
    private final BoardRepository boardRepository;      // 1. own Repository
    private final BoardReader boardReader;              //    own Reader

    private final TokenProvider tokenProvider;          // 2. own 기타 output port

    private final ChatRoomService chatRoomService;      // 3. 같은 컨텍스트 협력 Service (multi-aggregate)

    private final BlockFetchPort blockFetchPort;        // 4. Cross-context Fetch Port (알파벳순)
    private final ClubFetchPort clubFetchPort;
    private final UserFetchPort userFetchPort;

    private final ApplicationEventPublisher applicationEventPublisher; // 5. Spring/Lombok infra
}
```

## 12. import 방향성 체크리스트

| 작성 중인 파일 | import 가능 | import 금지 |
|:---|:---|:---|
| `{ctx}.adapter.in.web.*` | `{ctx}.application.port.in.*`, `{ctx}.application.dto.*` | `{ctx}.application.service.*`(구현체), 다른 `{other}.*` |
| `{ctx}.application.service.*` | `{ctx}.application.port.{in,out}.*`, `{ctx}.domain.*`, `common.*` | `{ctx}.adapter.*`, 다른 `{other}.*` (Fetch Port 경유) |
| `{ctx}.domain.*` | 같은 `{ctx}.domain.*`, `common.*` | Spring, JPA, `{ctx}.application.*`, `{ctx}.adapter.*` |
| `{ctx}.adapter.out.persistence.*` | `{ctx}.application.port.out.*`, `{ctx}.domain.model.*`, JPA/QueryDSL | `{ctx}.application.service.*`, `{ctx}.adapter.in.*` |
| `{ctx}.adapter.out.external.*` (Fetch Adapter) | `{ctx}.application.port.out.*FetchPort`, 다른 `{other}.application.port.in.*UseCase`, 다른 `{other}.application.dto.*`(record) | 다른 `{other}.application.service.*`, 다른 `{other}.domain.*` |
| `{ctx}.adapter.in.event.*` (Listener) | `{ctx}.application.port.in.*`, 다른 `{other}.application.event.*Event`, 다른 `{other}.application.dto.*`(record) | 다른 `{other}.domain.*`, 다른 `{other}.application.service.*` |
