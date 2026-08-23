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

**정적 팩토리와 불변식은 엔티티에 남긴다.** `static Report createReport(...)` 가 내부에서
`BaseException(ErrorCode.CANNOT_REPORT_SELF)` 를 던지는 식이면 그대로 엔티티로 옮긴다.
MVC 로 간다고 도메인 로직을 서비스로 끌어낼 이유는 없다 — 옮기는 것은 패키지 구조지 설계 철학이
아니다. 로직 이동을 최소화할수록 "동작이 같다" 를 증명하기 쉽다.

**⚠️ `BaseTimeEntity` 를 상속하는 엔티티의 빌더 함정.** 도메인 정적 팩토리에 
`.createdAt(LocalDateTime.now())` 같은 줄이 있으면 **그대로 옮기면 컴파일이 깨진다.** Lombok
`@Builder` 는 자기 클래스 필드만 포함하는데 `createdAt` 은 상위 클래스(`BaseTimeEntity`)의
`@CreatedDate` 필드이기 때문이다. 그 줄을 **삭제하고 JPA Auditing 에 맡긴다.**

이건 동작 변경이 아니다 — 전환 전에도 `{X}Entity.from(domain)` 이 `createdAt` 을 복사하지 않아
도메인이 찍은 `now()` 는 저장 시점에 이미 버려지고 있었다. 다만 전환 후 응답 DTO 의 `createdAt`
이 null 로 나가지 않는지 **실호출로 한 번 확인**하라 (`save()` 직후 `@PrePersist` 가 채운다).

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

분리 기준은 **애그리거트 먼저, 그다음 규모**다.

**1단계 — 애그리거트별로 가른다.** 한 컨텍스트에 애그리거트가 여럿이면 `{Ctx}Service` 하나로
합치지 않는다. 예: `user` 에는 `User`·`Block`·`UserOnlineStatus` 셋이 있고 서비스가 11개다.
전부 합치면 god class 가 되고, 서로 다른 테이블·수명주기·트랜잭션 특성이 한 클래스에 섞인다.
→ `UserService` · `BlockService` · `UserOnlineStatusService`.

애그리거트 판별은 **엔티티 기준**이다. `{ctx}/entity/` 에 남을 엔티티(조인·토글 엔티티 포함)마다
그것을 주로 다루는 서비스가 하나씩 있으면 그 경계로 가른다. 엔티티 하나에 서비스 하나가
기계적으로 대응한다는 뜻은 아니고, **원래 `{Aggregate}ClientQueryService` 처럼 접두사가
갈려 있던 것을 그 접두사대로 묶으라**는 것이다.

**2단계 — 애그리거트 안에서 Client/Internal/Admin 을 합친다.**
`{Agg}ClientQueryService` + `{Agg}ClientCommandService` + `{Agg}InternalQueryService` +
`{Agg}InternalCommandService` + `{Agg}AdminQueryService` → **`{Agg}Service` 하나.**
`{Agg}Reader` 는 그 안의 private 로 흡수한다.

**3단계 — 그래도 크면 규모로 쪼갠다.**
합친 결과가 **public 메서드 20개 초과 또는 400줄 초과**면 축 하나로만 쪼갠다 —
`{Agg}CommandService` / `{Agg}QueryService`. Client/Internal 축으로는 쪼개지 않는다.

`{Ctx}Assembler` 는 합치지 말고 그대로 둔다. 응답 조립은 별개 관심사다.

### 자기 컨텍스트 안의 포트도 지운다

`application/port/out/external/` 에는 다른 컨텍스트를 부르는 FetchPort 말고 **자기 인프라를
가리키는 포트**도 있다 (예: `auth` 의 `TokenProvider` → `adapter/out/provider/JwtTokenProvider`).
이것도 의존 역전을 위한 간접층이므로 **포트를 지우고 구현체를 `infra/` 로 옮겨 직접 주입**한다.

구현이 하나뿐인 인터페이스는 MVC 에서 남길 이유가 없다. 다만 **구현이 실제로 둘 이상이거나
`@Profile`·`@ConditionalOnProperty` 로 갈아끼우고 있다면 인터페이스를 남긴다** — 그건 의존
역전이 아니라 실제 다형성이다. 지우기 전에 구현체 개수를 세어라.

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

**어느 쪽이 옮겨지느냐로 처리가 정반대다.** 헷갈리면 라운드가 망가진다:

| 상황 | 내 FetchPort·FetchAdapter·`*Info` |
|---|---|
| **내가** 옮겨진다 (상대는 아직 헥사고날) | **삭제** — 내 Service 가 상대 UseCase 를 직접 주입 |
| **내가** 옮겨진다 (상대도 이미 MVC) | **삭제** — 내 Service 가 상대 `{Ctx}Service` 를 직접 주입 |
| **상대가** 옮겨진다 (나는 아직 헥사고날) | **존치** — 어댑터 안의 주입 타입·반환 타입·메서드명만 교체 (아래 §전환 중) |

즉 **어댑터는 그 소유자가 옮겨질 때 사라진다.** 상대가 먼저 옮겨졌다고 내 어댑터를 걷어내면,
아직 헥사고날인 내 구조가 절반만 무너진다.

1. `FetchPort` 인터페이스, `FetchAdapter` 구현, `port/out/dto/*Info` 세 개를 모두 삭제한다.
2. 호출부는 상대 `{Ctx}Service` 를 생성자 주입하고, 상대의 `dto/response` 타입을 직접 쓴다.
3. FetchAdapter 안에 있던 **필드 매핑 로직은 버려진다** — 같은 필드를 이름만 바꿔 담던 코드라
   대부분 순수 손실이다. 다만 어댑터가 필터링·기본값·널 처리를 하고 있었다면 그것은
   호출부나 상대 서비스로 옮겨야 한다. 어댑터를 지우기 전에 `map(...)` 안을 읽어라.

**0-import 규칙은 여기서 폐기된다.** 이제 `bookmark` 가 `board.dto.response.BoardSummary` 를
직접 import 한다. 그 대신 지켜야 할 선이 둘 남는다 — 남의 `repository` 와 남의 `controller` 는
import 하지 않는다. (MVC 검증 훅이 이 둘을 막는다.)

### 전환 중 — 아직 안 옮긴 컨텍스트가 호출자일 때

**한 라운드 작업량의 절반은 옮기는 컨텍스트가 아니라 그것을 부르던 컨텍스트다.** club 은
피의존 9개, `user` 는 10개다. 이 호출자들은 아직 헥사고날이므로 **자기 FetchPort/FetchAdapter
체인을 그대로 유지한다.** 어댑터 안에서 바꾸는 것은 **정확히 3가지**뿐이다:

```java
// admin/adapter/out/external/AdminClubFetchAdapter.java  (admin 은 아직 헥사고날)
-import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;   // 1
-import com.back.catchmate.club.application.dto.response.ClubInternalResponse;  // 2
+import com.back.catchmate.club.service.ClubService;
+import com.back.catchmate.club.dto.response.ClubSummary;

-    private final ClubInternalQueryUseCase clubInternalQueryUseCase;           // 3
+    private final ClubService clubService;
```
그리고 호출 메서드명 정정 (`getClub` → `getClubSummary` 등).

**그 외에는 한 글자도 바꾸지 않는다.** `XxxFetchPort` 인터페이스, `Xxx{Ctx}Info` DTO,
어댑터의 `map()` 필드 매핑, 방어용 널 가드, private 헬퍼 이름 — 전부 그대로 둔다.
**호출자 컨텍스트의 service 코드는 한 줄도 바뀌지 않아야 한다.** 파라미터 타입과 반환
컨테이너(단건/`List`/`Optional`)가 동일하면 변경이 어댑터 안에서 전부 흡수되기 때문이다.
service 가 바뀌었다면 시그니처를 잘못 바꿨다는 신호다.

어댑터·포트·`*Info` 제거는 **그 컨텍스트 자신의 라운드**에서 한다. 지금 걷어내면 아직
헥사고날인 컨텍스트의 구조가 절반만 무너진 상태가 된다.

> 검증기는 이 상황을 알고 있다 — 미전환 컨텍스트의 `adapter/out/external`·`adapter/in/event`
> 에서 전환된 컨텍스트의 `service`·`dto`·`entity`·`event` 를 부르는 것만 허용하고,
> 미전환 컨텍스트의 **service** 가 전환된 상대를 직접 부르면 차단한다 (자기 어댑터를 창구로
> 써야 한다). 상세 → `mvc-guardrail-switch` 스킬.

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

**`{Ctx}Response` 는 실제 사용처가 있을 때만 만든다.** 컨트롤러에 조회 경로가 없는 컨텍스트
(예: 생성 엔드포인트만 있는 `report`)에 `{Ctx}Response` 를 선제적으로 만들면 사용처 0인 죽은
타입이 된다. "Response 와 Summary 를 둘 다 둔다" 는 **양쪽 사용처가 실재할 때** 적용된다.

**개명 규약이 적용되는 범위.** cross-context 노출 메서드의 개명(`getX` → `getXSummary`)은
**`{Ctx}Summary` 를 반환할 때만** 쓴다. `long`·`void`·`boolean` 을 반환하는 메서드
(`getPendingReportCount()`, `processReport()`)는 반환 타입이라는 표식 자체가 없으므로 **이름을
그대로 둔다.** 억지로 `Summary` 를 붙이면 이름이 거짓말을 하고, 호출부 변경도 "정확히 3가지" 를
넘는다.

### 양방향 DTO 는 `dto/` 평탄 배치

`request/`·`response/` 는 **HTTP 방향**을 가리키는 이름이다. 컨텍스트 간에 **인자로도 반환으로도**
쓰이는 타입(예: `auth` 의 `SignupTokenPayload` — oauth 가 넘기기도 하고 받기도 한다)은 어느
쪽에 넣어도 이름이 절반은 거짓말이 된다. **`dto/` 바로 아래**에 둔다.

`dto/command/` 는 컨트롤러에서 서비스로 내려가는 것에만 쓴다. cross-context 인자는 command 가
아니다.

### JPA 가 아닌 영속성은 `infra/`

`repository/` 는 **Spring Data JPA 전용**이다. Redis·외부 저장소를 다루는 클래스는 이름에
`Repository` 가 들어가더라도 `infra/` 로 보낸다.

이건 취향이 아니라 부팅 안전 문제다. `repository/{X}RepositoryImpl` 이라는 조합을 만들면
Spring Data 가 그것을 `{X}Repository` 의 custom fragment 로 스캔하는데, 대응하는
`{X}RepositoryCustom` 이 없으면 **부팅이 깨진다.** 이름도 함께 바꿔 오해를 없앤다 —
`RefreshTokenRepositoryImpl` → `infra/RefreshTokenRedisRepository`.

### 주석이 전환으로 거짓이 되면 고친다

"데드코드는 옮기되 삭제 금지" 는 **코드**에 대한 규칙이다. 주석이 사라진 클래스를 가리키거나
폐기된 규칙(0-import 등)을 설명하고 있으면, 그 문장은 **적극적으로 오해를 유발**하므로
사실에 맞게 고친다. 주석 전체를 지우지는 말고 틀린 문장만 고친다.

### enum 배치

컨텍스트의 enum 이 **2개 이하면 `entity/` 에 평탄하게**, **3개 이상이면 `entity/enums/`** 로 묶는다.
enum 하나 때문에 디렉토리를 파면 대부분의 컨텍스트에서 파일 하나짜리 폴더가 생기고, 반대로
`board`·`enroll` 처럼 여럿인 곳은 엔티티가 enum 에 묻힌다.

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
