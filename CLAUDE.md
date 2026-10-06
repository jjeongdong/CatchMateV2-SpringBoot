# CatchMate 백엔드

야구 직관 동행 매칭 서비스의 백엔드. Spring Boot 3.4 · Java 21 · JPA/QueryDSL · MySQL · Redis · STOMP · FCM · Spring AI.

## 명령어
- 전체 검사: `./gradlew build` (spotlessCheck + test)
- 포맷: `./gradlew spotlessApply` — 커밋 전 필수
- 아키텍처 검사: `./gradlew test --tests 'com.back.catchmate.architecture.*'`
- 훅(`.claude/settings.json`): Java 편집 직후 그 파일만 자동 포맷, 턴 종료 전 아키텍처 검사 — 실패하면 종료가 막히고 위반 내용이 Claude 에게 돌아온다

## 아키텍처 한눈에
- DDD 4계층, 도메인 우선 패키지: `{bc}/{presentation,application,domain,infrastructure}` + `global`
- 의존: Presentation → Application → Domain ← Infrastructure
- 각 최상위 패키지(board, enroll, chat …)는 독립 바운디드 컨텍스트(BC). 아키텍처 검사는 `global` 을 뺀 최상위 패키지를 자동으로 BC 로 잡는다

## 절대 규칙
1. 타 BC 는 `{Bc}QueryApi`, `application/dto/api`, `domain/event` 만 사용한다. 타 BC 상태 변경은 이벤트로.
2. 타 BC 엔티티는 ID 로만 참조한다. 타 BC 테이블 JOIN 금지 (측정 근거가 있는 예외만).
3. 비즈니스 규칙은 엔티티에. Service 는 흐름만.
4. `@Transactional` 은 Application 계층 public 메서드에만, 메서드마다.
5. DTO 는 `record`, 흐름은 Request → Command → Result.
6. 에러마다 전용 예외 클래스 + BC 별 ErrorCode enum. 도메인은 `HttpStatus` 를 모른다.
7. Lombok 은 빈 `@RequiredArgsConstructor`/`@Slf4j`, 엔티티 `@Getter`/`@NoArgsConstructor(PROTECTED)` 만.
8. `var`, `TODO`, 와일드카드 import, `Optional.get()`, `System.out` 금지.
9. 주석·로그·커밋 메시지는 한국어. 주석은 "왜"만.
10. `main` 직접 커밋 금지. 브랜치 작업 후 `git merge --no-ff`.

## 규칙 문서 (`.claude/rules/`)
| 파일 | 로드 시점 | 내용 |
|---|---|---|
| `architecture.md` | 항상 | 계층, 패키지, 접미사, BC 경계, global |
| `git.md` | 항상 | 커밋, 브랜치, 머지 |
| `presentation.md` | `presentation/` 편집 | Controller, Request, URL, 상태 코드, 페이징, 에러 응답 |
| `application.md` | `application/` 편집 | Service, QueryApi, 트랜잭션, DTO, 이벤트 리스너 |
| `domain.md` | `domain/` 편집 | 엔티티, Repository 인터페이스, 에러 코드·예외, 이벤트 |
| `infrastructure.md` | `infrastructure/` 편집 | Repository 구현, 쿼리 |
| `code-style.md` | `*.java` 편집 | 포맷, Lombok, Java 문법, 이름, 로깅, 주석 |
| `testing.md` | `src/test/` 편집 | 테스트 전략·스타일 |

규칙이 다루지 않거나 규칙끼리 충돌하는 상황에서는 임의로 판단하지 말고 사용자에게 묻는다.

## 하네스: 리뷰 서브에이전트

**목표:** 변경분을 아키텍처·동시성·성능·보안 4개 관점으로 동시에 리뷰하고, 교차 검증·중복 제거를 거친 리포트 1장으로 모은다. 리뷰어는 전원 읽기 전용 — 수정 여부는 사용자가 결정한다.

**트리거:** "리뷰 팀", "전방위 리뷰", "제대로 리뷰해줘", 머지 전 점검, BC 단위 감사, 재리뷰 요청 시 `review-board` 스킬을 사용하라. 관점 하나만 필요하면 해당 관점 스킬(`architecture-review`·`concurrency-review`·`persistence-perf-review`·`auth-security-review`)을 직접 쓴다.

**변경 이력:**
| 날짜 | 변경 내용 | 대상 | 사유 |
|---|---|---|---|
| 2026-10-03 | 재구성 (harness v2) — 에이전트 5개 + 스킬 5개. 헥사고날 리뷰어를 DDD `architecture-reviewer` 로 교체, 구문 게이트를 ArchUnit 테스트로, 체크리스트를 현재 Outbox·STOMP·Redis 구조로 갱신 | `.claude/agents/*`, `.claude/skills/*` | 9/10 MVC·DDD 전환 때 삭제된 리뷰 파이프라인 복원 |
