# 순환 의존 끊기

FetchPort/FetchAdapter 간접층이 사라지면 Service 가 Service 를 직접 주입한다.
순환이 있으면 **Spring 이 부팅 시점에 실패**한다 (`BeanCurrentlyInCreationException`).
컴파일은 통과하므로 CI 에서 `bootRun`/컨텍스트 로딩 테스트까지 가야 발견된다.

먼저 현황을 본다:

```bash
python3 .claude/skills/mvc-migration/scripts/dep-graph.py          # 요약 + 순환 + 순서
python3 .claude/skills/mvc-migration/scripts/dep-graph.py --ctx board   # 한 컨텍스트 상세
```

스크립트는 간선을 세 종류로 나눈다. **순환 판정은 `injection` 간선만으로 한다** —
이벤트 구독은 `ApplicationEventPublisher` 를 사이에 두므로 빈 순환이 아니고,
DTO 타입 참조는 컴파일 의존이지 빈 의존이 아니다. 셋을 섞으면 있지도 않은 순환을 보고하게 된다.

---

## 이 저장소의 순환 (2026-08-21 기준)

`board` 가 허브다. `bookmark`·`chat`·`enroll` 과 각각 양방향이다.

```
board ⇄ bookmark
board ⇄ chat
board ⇄ enroll
enroll ⇄ bookmark
```

**방향마다 성격이 다르다는 게 해법의 열쇠다.**

| 방향 | 어디서 | 무엇을 |
|---|---|---|
| board → bookmark/chat/enroll | `BoardClientQueryService` 한 곳 | `isBookmarked`, `findChatRoomIdByBoardId`, `findEnrollByUserIdAndBoardId` — **응답 조립용 부가 정보** |
| bookmark/chat/enroll → board | 각 컨텍스트의 FetchAdapter | `getBoard`, `getBoards` — **순수 데이터 조회** |

역방향(→ board)은 정당한 데이터 의존이라 끊을 수 없다. 끊어야 할 것은 정방향이고,
정방향은 board 의 **비즈니스 로직이 아니라 화면 조립**이다. 조립은 board 서비스의 일이 아니다.

---

## 권장 해법 — 조립을 상위로 올린다

`BoardResponseAssembler` 가 이미 있다 (지금은 user·club·game 만 주입). 여기로 조립 책임을 모은다.

```
[전환 전]                          [전환 후]
BoardClientQueryService            BoardService              (순수 — board 데이터만)
  ├─ BookmarkFetchPort               ↑
  ├─ ChatRoomFetchPort             BoardResponseAssembler    (조립 — 여럿을 주입)
  ├─ EnrollFetchPort                 ├─ BoardService
  └─ BoardResponseAssembler          ├─ BookmarkService
                                     ├─ ChatService
BookmarkService → BoardService       ├─ EnrollService
  (순환!)                            └─ User/Club/GameService

                                   BookmarkService → BoardService   (순환 없음)
```

1. `BoardService` 에서 bookmark·chat·enroll 주입을 **전부 뺀다.** board 는 자기 데이터만 안다.
2. 빠진 호출을 `BoardResponseAssembler` 로 옮긴다. 조립기는 `BoardService`·`BookmarkService`·
   `ChatService`·`EnrollService` 를 모두 주입한다.
3. `BoardController` 는 목록·상세 조회에서 `BoardResponseAssembler` 를 호출하고,
   생성·수정·삭제는 `BoardService` 를 직접 호출한다.
4. `BookmarkService`·`ChatService`·`EnrollService` 는 `BoardService` 를 주입한다 (그대로).

빈 그래프에 순환이 없다 — 조립기를 향하는 화살표는 컨트롤러뿐이고, 조립기는 아무에게도
주입되지 않는다.

`enroll ⇄ bookmark` 도 같은 방식으로 본다. `EnrollService` 가 bookmark 를 부르는 곳이
조립이면 조립기로, 진짜 비즈니스 판단이면 아래 다른 전술을 쓴다.

**주의:** 조립기는 트랜잭션 경계를 넓히기 쉽다. `@Transactional(readOnly = true)` 를 조립기에
걸면 여러 서비스 호출이 한 커넥션을 오래 점유한다. 조립기에는 트랜잭션을 걸지 말고,
각 서비스가 자기 트랜잭션을 열고 닫게 둔다.

---

## 다른 전술 (조립이 아닐 때)

**이벤트로 뒤집기** — A 가 B 를 부르는 이유가 "B 에게 뭔가 시키려고" 이면, A 는 이벤트만
발행하고 B 가 구독한다. 주입 간선이 사라진다. 이미 `EnrollAcceptedEvent`·`BoardCompletedEvent`
로 쓰고 있는 패턴이다. **단, 즉시 결과가 필요한 조회에는 쓸 수 없다.**

**조회 전용 서비스 분리** — B 를 `BService`(전체)와 `BQueryService`(조회 전용, 아무도 주입 안 함)로
나누고 A 는 후자만 주입한다. 서비스 수가 늘어나므로 순환이 한두 개일 때만 쓴다.

**데이터 소유권 재배치** — `isBookmarked(userId, boardId)` 처럼 어느 쪽 데이터인지 애매한
질의는, 애초에 소유가 잘못됐다는 신호일 수 있다. 다만 이건 스키마 변경이라 마이그레이션 중에
같이 하지 않는다. **기록만 남기고 전환 후로 미룬다.**

**`@Lazy` — 최후의 수단.** 순환을 없애는 게 아니라 부팅 실패만 미룬다. 프록시가 끼면
트랜잭션 전파와 스택 트레이스가 나빠진다. 쓰기로 했다면 왜 다른 방법이 안 되는지를 주석으로
남긴다. `@Lazy` 로 넘어간 순환은 리포트에 반드시 적는다 — 조용히 넘어가면 다음 사람이 못 본다.

---

## 순서

`dep-graph.py` 의 "변환 순서 제안" 은 주입 간선의 위상 정렬이다. leaf 부터 옮기면
옮기는 시점에 상대가 이미 MVC 라 참조를 한 번만 고친다.

현재 순서: `club` → `report` → `game` → `user` → `auth` → `notice` → `inquiry` → `oauth`
→ (순환 해소) → `board`·`bookmark`·`chat`·`enroll` → `admin`·`notification`

`admin`·`notification` 은 순환 당사자가 아니라 **순환에 의존해서 대기**하는 것뿐이다.
앞의 고리가 풀리면 순서대로 이어서 옮길 수 있다.

순환 4개는 함께 옮겨야 한다. 하나만 옮기면 나머지 셋이 아직 UseCase 인터페이스를 노출하고
있어서 중간 상태가 컴파일되지 않는다. **이 넷은 한 라운드로 묶는다.**
