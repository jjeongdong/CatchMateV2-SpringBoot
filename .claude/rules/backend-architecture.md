---
trigger: glob
description: Backend Architecture Guide (Hexagonal + DDD, Single Module)
globs: "**/*.java"
---

# Backend Architecture Rules

## 1. 단일 모듈 + Bounded Context 패키지

```
com.back.catchmate
├── {context}/                  # board, chat, enroll, user, auth, oauth,
│                               # notification, inquiry, report, admin,
│                               # bookmark, club, game, notice
│   ├── domain/
│   ├── application/
│   │   ├── port/{in,out}/
│   │   ├── service/
│   │   ├── event/
│   │   └── dto/
│   └── adapter/{in,out}/...
├── global/                     # cross-cutting
└── common/                     # ErrorCode, page, error
```

## 2. 헥사고날 의존성 방향

**바깥 → 안쪽 한 방향만.**

```
adapter/in/web (Controller)
   └─→ application/port/in (UseCase 인터페이스)        ◀ Input Port
            ↑ (implements)
       application/service ({Ctx}Service)
            ├─→ application/port/out (own Repository)  ◀ Output Port
            │        ↑ (implements)
            │       adapter/out/persistence
            │
            └─→ application/port/out (XxxFetchPort)    ◀ Cross-context Output Port
                     ↑ (implements)
                    adapter/out/external (Fetch Adapter → other context's Service)
```

**금지된 의존성:**
- ❌ Controller → Service 구현체 직접 의존
- ❌ Service → 다른 컨텍스트의 Service / Repository 직접 의존
- ❌ Application → Adapter 구현체 직접 의존
- ❌ Domain → Spring / JPA / Infrastructure
- ❌ Common → 다른 패키지

## 3. 계층별 책임

| 계층 | 패키지 | 책임 |
|:---|:---|:---|
| **Adapter In (Web)** | `{ctx}.adapter.in.web` | HTTP 요청 수신, Request → Command, UseCase 호출 |
| **Adapter In (WebSocket)** | `{ctx}.adapter.in.websocket` | STOMP 이벤트 수신 → UseCase 호출 |
| **Input Port** | `{ctx}.application.port.in` | UseCase 인터페이스 (interface only) |
| **Service** | `{ctx}.application.service` | UseCase 구현, 트랜잭션 경계, 도메인 모델 조작, own Repository 직접 호출, Fetch Port 호출, 이벤트 발행 |
| **Domain Model** | `{ctx}.domain.model` | Aggregate, Entity, Value Object, 도메인 이벤트 |
| **Domain Service** | `{ctx}.domain.service` | 도메인 규칙이 한 Aggregate에 안 들어갈 때 |
| **Output Port** | `{ctx}.application.port.out` | Repository + 다른 컨텍스트용 `XxxFetchPort` 인터페이스 |
| **Adapter Out (Persistence)** | `{ctx}.adapter.out.persistence` | JPA Entity, Repository 구현 |
| **Adapter Out (External)** | `{ctx}.adapter.out.external` | FCM, S3, OAuth Client + Fetch Port 구현 (다른 컨텍스트 Service 래핑) |
| **Global** | `global.*` | Security, AOP 권한, Redis Pub/Sub 인프라, Scheduler, 전역 예외 핸들러 |
| **Common** | `common.*` | ErrorCode, BaseException, 페이지 유틸 |

## 4. Cross-context 호출은 Fetch Port (⭐ 핵심)

같은 컨텍스트는 Repository를 직접 호출. 다른 컨텍스트는 자기 `application/port/out/` 에 정의한 Port 통해서만.

```java
// ✅ board/application/port/out/UserFetchPort.java
public interface UserFetchPort {
    User getUser(Long userId);
}

// ✅ board/adapter/out/external/BoardUserFetchAdapter.java
@Component
@RequiredArgsConstructor
public class BoardUserFetchAdapter implements UserFetchPort {
    private final UserService userService;          // 다른 컨텍스트 의존은 여기에만 격리
    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }
}

// ✅ board/application/service/BoardService.java
@Service
@RequiredArgsConstructor
public class BoardService implements BoardUseCase {
    private final BoardRepository boardRepository;  // own — 직접
    private final UserFetchPort userFetchPort;      // cross — Port 통해

    @Override
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        User user = userFetchPort.getUser(userId);  // 다른 컨텍스트 구체화 모름
        Board saved = boardRepository.save(...);
        return BoardCreateResponse.of(saved.getId());
    }
}

// ❌ Bad — 다른 컨텍스트 Service 직접 주입
private final UserService userService;
```

**예외**: 같은 컨텍스트 안에 여러 Aggregate가 있을 때 (e.g., chat의 ChatRoomService, ChatMessageService 등). 이들은 같은 경계 안의 협력자이므로 직접 호출 OK.

## 5. Controller는 UseCase 인터페이스만 의존

```java
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardUseCase boardUseCase;        // ⭐ 인터페이스

    @PostMapping("/boards")
    public ResponseEntity<BoardCreateResponse> create(...) {
        return ResponseEntity.ok(boardUseCase.createBoard(...));
    }
}

// ❌ Bad
private final BoardService boardService;
```

## 6. 의존성 역전 (DIP)

```java
// ✅ application/port/out 에 인터페이스
public interface BoardRepository {
    Optional<Board> findById(Long id);
    Board save(Board board);
}

// ✅ adapter/out/persistence 에 구현
@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final JpaBoardRepository jpa;
    @Override
    public Optional<Board> findById(Long id) {
        return jpa.findById(id).map(BoardEntity::toModel);
    }
}
```

## 7. 새 기능 추가 시 파일 생성 순서

1. `{ctx}/domain/model/` — 도메인 모델
2. `common/error/ErrorCode` — 새 에러 케이스
3. `{ctx}/application/port/out/` — Repository / Fetch Port 인터페이스
4. `{ctx}/adapter/out/persistence/` — JPA Entity + Repository 구현
5. `{ctx}/adapter/out/external/` — Fetch Port 구현 (필요 시)
6. `{ctx}/application/dto/` — Command / Response DTO
7. `{ctx}/application/port/in/{Ctx}{Client/Internal}{Command/Query}UseCase` — UseCase 인터페이스
8. `{ctx}/application/service/{Ctx}Service` — 구현
9. `{ctx}/adapter/in/web/controller/` — Controller, Request DTO
10. `global/authorization/` — 권한 어노테이션 (필요 시)

## 8. Cross-context 의존성은 모두 Fetch Port

모든 컨텍스트가 Fetch Port 패턴을 사용합니다. Service 파일에 다른 컨텍스트의 `XxxService` 를 `import` 하면 잘못된 신호입니다. 새 cross-context 호출이 필요하면:

1. 자기 `application/port/out/{Source}FetchPort.java` 에 인터페이스 정의
2. 자기 `adapter/out/external/{OwnCtx}{Source}FetchAdapter.java` 에 어댑터 추가
3. Service 가 Port 만 주입
