# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **이 파일은 매 세션 항상 로드되는 얇은 인덱스입니다.** 상세 규칙은 `.claude/ondemand-rules/*.md`가 단일 출처(SSOT)이며, **Java 파일을 Read/Edit/Write 할 때 PreToolUse 훅(`.claude/hooks/pretooluse-inject-rules.py`)이 세션당 1회 자동 주입**합니다 (Java 작업이 없는 세션엔 로드 안 됨 → 토큰 절약). 단, '절대 변경 금지' 핵심 3개는 `SessionStart` 훅(`.claude/hooks/sessionstart-guardrails.py`)이 **모든 세션 시작 시 항상** 주입합니다. 훅 등록은 `.claude/settings.json`:
> - `backend-architecture.md` — 헥사고날 의존성·UseCase 정문·Fetch Port·DTO 격리(0-import)
> - `backend-coding-conventions.md` — 네이밍·예외·트랜잭션·필드 순서·import 방향
> - `backend-patterns.md` — 이벤트 2단계 리스너·Outbox·AOP 권한·QueryDSL·Redis
>
> **보조 자산**: `.claude/skills/`(add-notification·cross-context-access·hexagonal-review), `.claude/agents/hexagonal-reviewer.md`, 그리고 작업 종료 시 빌드를 자동 검증하는 `Stop` 훅(`stop-build-gate.py`).

## Build & Run Commands

```bash
./gradlew build          # 빌드
./gradlew bootJar        # 실행 가능 JAR
./gradlew test           # 전체 테스트
./gradlew archCheck      # 헥사고날 아키텍처 규칙 검사 (check 에 포함)
docker-compose up -d     # 로컬 개발 (Docker Compose)
```

## Architecture: Hexagonal + DDD (Single Module)

**단일 Gradle 모듈** 안에서 Bounded Context(`board`, `user` 등) 별로 패키지를 나눈 **Hexagonal Architecture (Ports & Adapters)** 구조다. 의존성은 항상 **바깥 → 안 한 방향**으로만 흐른다: `adapter` → `application(port/service)` → `domain`. 반대 방향(안→밖) import 는 금지.

Java 코드를 작성·수정할 땐 아래 **불변 규칙 3개**를 반드시 지킨다 (상세·예시 → `backend-architecture.md`):

1. **정문은 UseCase 인터페이스뿐** — 컨텍스트 외부(Controller·다른 컨텍스트·Scheduler·AOP)는 `{Ctx}...UseCase` 인터페이스로만 진입한다. 외부에서 Service 구체 클래스·Repository·Reader·JPA Entity 를 주입/import 하면 **안 된다**.
2. **Cross-context 는 Fetch Port 로만** — 다른 컨텍스트가 필요하면 `자기 port/out/XxxFetchPort` → `자기 adapter/out/external/XxxFetchAdapter` → `상대 정문 UseCase` 체인을 거친다. 정문은 호출자 목적에 맞는 **Internal 계열**: 읽기 `XxxInternalQueryUseCase`, 쓰기 `XxxInternalCommandUseCase` (admin 컨텍스트만 `XxxAdminQueryUseCase`). Controller 전용 `Client` 계열로 진입 금지, Service 끼리 직접 의존 **금지**.
3. **외부 도메인 0-import** — 다른 컨텍스트의 도메인 모델/enum 을 자기 시그니처(Port·Service·Event·DTO)에 노출하지 않는다. 자기 컨텍스트의 record DTO 또는 String 으로 격리한다.

```
com.back.catchmate
├── {context}/                  # board, chat, enroll, user, auth, oauth, notification,
│                               # inquiry, report, admin, bookmark, club, game, notice
│   ├── domain/                 # model / service / event / enums / dto (순수 Java)
│   ├── application/
│   │   ├── port/{in,out}/      # in: UseCase 인터페이스 · out: Repository + FetchPort
│   │   ├── service/            # {Ctx}{Client/Internal}{Command/Query}Service + {Ctx}Reader
│   │   ├── event/              # 발행 이벤트 (publisher 소유)
│   │   └── dto/                # Command / Response DTO
│   └── adapter/
│       ├── in/{web,websocket,event}/   # Controller / STOMP / 이벤트 구독 리스너
│       └── out/{persistence,external}/ # JPA·QueryDSL / FCM·S3·OAuth + FetchPort 구현
├── global/                     # config(cloud/data/security/web), authorization(AOP), persistence, error
│                               # (redis·idempotency·scheduler 는 컨텍스트별 adapter 로 분산)
└── common/                     # error(ErrorCode, BaseException), page
```

## ⚠️ 절대 변경 금지 (단순화 X)

- **이중 단계 이벤트 리스너 (Transactional Outbox)**: `@EventListener`(커밋 전 DB 저장) + `@TransactionalEventListener(AFTER_COMMIT)`(커밋 후 FCM) + `NotificationScheduler`(60초 재시도). 합치거나 단순화 금지. (상세 → `backend-patterns.md`)
- **RedisNotificationPublisher 분리**: `RedisNotificationPublisher`(`NotificationDispatchPort` 구현)와 `ChatMessageRedisPublisher`(`@TransactionalEventListener`)는 의도적 분리. 합치면 JDK 동적 프록시에서 메서드가 사라져 Spring 부팅이 깨짐.
- **Soft Delete (엔티티 성격별)**: 핵심 애그리거트(`User`·`Board`·`ChatRoom`·`ChatMessage`)만 `deletedAt`+`@SQLRestriction("deleted_at IS NULL")`+도메인 `delete()`(deletedAt 세팅)→`save()`. **이 엔티티들엔 `deleteById`/물리 삭제 금지.** 조인·토글·토큰·아웃박스 엔티티(`Bookmark`·`Block`·`Enroll`·`ChatRoomMember`·`RefreshToken`·`NotificationOutbox` 등)는 유니크 제약·보안·볼륨 때문에 **물리 삭제가 정상**. ("모든 엔티티 soft-delete" 아님 — 상세·분류표 → `backend-patterns.md`)

## Configuration Profiles

| Profile | 파일 | 용도 |
|---|---|---|
| `local` | `application-local.yml` | 로컬 개발용 하드코딩 값 |
| `dev` | `application-dev.yml` | CI/CD 환경변수 주입 (GitHub Secrets → `deploy.yml`) |

## Technology Stack

- **Java 21** (Gradle toolchain), Spring Boot 3.4.2
- **ORM**: Spring Data JPA + QueryDSL 5.0 (Jakarta) · **DB**: MySQL on AWS RDS (HikariCP)
- **Cache**: Redis (Lettuce) · **Push**: Firebase Admin SDK 9.3 (FCM) · **Storage**: AWS SDK v2 (S3)
- **Auth**: JWT (jjwt 0.11.5) + Spring Security · **WebSocket**: Spring STOMP + Redis Pub/Sub
- **Docs**: springdoc-openapi 2.8.5

## Deployment

EC2 단일 인스턴스 Nginx Blue/Green. `.github/workflows/deploy.yml` 이 `main` 푸시 시 Docker 이미지 빌드/푸시 후 SSH 로 `deploy.sh` 실행.

## 주요 파일 위치

| 목적 | 경로 |
|---|---|
| 에러 코드 | `common/error/ErrorCode.java` |
| 전역 예외 핸들러 | `global/error/GlobalExceptionHandler.java` |
| JWT 인증 필터 | `global/config/security/JwtAuthenticationFilter.java` |
| 비동기 설정 | `global/config/web/AsyncConfig.java` |
| FCM 발신 | `notification/adapter/out/external/FcmNotificationSender.java` |
| 알림 스케줄러 | `notification/adapter/in/scheduler/NotificationScheduler.java` |
| Outbox 상태 관리 | `notification/application/service/OutboxStateTransitioner.java` |
| 알림 템플릿 | `notification/domain/model/NotificationTemplate.java` |
| Redis 설정 / Pub/Sub | `global/config/data/RedisConfig.java`, `notification/adapter/out/external/RedisNotificationPublisher.java`, `chat/adapter/out/external/ChatMessageRedisPublisher.java` |
| WebSocket 설정 | `global/config/web/WebSocketConfig.java` |
| Fetch Port 예시 | `board/application/port/out/*FetchPort.java`, `board/adapter/out/external/Board*FetchAdapter.java` |
