---
name: mvc-invariant-check
description: >-
  헥사고날 → MVC 전환에서 **패키지는 옮겨졌는데 런타임 동작이 조용히 바뀐 것**을 잡아낸다.
  컴파일도 통과하고 테스트도 초록인데 프로덕션에서만 깨지는 부류 — 이벤트 2단계 리스너(Outbox)가
  한 단계로 합쳐짐, Redis 퍼블리셔 둘이 병합돼 프록시가 깨짐, soft delete 가 물리 삭제로 바뀜,
  서비스 통합 중 쓰기 메서드가 readOnly 트랜잭션을 물려받음, AFTER_COMMIT 외부 호출이 커밋 전으로
  당겨짐, @AuthUser 소유권 검사가 사라짐. 다음 상황에 사용한다 — "전환한 거 검증해줘",
  "동작 안 바뀌었는지 봐줘", "불변식 확인", 한 컨텍스트를 MVC 로 옮긴 직후, 서비스를 합친 뒤,
  엔티티와 도메인 모델을 병합한 뒤. 구조·계층 규칙 위반은 mvc-validate-arch 훅이 이미 잡으므로
  중복 보고하지 않는다.
---

# MVC 전환 불변식 검증

구조 변경은 컴파일러가 지켜준다. **이 스킬이 보는 것은 컴파일러가 못 보는 쪽**이다 —
패키지가 제자리로 갔는데 실행 순서·트랜잭션 경계·삭제 방식이 바뀐 경우.

전환 전후를 **반드시 대조**한다. 변환 후 코드만 읽으면 "원래 이랬나 보다" 로 넘어간다.

```bash
git diff HEAD -- src/main/java/com/back/catchmate/{ctx}/    # 전환 diff
git show HEAD:<원래경로>                                      # 원본 파일 원문
```

---

## 1. 이벤트 2단계 리스너 (Outbox) — 최우선

알림은 3단계로 나뉘어 있고, 나뉜 데 이유가 있다. 합치면 커밋되지 않은 트랜잭션의 알림이
발송되거나, 발송 실패가 영구 유실된다.

| 단계 | 형태 | 하는 일 |
|---|---|---|
| 1 | `@EventListener` | 커밋 **전**, 같은 트랜잭션에서 Outbox 행 저장 |
| 2 | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | 커밋 **후** FCM 발송 |
| 3 | `NotificationScheduler` (60초) | 실패분 재시도 |

확인:
- 세 단계가 여전히 **세 개의 별도 메서드/클래스**인가? 하나로 합쳐지지 않았는가?
- `@TransactionalEventListener` 의 `phase` 가 `AFTER_COMMIT` 그대로인가?
  (기본값은 `AFTER_COMMIT` 이지만, 명시돼 있던 것이 지워졌다면 왜인지 확인)
- **외부 호출(FCM·S3·HTTP)이 `@EventListener` 쪽으로 옮겨가지 않았는가?**
  서비스를 합치는 과정에서 리스너 메서드가 뒤섞이기 쉽다.
- `@Async` 가 유지되는가? 떼면 커밋 후 응답이 FCM 지연만큼 늘어난다.

## 2. Redis 퍼블리셔 두 개의 분리

`RedisNotificationPublisher`(`NotificationDispatchPort` 구현)와
`ChatMessageRedisPublisher`(`@TransactionalEventListener`)는 **의도적으로 분리**돼 있다.
합치면 JDK 동적 프록시에서 메서드가 사라져 Spring 부팅이 깨진다.

확인:
- 두 클래스가 여전히 별개인가? "둘 다 Redis 발행이니 합치자" 가 되지 않았는가?
- 인터페이스 메서드가 지워지지 않았는가?
- 개명은 괜찮다 (`NotificationDispatchPort` → `NotificationDispatcher`). **병합이 금지**다.

## 3. Soft delete

`User`·`Board`·`ChatRoom`·`ChatMessage` **넷만** soft delete 다. 엔티티를 병합하면서
`@SQLRestriction` 이 빠지면 삭제된 데이터가 전부 조회에 다시 나타난다.

확인 (이 4개 엔티티에 대해):
- `deletedAt` 필드가 살아있는가?
- `@SQLRestriction("deleted_at IS NULL")` 이 클래스에 붙어있는가?
- 도메인 모델에 있던 `delete()`(deletedAt 세팅)가 엔티티로 옮겨왔는가?
- `deleteById`·`delete(entity)`·`@Modifying DELETE` 가 새로 생기지 않았는가?

반대 방향도 본다 — `Bookmark`·`Block`·`Enroll`·`ChatRoomMember`·`RefreshToken`·
`NotificationOutbox` 는 **물리 삭제가 정상**이다. 전환하면서 "일관성" 명목으로 soft delete 를
붙이면 유니크 제약이 깨지고 토큰이 남는다.

## 4. 트랜잭션 경계 — 서비스 통합의 대표 사고

`{Ctx}ClientQueryService`(`readOnly = true`)와 `{Ctx}ClientCommandService` 를 합치면,
클래스 레벨 `@Transactional(readOnly = true)` 를 쓰기 메서드가 물려받는다.
**저장이 조용히 무시되거나 드라이버에 따라 런타임에 터진다.**

확인:
- 통합 Service 의 **모든 쓰기 메서드**(save/update/delete/상태전이)에 `@Transactional` 이
  메서드 레벨로 재선언돼 있는가?
- 원래 트랜잭션이 걸려있지 않던 메서드에 새로 경계가 생기지 않았는가?
  (Reader 흡수 과정에서 조회가 쓰기 트랜잭션 안으로 들어가면 커넥션 점유가 길어진다)
- `propagation`·`timeout`·`isolation` 을 명시했던 메서드가 있었다면 그대로 옮겨졌는가?

## 5. 프록시 self-invocation

서비스를 합치면 **원래 다른 빈이었던 호출이 같은 클래스 내부 호출로 바뀐다.**
`this.method()` 는 프록시를 타지 않으므로 `@Transactional`·`@Async`·`@Cacheable` 이
전부 무효화된다. 컴파일도 되고 테스트도 통과하는데 애노테이션만 죽는다.

확인:
- 합쳐진 Service 안에서 `@Transactional`/`@Async`/`@Cacheable` 이 붙은 **public 메서드를
  같은 클래스의 다른 메서드가 호출**하는 곳이 있는가?
- 있다면 그 애노테이션은 지금 죽어있다. 별도 빈으로 분리하거나 자기 주입이 필요하다.

## 6. 인가·소유권 검사

`@AuthUser` 로 받은 userId 와 리소스 소유자를 대조하던 검사가, 서비스를 합치면서
중복으로 보여 지워지기 쉽다.

확인:
- 전환 전 서비스에 있던 소유권 검사(`if (!board.getUserId().equals(userId)) throw ...`)가
  전부 남아있는가?
- STOMP 인터셉터(`StompAuthChannelInterceptor`)의 구독·전송 인가가 그대로인가?
- AOP 권한 검사(`global/authorization`)가 참조하던 UseCase 인터페이스가 삭제됐다면,
  Service 로 갈아끼우면서 검사가 통째로 빠지지 않았는가?

## 7. 이벤트 페이로드

이벤트 클래스를 `event/` 로 옮기면서 필드가 늘거나 줄면 구독자가 조용히 깨진다.
FetchAdapter 를 지우는 과정에서 "이제 상대 DTO 를 직접 쓰니까" 하고 이벤트에 엔티티를
실어 보내는 변경이 특히 위험하다 — 트랜잭션 밖에서 지연 로딩이 터진다.

확인:
- 이벤트 record 의 필드가 전환 전과 동일한가?
- 이벤트에 **엔티티가 실려있지 않은가**? (id + 스칼라 값만 실어야 한다)

---

## 보고 형식

발견마다 이렇게 적는다. **오탐 0 이 우선**이고, 확신이 없으면 "확인 필요" 로 낮춰 적는다.

```
🔴 [불변식 N] 파일:줄
   무엇이 바뀌었나: (전환 전 → 전환 후)
   런타임에 무슨 일이 일어나나: (구체적으로)
   근거: git show HEAD:<원본경로> 의 해당 부분
```

등급: 🔴 프로덕션에서 깨짐 · 🟡 동작은 하나 의도가 바뀜 · ⚪ 확인 필요

불변식 위반이 하나도 없으면 그렇게 말한다. 없는 문제를 만들지 않는다.
