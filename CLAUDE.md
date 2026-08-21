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
| `dev` | `application-dev.yml` | 운영 환경값 (서버에 직접 배치, `.gitignore`) |

## Technology Stack

- **Java 21** (Gradle toolchain), Spring Boot 3.4.2
- **ORM**: Spring Data JPA + QueryDSL 5.0 (Jakarta) · **DB**: MySQL on AWS RDS (HikariCP)
- **Cache**: Redis (Lettuce) · **Push**: Firebase Admin SDK 9.3 (FCM) · **Storage**: AWS SDK v2 (S3)
- **Auth**: JWT (jjwt 0.11.5) + Spring Security · **WebSocket**: Spring STOMP + Redis Pub/Sub
- **Docs**: springdoc-openapi 2.8.5

## Deployment

**ALB 뒤 EC2 2대** 스케일아웃 구성. ALB가 HTTPS/WSS(`wss://…/ws/chat`) SSL 을 종단하고 2개 `catchmate-app` 컨테이너로 분산한다(**stickiness ON** — WebSocket 세션이 한 인스턴스에 고정돼야 함). 인스턴스 간 실시간 전달은 **Redis Pub/Sub 브리지**로 팬아웃한다(각 인스턴스가 토픽을 구독해 자기에게 붙은 STOMP 세션에만 relay → 세션은 한 인스턴스에만 붙으므로 중복 없음). HikariCP 는 인스턴스당 pool=15(2×15=30). 각 EC2 에서 소스를 직접 빌드해 재기동하는 수동 배포(`deploy-local.sh`: `docker-compose up -d --build`).

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

## 하네스: 리뷰어 에이전트 팀

**목표:** 변경분을 아키텍처·동시성·성능·보안 4개 축으로 동시에 리뷰하고, 교차 검증·중복 제거를 거친 리포트 1장으로 수렴시킨다. 리뷰어는 전원 읽기 전용 — 수정 여부는 항상 사용자가 결정한다.

**트리거:** "리뷰어 팀", "전방위 리뷰", "제대로 리뷰해줘", 커밋·PR 전 점검, 컨텍스트 단위 감사 요청 시 `review-board` 스킬을 사용하라. 축 하나만 필요하면 해당 축 스킬(`hexagonal-review`·`concurrency-review`·`persistence-perf-review`·`auth-security-review`)을 직접 쓴다. 단순 질문은 직접 응답 가능.

**변경 이력:**
| 날짜 | 변경 내용 | 대상 | 사유 |
|---|---|---|---|
| 2026-08-18 | 리뷰어 팀 구성 (harness v2) — 에이전트 4개(concurrency/persistence-perf/auth-security/synthesizer) + 스킬 4개(3축 + review-board 오케스트레이터) 신설, 기존 `hexagonal-reviewer` 편입 | `.claude/agents/*`, `.claude/skills/*` | 리뷰 축이 아키텍처 하나뿐이라 런타임·비용·인가 결함이 사각지대였음 |
| 2026-08-19 | Phase 0 '축 지정 단발' 안내 정정 — 스킬(체크리스트)/에이전트(격리 실행자) 선택 기준 명시 | `skills/review-board` | 축 하나여도 스코프가 크면 격리가 필요한데 "스킬만 사용"으로 오안내 (실행 중 발견) |

## 하네스: 테스트 작성 팀

**목표:** 테스트를 계획→작성→검증 루프로 만든다. 케이스 도출과 품질 비평을 작성자와 분리해, 통과만 하고 아무것도 증명하지 않는 테스트가 남지 않게 한다.

**트리거:** "테스트 팀", "{컨텍스트} 테스트 채워줘", "테스트 제대로 짜줘", 여러 클래스에 테스트가 필요하거나 테스트 공백을 진단할 때 `test-team` 스킬을 사용하라. 클래스 하나에 테스트 몇 개만 붙이는 단발 작업은 `write-tests` 스킬로 직접 작성한다. 작성 범위는 `src/test` 로 제한되며, 프로덕션 코드 수정은 이 팀의 권한 밖이다.

**변경 이력:**
| 날짜 | 변경 내용 | 대상 | 사유 |
|---|---|---|---|
| 2026-08-18 | 테스트 팀 구성 (harness v2) — 에이전트 3개(test-planner/test-writer/test-verifier) + 스킬 3개(test-planning·test-quality-check·test-team 오케스트레이터) 신설, 기존 `write-tests` 를 작성 규약 SSOT 로 편입 | `.claude/agents/*`, `.claude/skills/*` | 작성 규약은 있었으나 케이스 도출·품질 검증이 작성자와 분리돼 있지 않았음 |
| 2026-08-19 | 병렬 writer 시 빌드 명령을 오케스트레이터가 직렬 1회 실행하도록 변경 + writer 에 계획서 시그니처 실검증 의무 추가 | `skills/test-team`, `agents/test-writer` | enroll 라운드에서 병렬 writer 3명이 각자 gradle 을 부르면 데몬 lock 이 충돌 / 계획서는 스냅샷이라 실제 소스와 어긋날 수 있음 |

## 하네스: MVC 마이그레이션 팀

**목표:** 헥사고날(포트·어댑터·UseCase 정문)을 **기능별 패키지 3계층 MVC**(`{ctx}/controller·service·repository·entity·dto`)로 옮긴다. 파일럿 1개로 변환 규칙을 확정한 뒤 의존 역순으로 확산하며, 매 라운드가 컴파일·검증을 통과한 커밋 가능 상태로 끝난다.

**⚠️ 전환 중 규칙 이원화:** `.claude/mvc-migration-state.json` 의 `migrated` 목록에 있는 컨텍스트만 MVC 규칙(`ondemand-rules/mvc-architecture.md`)이고, 나머지는 **헥사고날 규칙 그대로**다. Java 를 고치기 전에 그 컨텍스트가 어느 쪽인지 확인하라. `mvc-validate-arch.py` 훅이 컨텍스트별로 갈라 검사한다. 상태 파일이 없으면 전부 헥사고날이다.

**트리거:** "MVC 로 바꿔줘", "헥사고날 걷어내줘", "UseCase 없애줘", "{컨텍스트} 옮겨줘", "마이그레이션 시작", "다음 컨텍스트", "전환 상태" 요청 시 `mvc-migration` 스킬을 사용하라. 파일 배치만 궁금하면 `hexagonal-to-mvc-mapping`, 가드레일 문제면 `mvc-guardrail-switch`, 전환 결과 동작 검증이면 `mvc-invariant-check` 를 직접 쓴다.

**절대 변경 금지 3개는 전환 후에도 그대로다.** Outbox 2단계 리스너 · Redis 퍼블리셔 분리 · soft delete 는 아키텍처가 아니라 런타임 동작이므로 MVC 로 와도 형태를 유지한다.

**변경 이력:**
| 날짜 | 변경 내용 | 대상 | 사유 |
|---|---|---|---|
| 2026-08-21 | 초기 구성 (harness v2) — 에이전트 4개(planner/converter/invariant-guard/build-verifier) + 스킬 4개(mvc-migration 오케스트레이터 · hexagonal-to-mvc-mapping 규칙 SSOT · mvc-invariant-check · mvc-guardrail-switch) + `mvc-validate-arch.py` 검증기 + `dep-graph.py` 순환 예측기 | `.claude/agents/mvc-*`, `.claude/skills/mvc-*`, `.claude/hooks/mvc-validate-arch.py`, `.claude/ondemand-rules/mvc-architecture.md` | 저장소가 헥사고날을 5중으로 강제(훅·archCheck·Stop 게이트·룰 주입)하고 있어 코드 변환만으로는 전환이 불가능. 가드레일 전환을 하네스에 포함 |
