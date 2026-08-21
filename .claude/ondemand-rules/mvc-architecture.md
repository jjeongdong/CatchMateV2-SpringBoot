# MVC 아키텍처 규칙 (전환 중)

> ⚠️ **이 저장소는 지금 헥사고날 → MVC 전환 중이다.** 컨텍스트마다 적용 규칙이 다르다.
>
> `.claude/mvc-migration-state.json` 의 `migrated` 목록에 있는 컨텍스트 → **이 문서의 규칙**
> 목록에 없는 컨텍스트 → **`backend-architecture.md` 의 헥사고날 규칙 그대로**
>
> 파일을 고치기 전에 그 컨텍스트가 어느 쪽인지 먼저 확인하라. `mvc-validate-arch.py` 훅이
> 컨텍스트별로 갈라서 검사하므로, 틀린 규칙으로 작성하면 편집이 차단된다.

## 목표 구조 — 기능별 패키지 3계층

Bounded Context 경계(`board`, `chat` …)는 **유지**하고 그 안을 평탄화한다.

```
com.back.catchmate.{ctx}/
├── controller/     @RestController, STOMP @MessageMapping
├── service/        @Service @Transactional — 유일한 비즈니스 계층
├── repository/     Spring Data JPA + QueryDSL
├── entity/         @Entity (= 도메인 모델. 별도 domain 패키지 없음)
├── dto/request/    컨트롤러 수신
│   /response/      컨트롤러 반환 + 컨텍스트 간 반환
├── event/          이벤트 + 리스너
└── infra/          FCM·S3·Redis 등 진짜 외부 연동 (있는 컨텍스트만)
```

`global/`, `common/` 은 계층이 아니라 공용 인프라다. 전환 대상이 아니다.

## 계층 방향 — 안쪽으로만

```
controller → service → repository → entity
```

역류 금지 (훅이 차단한다):
- `repository` 가 `service`/`controller` 를 import — repository 는 entity·dto 만 안다
- `service` 가 `controller` 를 import — 요청/응답 타입이 필요하면 dto 로 옮겨라
- `entity` 가 어떤 계층도 import 하지 않는다
- `controller` 가 `repository` 를 직접 import — service 를 건너뛰면 트랜잭션 경계가 사라진다
- `controller` 가 `entity` 를 직접 import — **엔티티가 HTTP 경계로 샌다.** dto 로 변환해라

마지막 항목이 MVC 에서 가장 흔한 붕괴 지점이다. 엔티티를 그대로 반환하면 지연 로딩이
직렬화 중에 터지고, 스키마 변경이 곧 API 변경이 된다.

## 컨텍스트 간 호출

```java
@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BoardService boardService;   // 상대 Service 를 직접 주입
}
```

- 남의 `service`·`dto`·`entity`·`event` 는 import 해도 된다.
- 남의 **`repository` 는 금지** — 상대의 트랜잭션·soft delete 규칙을 우회한다.
- 남의 **`controller` 는 금지** — HTTP 계층을 가로지른다.

**0-import 규칙은 폐기됐다.** 이제 남의 DTO 를 직접 쓴다. 대신 위 두 선을 지킨다.

### 노출 메서드 이름 규약

인터페이스가 사라져 "누가 부를 수 있는가" 를 구조로 막을 수 없다. 이름으로 표시한다:

```java
public ClubResponse getClub(Long clubId)        // 컨트롤러용 — 전체 응답
public ClubSummary getClubSummary(Long clubId)  // 다른 컨텍스트용 — 축약형
```

컨텍스트 간에 오가는 타입은 소유한 컨텍스트에 **하나 또는 둘**(`{X}Summary`/`{X}Detail`)만 둔다.
셋 이상으로 늘어나면 서비스 분리 신호지 DTO 추가 신호가 아니다.

## 순환 의존

Service 가 Service 를 직접 주입하므로 순환이 생기면 **Spring 부팅이 실패**한다.
컴파일은 통과하므로 컨텍스트 로딩까지 가야 발견된다.

```bash
python3 .claude/skills/mvc-migration/scripts/dep-graph.py
```

새 주입을 추가하기 전에 돌려라. 조립(응답 합치기)이 원인이면 조립 책임을
`{Ctx}ResponseAssembler` 로 올려 끊는다. 상세 → `.claude/skills/mvc-migration/references/cycle-breaking.md`

## 트랜잭션

```java
@Service
@Transactional(readOnly = true)      // 클래스 기본값
public class ClubService {
    @Transactional                    // 쓰기 메서드마다 재선언 — 빠뜨리면 조용히 저장 안 됨
    public ClubResponse createClub(...) { ... }
}
```

같은 클래스 안에서 `this.method()` 로 부르면 프록시를 안 타서 `@Transactional`·`@Async`·
`@Cacheable` 이 전부 무효화된다. 서비스를 합칠 때 특히 조심하라.

## 변하지 않는 것

아키텍처가 아니라 런타임 동작이라 MVC 로 와도 그대로다:

1. **이벤트 2단계 리스너 (Outbox)** — `@EventListener`(커밋 전 저장) +
   `@TransactionalEventListener(AFTER_COMMIT)`(커밋 후 FCM) + `NotificationScheduler`(재시도).
   합치지 않는다. 외부 호출은 AFTER_COMMIT + `@Async` 에만.
2. **`RedisNotificationPublisher` / `ChatMessageRedisPublisher` 분리** — 합치면 JDK 동적
   프록시에서 메서드가 사라져 부팅이 깨진다. 개명은 되지만 병합은 금지.
3. **soft delete** — `User`·`Board`·`ChatRoom`·`ChatMessage` 만 `deletedAt` +
   `@SQLRestriction("deleted_at IS NULL")` + `delete()`. 이 넷에 물리 삭제 금지.
   `Bookmark`·`Block`·`Enroll`·`ChatRoomMember`·`RefreshToken`·`NotificationOutbox` 는
   물리 삭제가 정상 — "일관성" 명목으로 soft delete 를 붙이지 마라.
4. **예외** — `BaseException(ErrorCode)`. `catch (Exception ignored) {}` 금지.
