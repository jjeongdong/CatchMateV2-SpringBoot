---
trigger: glob
description: Backend Architecture Guide (Hexagonal + DDD, Single Module)
globs: "**/*.java"
---

# Backend Architecture Rules

## 1. 단일 모듈 + Bounded Context 패키지

이 프로젝트는 **단일 Gradle 모듈** 안에서 Bounded Context 별로 패키지를 분리한 **Hexagonal (Ports & Adapters) + DDD** 구조입니다. 모듈 분리는 패키지 경계로 표현합니다.

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
├── global/                     # cross-cutting (config, authorization, redis, scheduler...)
└── common/                     # 모든 컨텍스트가 참조하는 공통 (ErrorCode, page, error...)
```

## 2. 헥사고날 의존성 방향

**바깥 → 안쪽 한 방향만 의존합니다.**

```
adapter/in/web (Controller)
   └─→ application/port/in (UseCase 인터페이스)         ◀ Input Port
            ↑ (implements)
       application/service (XxxApplicationService)
            └─→ application/port/out (Repository/외부 Port) ◀ Output Port
                 ↑ (implements)
                adapter/out/persistence | adapter/out/external

application/service (또는 domain/service)
   └─→ domain/model, domain/service
```

**금지된 의존성:**
- ❌ Controller → ApplicationService 구현체 직접 의존
- ❌ Application → Adapter (구현체) 직접 의존
- ❌ Domain → Spring / JPA / Infrastructure
- ❌ 한 컨텍스트의 Adapter / Repository 를 다른 컨텍스트가 직접 호출
- ❌ Common → 다른 패키지

## 3. 계층별 책임 / 금지 사항

| 계층 | 패키지 | 책임 | 금지 사항 |
|:---|:---|:---|:---|
| **Adapter In (Web)** | `{ctx}.adapter.in.web` | HTTP 요청 수신, Request → Command 변환, UseCase 호출 | 비즈니스 로직, 직접 Repository 호출, ApplicationService 구현체 import |
| **Adapter In (WebSocket)** | `{ctx}.adapter.in.websocket` | STOMP 이벤트 수신 → UseCase 호출 | 위와 동일 |
| **Input Port** | `{ctx}.application.port.in` | UseCase 인터페이스 정의 | 구현 코드 절대 X (interface 만) |
| **Application Service** | `{ctx}.application.service` | UseCase 구현, 트랜잭션 경계, 도메인 Service 조합, 이벤트 발행 | 도메인 로직 직접 구현 (도메인 Service 에 위임), JPA / Redis 직접 사용 |
| **Domain Service** | `{ctx}.domain.service` 또는 얇은 `{ctx}.application.service` 의 `XxxService` | Aggregate 한 컨텍스트 내부의 영속성/조회 + 단순 정책 | Spring 의존성 (선택), 다른 컨텍스트 Aggregate 직접 조작 |
| **Domain Model** | `{ctx}.domain.model` | Aggregate, Entity, Value Object, 도메인 이벤트 | Spring / JPA / Infrastructure 의존성 |
| **Output Port** | `{ctx}.application.port.out` | Repository / 외부 시스템 인터페이스 | 구현 코드 절대 X |
| **Adapter Out (Persistence)** | `{ctx}.adapter.out.persistence` | JPA Entity + Repository 구현 (QueryDSL 포함) | 도메인 모델로 변환하지 않은 채 Entity 누출 |
| **Adapter Out (External)** | `{ctx}.adapter.out.external` | FCM, S3, OAuth Client 등 | 비즈니스 결정 (구현체는 단순 어댑터) |
| **Global** | `global.*` | 보안, AOP 권한, Redis Pub/Sub 인프라, Scheduler, 전역 예외 핸들러 | 특정 도메인 로직 |
| **Common** | `common.*` | ErrorCode, BaseException, 페이지 유틸 등 공통 코드 | 다른 패키지 import |

## 4. 의존성 역전 (DIP)

모든 외부 호출은 **인터페이스 (Port)** 를 거칩니다.

```java
// ✅ application/port/out 에 인터페이스 정의
public interface BoardRepository {
    Optional<Board> findById(Long id);
    Board save(Board board);
}

// ✅ adapter/out/persistence 에 구현
@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final JpaBoardRepository jpa;
    private final QueryDslBoardRepository qdsl;
    @Override
    public Optional<Board> findById(Long id) {
        return jpa.findById(id).map(BoardEntity::toModel);
    }
}

// ✅ ApplicationService 는 인터페이스에만 의존
@Service
@RequiredArgsConstructor
public class BoardApplicationService implements BoardUseCase {
    private final BoardRepository boardRepository;  // 구현체 모름
}
```

## 5. Controller 는 UseCase 만 본다

```java
// ✅ Controller → UseCase 인터페이스
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardUseCase boardUseCase;        // ⭐ 인터페이스

    @PostMapping("/boards")
    public ResponseEntity<BoardCreateResponse> create(
            @AuthUser Long userId,
            @RequestBody @Valid BoardCreateRequest req) {
        return ResponseEntity.ok(boardUseCase.createBoard(userId, req.toCommand()));
    }
}

// ❌ Bad — 구현체 의존
private final BoardApplicationService boardApplicationService;
```

## 6. 컨텍스트 간 호출

다른 컨텍스트 기능이 필요하면:

```java
// ✅ 다른 컨텍스트의 UseCase 만 호출
@Service
@RequiredArgsConstructor
public class BoardApplicationService implements BoardUseCase {
    private final UserUseCase userUseCase;     // 다른 컨텍스트는 Input Port 통해
}

// ❌ Bad — 다른 컨텍스트 Repository 직접 호출
private final UserRepository userRepository;   // user 컨텍스트의 port.out
```

도메인 객체가 필요하면 다른 컨텍스트의 UseCase 가 도메인 모델을 반환하도록 설계하거나, ID 만 들고 다닙니다.

## 7. 새 기능 추가 시 파일 생성 순서

1. `{ctx}/domain/model/` — 도메인 모델 / Aggregate
2. `common/error/ErrorCode` — 필요한 에러 케이스
3. `{ctx}/application/port/out/` — Repository / 외부 Port 메서드 추가
4. `{ctx}/adapter/out/persistence/` — JPA Entity + Repository 구현
5. `{ctx}/application/dto/` — Command / Response DTO
6. `{ctx}/application/port/in/{Ctx}UseCase` — UseCase 인터페이스에 메서드
7. `{ctx}/application/service/{Ctx}ApplicationService` — 구현 + 트랜잭션
8. `{ctx}/adapter/in/web/controller/` — Controller, Request DTO
9. `global/authorization/` — 권한 어노테이션 (필요 시)
