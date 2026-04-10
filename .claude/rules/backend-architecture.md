---
trigger: glob
description: Backend Architecture Guide (Layers, Modules, Dependencies)
globs: "**/*.java"
---

# Backend Architecture Rules

## 1. 모듈 의존성 방향

의존성은 오직 **안쪽(Domain)**으로만 향합니다. 계층을 건너뛰는 호출은 금지합니다.

```
catchmate-api
  → catchmate-orchestration
    → catchmate-application
      → catchmate-domain
  → catchmate-authorization

catchmate-infrastructure → catchmate-domain
catchmate-common ← (모든 모듈이 참조)
catchmate-boot (전체 조립)
```

## 2. 계층별 책임 규칙

| 계층 | 모듈 | 책임 | 금지 사항 |
|:---|:---|:---|:---|
| **API** | `catchmate-api` | 요청 수신, DTO → Command 변환, 응답 반환 | 비즈니스 로직 작성 금지. Orchestrator만 호출. |
| **Orchestration** | `catchmate-orchestration` | 트랜잭션 관리, 여러 Service 조합, 이벤트 발행 | 도메인 로직 직접 구현 금지. |
| **Authorization** | `catchmate-authorization` | AOP 기반 권한 체크 | 비즈니스 로직 포함 금지. 순수 권한 검증만. |
| **Application** | `catchmate-application` | 단일 유스케이스 서비스, 도메인 이벤트 리스너 | 여러 도메인을 조율하는 로직 금지 (그건 Orchestrator 역할). |
| **Domain** | `catchmate-domain` | JPA Entity, Repository 인터페이스(Port), 도메인 모델 | Spring/Infrastructure 구현체 직접 참조 금지. |
| **Infrastructure** | `catchmate-infrastructure` | Repository 구현체(QueryDSL), 외부 연동(FCM, S3, Redis) | Domain 레이어의 Port 인터페이스만 구현. |
| **Common** | `catchmate-common` | 공통 Enum, ErrorCode, BaseException | 다른 모듈 참조 금지. |

## 3. 의존성 역전 (DIP)

- Domain 레이어에 `XxxRepository` 인터페이스(Port) 정의
- Infrastructure 레이어에 `XxxRepositoryImpl`이 이를 구현
- Application 서비스는 구현체가 아닌 **인터페이스**에만 의존

```java
// ✅ Domain (Port 정의)
public interface BoardRepository {
    Optional<Board> findById(Long id);
    Board save(Board board);
}

// ✅ Infrastructure (Port 구현)
@Repository
public class BoardRepositoryImpl implements BoardRepository { ... }

// ✅ Application (인터페이스만 주입)
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository; // 구현체 모름
}
```

## 4. 새 기능 추가 시 파일 생성 순서

1. `catchmate-domain` — 도메인 모델, Repository 인터페이스
2. `catchmate-common` — 필요한 Enum, ErrorCode 추가
3. `catchmate-application` — Service 클래스
4. `catchmate-infrastructure` — JPA Entity, QueryDSL Repository 구현체
5. `catchmate-orchestration` — Command/Response DTO, Orchestrator
6. `catchmate-api` — Controller, Request DTO
7. `catchmate-authorization` — 권한 어노테이션 (필요 시)
