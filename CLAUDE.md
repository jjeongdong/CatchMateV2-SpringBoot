# CatchMate 백엔드

야구 직관 동행 매칭 서비스의 백엔드. Spring Boot 3.4 · Java 21 · JPA/QueryDSL · MySQL · Redis · STOMP · FCM · Spring AI.

## 명령어
- 전체 검사: `./gradlew build` (spotlessCheck + test)
- 포맷: `./gradlew spotlessApply` — 커밋 전 필수
- 아키텍처 검사: `./gradlew test --tests 'com.back.catchmate.architecture.*'`

## 아키텍처 한눈에
- DDD 4계층, 도메인 우선 패키지: `{bc}/{presentation,application,domain,infrastructure}` + `global`
- 의존: Presentation → Application → Domain ← Infrastructure
- 각 최상위 패키지(board, enroll, chat …)는 독립 바운디드 컨텍스트(BC)

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

## 마이그레이션 과도기
기존 코드는 아직 옛 구조(`controller/service/repository/entity/dto`)다. 새 컨벤션 검사는 `src/test/java/com/back/catchmate/architecture/MigratedContexts.java` 의 `NAMES` 에 있는 BC 만 대상이다.
- 전환된 BC: 새 컨벤션 100%
- 미전환 BC 버그 수정: 기존 구조 유지, 최소 수정 (부분 전환 금지)
- 미전환 BC 새 기능: 그 BC 를 먼저 전환
- 새 BC: 새 컨벤션으로 만들고 `NAMES` 에 추가
- 전환 중 미전환 BC 조회가 필요하면: 미전환 BC 에 `{Bc}QueryApi` + `application/dto/api` 만 추가 허용

## 규칙 문서 (`.claude/rules/`)
| 파일 | 로드 시점 | 내용 |
|---|---|---|
| `architecture.md` | 항상 | 계층, 패키지, 접미사, BC 경계, global, 과도기 |
| `git.md` | 항상 | 커밋, 브랜치, 머지 |
| `presentation.md` | `presentation/` 편집 | Controller, Request, URL, 상태 코드, 페이징, 에러 응답 |
| `application.md` | `application/` 편집 | Service, QueryApi, 트랜잭션, DTO, 이벤트 리스너 |
| `domain.md` | `domain/` 편집 | 엔티티, Repository 인터페이스, 에러 코드·예외, 이벤트 |
| `infrastructure.md` | `infrastructure/` 편집 | Repository 구현, 쿼리 |
| `code-style.md` | `*.java` 편집 | 포맷, Lombok, Java 문법, 이름, 로깅, 주석 |
| `testing.md` | `src/test/` 편집 | 테스트 전략·스타일 |

규칙이 다루지 않거나 규칙끼리 충돌하는 상황에서는 임의로 판단하지 말고 사용자에게 묻는다.
