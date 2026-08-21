---
name: hexagonal-to-mvc-mapping
description: >-
  catchmate 백엔드의 헥사고날 구조를 기능별 패키지 3계층 MVC
  (controller/service/repository/entity/dto)로 옮기는 **변환 규칙 SSOT**. 어떤 파일이
  어디로 가고, 무엇이 병합되고, 무엇이 삭제되는지 파일 종류별로 규정한다 — UseCase 인터페이스
  제거, FetchPort/FetchAdapter 체인 걷어내고 Service 직접 주입, 도메인 모델과 JPA 엔티티 병합,
  Reader 흡수, Client/Internal/Admin 서비스 통합, DTO 이름 충돌 해소. 다음 상황에 사용한다 —
  "이 파일 MVC 로 어디로 가야 해", "UseCase 어떻게 없애", "FetchAdapter 걷어내줘",
  "엔티티랑 도메인 합쳐줘", "Reader 어떻게 처리해", 한 컨텍스트를 MVC 로 옮기는 작업 중,
  변환 결과가 규칙에 맞는지 확인할 때. 여러 컨텍스트를 순서대로 옮기는 전체 마이그레이션은
  mvc-migration 오케스트레이터가 이 규칙을 SSOT 로 삼아 호출한다.
---

# 헥사고날 → MVC 변환 규칙 (SSOT)

목표 구조는 **기능별 패키지 3계층**이다. Bounded Context 경계(`board`, `chat` …)는 유지하고
그 안을 평탄화한다.

```
com.back.catchmate.{ctx}/
├── controller/     @RestController, STOMP @MessageMapping
├── service/        @Service @Transactional — 유일한 비즈니스 계층
├── repository/     Spring Data JPA + QueryDSL
├── entity/         @Entity (= 도메인 모델. 별도 domain 없음)
├── dto/request/    컨트롤러 수신
│   /response/      컨트롤러 반환 + 컨텍스트 간 반환
├── event/          이벤트 + 리스너
└── infra/          FCM·S3·Redis·외부 API 등 진짜 외부 연동 (선택, 있는 컨텍스트만)
```

`global/`, `common/` 은 **건드리지 않는다.** 이미 계층 개념이 아니라 공용 인프라다.

---

## 왜 이 규칙이 이렇게 생겼는가

헥사고날의 인터페이스 계층은 두 가지 일을 겸하고 있었다 — **의존 역전**과 **호출자별 접근 제어**
(Client/Internal/Admin). MVC 로 가면 전자는 사라지지만 후자는 사라지지 않는다.
`BoardService` 하나에 컨트롤러용 메서드와 남의 컨텍스트용 메서드가 섞이면, 지금 인터페이스가
막아주던 "남이 내 Client 메서드를 부르는" 사고가 열린다.

그래서 이 규칙은 **인터페이스는 지우되 그 자리에 이름 규약과 검증 훅을 놓는다.**
지우는 것과 잃는 것을 구분하는 게 이 문서의 핵심이다.

---

## 파일 종류별 매핑표

| 현재 | 이동/처리 | 비고 |
|---|---|---|
| `adapter/in/web/controller/*Controller` | → `controller/` | 그대로 이동 |
| `adapter/in/web/dto/request/*Request` | → `dto/request/` | 그대로 이동 |
| `adapter/in/websocket/*` | → `controller/` | 이름 유지 (`ChatWebSocketController` 등) |
| `adapter/in/event/*EventListener` | → `event/` | 그대로 이동 |
| `adapter/in/event/*RedisSubscriber` | → `event/` | 그대로 이동 |
| `adapter/in/scheduler/*Scheduler` | → `service/` 옆 `scheduler/` | 별도 유지 (진입점이라 계층이 아님) |
| `application/port/in/*UseCase` | **삭제** | 인터페이스 제거 |
| `application/service/{Ctx}*Service` | → `service/{Ctx}Service` 로 **통합** | 아래 §서비스 통합 |
| `application/service/{Ctx}Reader` | **흡수 → 삭제** | 아래 §Reader |
| `application/service/*Assembler` | → `service/` 유지 | 조립 로직은 서비스 계층이 맞다 |
| `application/dto/command/*Command` | → `dto/request/` 흡수 or `dto/command/` | 아래 §DTO |
| `application/dto/response/*Response` | → `dto/response/` | 아래 §DTO |
| `application/event/*Event` | → `event/` | 그대로 이동 |
| `application/port/out/persistence/{Ctx}Repository` | **삭제** (인터페이스) | 아래 §Repository |
| `application/port/out/external/*FetchPort` | **삭제** | 간접층 제거 |
| `application/port/out/dto/*Info` | **삭제** | 상대 `dto/response` 를 직접 쓴다 |
| `adapter/out/persistence/entity/{X}Entity` | → `entity/{X}` | 아래 §엔티티 |
| `adapter/out/persistence/repository/Jpa{X}Repository` | → `repository/{X}Repository` | |
| `adapter/out/persistence/repository/{X}RepositoryImpl` | → `repository/{X}RepositoryImpl` | QueryDSL 구현 |
| `adapter/out/persistence/repository/QueryDsl{X}Repository` | → `repository/{X}RepositoryCustom` | |
| `adapter/out/external/*FetchAdapter` | **삭제** | 상대 Service 직접 주입 |
| `adapter/out/external/*CommandAdapter` | **삭제** | 쓰기 측 cross-context 어댑터. 위와 동일 |
| `adapter/out/external/` 나머지 | → `infra/` | Fcm·S3·Redis·SpringAi 등 **진짜** 외부 연동 |
| `domain/model/{X}` | **엔티티에 병합 → 삭제** | 아래 §엔티티 |
| `domain/model/*Status`, `*Range` 등 enum | → `entity/` 또는 `entity/enums/` | |
| `domain/dto/*SearchCondition` | → `dto/request/` | |

`adapter/out/external` 을 기계적으로 옮기면 안 된다. 이 디렉토리엔 **두 종류**가 섞여 있다 —
다른 컨텍스트를 부르는 어댑터(삭제 대상)와 FCM·S3·Redis 같은 실제 외부 연동(`infra/` 로 보존).
클래스가 `com.back.catchmate.{다른컨텍스트}` 를 import 하면 전자, 아니면 후자다.

---

## §엔티티 — 도메인 모델과 JPA 엔티티 병합

지금은 `Club`(순수) ↔ `ClubEntity`(JPA) 두 벌이고 `toDomain()`/`toEntity()` 로 오간다.
MVC 에선 `entity/Club` 한 벌이다.

1. `{X}Entity` 를 `entity/{X}` 로 옮기고 클래스명에서 `Entity` 접미사를 뗀다.
2. `toDomain()` / `toEntity()` / `from()` 변환 메서드를 **삭제**한다.
3. 순수 도메인 모델(`domain/model/{X}`)에만 있던 **비즈니스 메서드를 엔티티로 옮긴다.**
   필드는 이미 엔티티에 있으니 옮길 것은 행위뿐이다.
4. `domain/model/{X}` 를 삭제한다.

**여기서 잃기 쉬운 것:** 순수 도메인 모델에 있던 검증·상태전이 메서드를 옮기지 않고 삭제하면
로직이 조용히 사라진다. 병합 후 반드시 확인한다 — 도메인 모델의 public 메서드 중
getter/builder 가 아닌 것이 전부 엔티티에 있는가?

**soft delete 는 그대로 간다.** `deletedAt` 필드, `@SQLRestriction("deleted_at IS NULL")`,
`delete()` 메서드는 엔티티에 그대로 살아있어야 한다. 대상은 `User`·`Board`·`ChatRoom`·
`ChatMessage` 넷뿐이고, 나머지 조인·토글·토큰·아웃박스 엔티티는 물리 삭제가 정상이다.
(이 구분은 아키텍처가 아니라 데이터 성격의 문제라 MVC 로 와도 바뀌지 않는다.)

---

## §Repository — 3개 파일이 2~3개로

현재:
```
application/port/out/persistence/ClubRepository   (인터페이스, 도메인 타입 반환)
adapter/out/persistence/repository/JpaClubRepository      (Spring Data)
adapter/out/persistence/repository/ClubRepositoryImpl     (포트 구현, Entity↔Domain 변환)
```

전환 후:
```
repository/ClubRepository        extends JpaRepository<Club, Long>, ClubRepositoryCustom
repository/ClubRepositoryCustom  (QueryDSL 시그니처, 있는 경우만)
repository/ClubRepositoryImpl    (QueryDSL 구현, 있는 경우만)
```

- 포트 인터페이스(`application/port/out/persistence/*`)는 삭제한다. Spring Data 인터페이스가
  그 역할을 대신한다.
- `{X}RepositoryImpl` 이 하던 **Entity↔Domain 변환은 통째로 사라진다** (엔티티가 하나가 됐으므로).
  변환 코드만 걷어내고 QueryDSL 쿼리는 그대로 살린다.
- QueryDSL 이 없는 컨텍스트면 `ClubRepository` 하나로 끝난다.
- Spring Data 명명 규칙에 맞추려면 `{X}RepositoryImpl` 은 반드시 `{X}RepositoryCustom` 의
  구현이어야 하고 이름이 정확히 `{X}RepositoryImpl` 이어야 한다. 어긋나면 런타임에
  "No property found" 로 터진다.

---

## §서비스 통합 — Client/Internal/Admin 을 하나로

`{Ctx}ClientQueryService` · `{Ctx}ClientCommandService` · `{Ctx}InternalQueryService` ·
`{Ctx}InternalCommandService` · `{Ctx}AdminQueryService` → **`{Ctx}Service` 하나**.

분리 기준은 규모다:
- 기본: `{Ctx}Service` 하나로 합친다.
- 합친 결과가 **public 메서드 20개 초과 또는 400줄 초과**면 축 하나로만 쪼갠다 —
  `{Ctx}CommandService` / `{Ctx}QueryService`. Client/Internal 축으로는 쪼개지 않는다.
- `{Ctx}Assembler` 는 합치지 말고 그대로 둔다. 응답 조립은 별개 관심사다.

**호출자 구분이 사라지는 문제를 이름으로 막는다.** 통합 서비스에서 다른 컨텍스트가 부르라고
남긴 메서드는 이름 끝에 의도를 남긴다:

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {
    private final ClubRepository clubRepository;

    // 컨트롤러용 — 전체 응답
    public ClubResponse getClub(Long clubId) { ... }

    // 다른 컨텍스트용 — 최소 필드만. 남이 부르는 메서드라는 표시
    public ClubSummary getClubSummary(Long clubId) { ... }
}
```

컨트롤러용 메서드가 남의 컨텍스트에서 호출되는 것을 막을 구조적 장치는 이제 없다.
`{Ctx}Summary` 같은 축약 응답 타입을 반환하는 메서드만 cross-context 로 쓰고,
리뷰에서 이 경계를 본다. 이것이 인터페이스를 지우고 치르는 값이다.

**`@Transactional` 은 그대로 옮긴다.** 클래스에 `readOnly = true`, 쓰기 메서드에
`@Transactional` 재선언. 합치는 과정에서 쓰기 메서드가 `readOnly = true` 를 물려받으면
조용히 저장이 안 된다 — 통합 시 가장 흔한 사고다.

---

## §Reader — 흡수

`{Ctx}Reader` 는 "조회 + 없으면 예외" 를 모으는 헬퍼다. MVC 에선 Service 안으로 흡수한다.

```java
// ClubReader.getClub(id) 였던 것
private Club getClubOrThrow(Long clubId) {
    return clubRepository.findById(clubId)
            .orElseThrow(() -> new BaseException(ErrorCode.CLUB_NOT_FOUND));
}
```

- Reader 메서드는 통합 Service 의 **private 메서드**가 된다. 이름은 `get{X}OrThrow` 로.
- Reader 를 다른 서비스가 주입받고 있었다면 그 호출부도 함께 고친다.
- `ErrorCode` 는 그대로다. 예외 코드가 바뀌면 클라이언트가 깨진다.

---

## §Cross-context — FetchPort 체인 제거

지금:
```
BookmarkClientQueryService
  → BoardFetchPort (인터페이스)
  → BookmarkBoardFetchAdapter (구현, BoardInternalResponse → BookmarkBoardInfo 변환)
  → BoardInternalQueryUseCase
```

전환 후:
```
BookmarkService → BoardService.getBoardSummaries(ids)  // 직접 주입
```

1. `FetchPort` 인터페이스, `FetchAdapter` 구현, `port/out/dto/*Info` 세 개를 모두 삭제한다.
2. 호출부는 상대 `{Ctx}Service` 를 생성자 주입하고, 상대의 `dto/response` 타입을 직접 쓴다.
3. FetchAdapter 안에 있던 **필드 매핑 로직은 버려진다** — 같은 필드를 이름만 바꿔 담던 코드라
   대부분 순수 손실이다. 다만 어댑터가 필터링·기본값·널 처리를 하고 있었다면 그것은
   호출부나 상대 서비스로 옮겨야 한다. 어댑터를 지우기 전에 `map(...)` 안을 읽어라.

**0-import 규칙은 여기서 폐기된다.** 이제 `bookmark` 가 `board.dto.response.BoardSummary` 를
직접 import 한다. 그 대신 지켜야 할 선이 둘 남는다 — 남의 `repository` 와 남의 `controller` 는
import 하지 않는다. (MVC 검증 훅이 이 둘을 막는다.)

---

## §DTO — 병합과 이름 충돌

- `adapter/in/web/dto/request/*Request` → `dto/request/`
- `application/dto/response/*Response` → `dto/response/`
- `application/dto/command/{X}Command`: 대응하는 `{X}Request` 와 필드가 같으면 **Request 로
  흡수**하고 Command 를 삭제한다. 여러 Request 가 한 Command 로 모이거나 서비스가 컨트롤러
  타입을 몰라야 하는 이유가 있으면 `dto/command/` 에 남긴다.

**이름 충돌이 진짜 문제다.** 지금은 컨텍스트마다 자기 DTO 를 따로 갖고 있어서 같은 개념이
여러 이름으로 존재한다:

```
board/port/out/dto/BoardUserInfo      ┐
chat/port/out/dto/ChatUserInfo        ├ 전부 "유저 요약" 을 가리킴
enroll/port/out/dto/EnrollUserInfo    ┘
user/application/dto/response/UserInternalResponse
user/application/dto/response/UserResponse
```

전환 후 이들은 전부 `user/dto/response/` 의 타입 **하나 또는 둘**로 수렴한다:
- `UserResponse` — 컨트롤러 반환용 전체 응답
- `UserSummary` — 다른 컨텍스트가 받는 축약형 (기존 `*Info` 들의 합집합)

규칙: **컨텍스트 간에 오가는 타입은 소유한 컨텍스트에 하나만 둔다.** 받는 쪽이 쓰는 필드가
제각각이면 합집합으로 만들고, 정말 다르면 `{X}Summary` / `{X}Detail` 둘까지만 둔다.
셋 이상으로 늘어나면 그건 서비스 분리 신호지 DTO 추가 신호가 아니다.

`{Ctx}InternalResponse` 는 `{Ctx}Summary` 로 개명한다. "Internal" 은 헥사고날 정문 구분에서
온 이름이라 MVC 에선 의미가 없다.

---

## 무엇을 지우면 안 되는가

구조를 바꾸는 작업이라 "이것도 간접층이네" 하고 함께 지우기 쉬운데, 아래 넷은 **아키텍처가
아니라 런타임 동작**이다. 패키지만 옮기고 형태는 그대로 둔다.

1. **이벤트 2단계 리스너 (Transactional Outbox)** — `@EventListener`(커밋 전 Outbox DB 저장)
   + `@TransactionalEventListener(AFTER_COMMIT)`(커밋 후 FCM 발송) + `NotificationScheduler`
   (60초 재시도). 셋을 합치거나 하나로 줄이지 않는다. 외부 호출은 AFTER_COMMIT + `@Async` 에만.
2. **`RedisNotificationPublisher` / `ChatMessageRedisPublisher` 분리** — 합치면 JDK 동적
   프록시에서 메서드가 사라져 Spring 부팅이 깨진다. 인터페이스 메서드를 지우지 않는다.
   (단, `NotificationDispatchPort` 는 포트 이름이므로 `NotificationDispatcher` 등으로 **개명은
   가능**하다. 없애는 것과 개명은 다르다.)
3. **soft delete** — §엔티티 참조.
4. **`@Transactional` 경계와 `@Async` 스레드풀** — 서비스를 합치면서 경계가 넓어지거나
   `readOnly` 가 잘못 걸리기 쉽다.

이 넷의 보존 여부는 `mvc-invariant-check` 스킬이 별도로 검증한다.

---

## 순환 의존

FetchAdapter 를 걷어내면 Service 가 Service 를 직접 주입한다. 이때 순환이 생기면
Spring 이 부팅 시점에 실패한다. **옮기기 전에** 예측하라:

```bash
python3 .claude/skills/mvc-migration/scripts/dep-graph.py
```

이 저장소에는 이미 순환이 하나 있다 — `board` 가 허브이고 `bookmark`·`chat`·`enroll` 과
각각 양방향이다. 끊는 방법은 `mvc-migration` 스킬의
`references/cycle-breaking.md` 에 있다.

---

## 변환 후 자기 점검

한 컨텍스트를 옮기고 나면 이걸 확인한다:

- [ ] `{ctx}/` 아래에 `adapter`·`application`·`domain` 디렉토리가 남아있지 않다
- [ ] `*UseCase`·`*FetchPort`·`*FetchAdapter`·`*CommandAdapter`·`*Info` 파일이 없다
- [ ] `{X}Entity` 이름이 없다 (엔티티는 `{X}`)
- [ ] 도메인 모델의 비즈니스 메서드가 전부 엔티티에 있다
- [ ] 통합 Service 의 쓰기 메서드에 `@Transactional` 이 붙어있다 (`readOnly` 상속 사고 방지)
- [ ] soft delete 대상 4종의 `deletedAt`·`@SQLRestriction`·`delete()` 가 살아있다
- [ ] `./gradlew compileJava` 통과
- [ ] `python3 .claude/hooks/mvc-validate-arch.py --scan` 통과
