---
name: write-tests
description: >-
  catchmate 백엔드(헥사고날 + JUnit 5 + Mockito + AssertJ)의 테스트 코드를 이 프로젝트
  컨벤션대로 작성한다. 다음 상황에 사용한다 — "테스트 작성해줘", "이 서비스 테스트 짜줘",
  "단위 테스트 추가", "테스트 코드 만들어줘", 새 Service/도메인 로직/상태전이/검증 로직을 만든 뒤
  검증 테스트가 필요할 때, 버그를 재현하는 테스트가 필요할 때. 무엇을(레이어) 어떻게(모킹 범위)
  테스트할지 결정하고, given/when/then · 한글 DisplayName · BaseException(ErrorCode) 검증 ·
  도메인 static factory/builder · FetchPort 모킹 같은 이 코드베이스의 실제 패턴을 따른다.
  E2E나 프론트엔드 테스트가 아니라 백엔드 JUnit 테스트에 사용한다. 클래스 하나에 테스트 몇 개를
  붙이는 단발 작업이면 이 스킬로 직접 작성하고, 여러 클래스를 한꺼번에 채우거나 컨텍스트 단위로
  테스트 공백을 진단해야 하면 test-team 오케스트레이터(계획→작성→검증 루프)를 대신 쓴다.
---

# Write Tests (catchmate 백엔드 테스트 작성)

이 프로젝트의 테스트는 **JUnit 5 + Mockito + AssertJ** 순수 단위 테스트가 주력이다.
목표는 "커버리지 숫자"가 아니라 **그 클래스가 책임지는 규칙 하나하나를 빠르고 결정적으로 못 박는 것**이다.
느린 스프링 컨텍스트 없이 도는 테스트가 많을수록 리팩터링이 안전해진다.

## 먼저: 무엇을 테스트하는가 (레이어 판단)

대상 클래스가 어느 레이어인지 먼저 보고 전략을 고른다. 헥사고날에서 레이어마다 테스트 방법이 다르다.

| 대상 | 테스트 방식 | 모킹 대상 |
|---|---|---|
| **domain/model, enums** (`NotificationTemplate`, 상태전이, VO) | 순수 단위 — 모킹 0개 | 없음 (진짜 객체로) |
| **application/service** (`{Ctx}...Service`, `Reader`, `Executor`) | Mockito 단위 | 그 서비스의 **out 포트 전부**: Repository·Reader·FetchPort·이벤트 publisher·Redis 어댑터 |
| **cross-context 흐름** | Service 단위 안에서 처리 | 자기 **FetchPort** 를 모킹 (상대 UseCase 를 직접 모킹하지 **않음**) |
| **adapter/out (QueryDSL, FetchAdapter)** | 대개 테스트 안 함 (얇은 위임) | — 필요하면 통합 테스트로 |
| **DB/동시성/스키마 검증** | `@SpringBootTest` 통합 (드묾, 무거움) | 실제 DB — [통합 테스트](#통합-테스트-무거우니-꼭-필요할-때만) 참고 |

**기본값은 항상 도메인 단위 또는 서비스 단위 테스트다.** 스프링 컨텍스트(`@SpringBootTest`)는 로컬 DB/프로파일이 있어야 돌고 느리므로, "이 로직은 진짜 DB나 스프링 배선이 있어야만 증명된다"가 확실할 때만 쓴다. 대부분의 비즈니스 규칙은 서비스 단위 테스트로 충분하다.

## 서비스 단위 테스트 (주력 패턴)

서비스는 UseCase 구현이다. 서비스가 의존하는 **out 포트를 모두 `@Mock`** 하고, 서비스 자체는 `@InjectMocks` 로 실제 인스턴스를 만든다. 그리고 "이 입력이 들어오면 → 포트를 이렇게 호출하고 / 이 값을 반환하고 / 이 예외를 던진다"를 검증한다.

```java
@ExtendWith(MockitoExtension.class)
class EnrollInternalCommandServiceTest {

    @Mock
    private EnrollRepository enrollRepository;
    @Mock
    private EnrollReader enrollReader;                 // 같은 컨텍스트 협력자도 모킹
    @Mock
    private BoardFetchPort boardFetchPort;             // cross-context 는 자기 FetchPort 를 모킹
    @Mock
    private ApplicationEventPublisher eventPublisher;  // 이벤트 발행 검증용

    @InjectMocks
    private EnrollInternalCommandService sut;          // system under test

    @Test
    @DisplayName("이미 수락된 신청이 있으면 재신청 시 예외를 던진다")
    void 중복_수락_신청이면_예외() {
        // given
        Long userId = 1L, boardId = 10L;
        given(enrollReader.findAcceptStatusById(any()))
                .willReturn(Optional.of(AcceptStatus.ACCEPTED));

        // when & then
        assertThatThrownBy(() -> sut.enroll(userId, boardId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_ACCEPTED));

        // 예외 경로에서는 저장/이벤트가 일어나지 않아야 한다
        then(enrollRepository).should(never()).save(any());
        then(eventPublisher).shouldHaveNoInteractions();
    }
}
```

핵심 습관:
- **`sut`(system under test)** 로 검증 대상을 명확히 한다. `@InjectMocks` 필드 하나만 실제 객체다.
- **out 포트만 모킹한다.** 도메인 모델·Command DTO·값 객체는 진짜로 만든다 (모킹하면 로직을 안 태워서 테스트가 거짓말을 한다).
- **BDDMockito(`given`/`willReturn`, `then`/`should`)** 를 선호하되, 기존 파일이 `when`/`verify` 를 쓰고 있으면 그 파일 스타일에 맞춘다.
- **해피 패스만 쓰지 말 것.** 이 코드베이스 버그는 대부분 경계에서 난다 — 빈 Optional, 권한 없음(`FORBIDDEN_ACCESS`), 중복 상태, 최대 재시도 초과 같은 분기마다 테스트 하나씩.

## 도메인 단위 테스트 (모킹 없음)

도메인 모델·enum·상태전이는 협력자가 없으니 모킹하지 말고 **진짜 객체로** 입력→출력만 본다. 가장 신뢰도 높은 테스트다.

```java
class NotificationTemplateTest {
    @Test
    void ENROLL_REQUEST_제목에_사용자이름_바인딩() {
        String result = NotificationTemplate.ENROLL_REQUEST.formatTitle("홍길동");
        assertThat(result).isEqualTo("홍길동님이 참여 신청을 보냈습니다");
    }
}
```

상태를 바꾸는 도메인 메서드(예: `outbox.updateStatusFailure(...)`)는 **호출 후 객체 상태를 직접 단언**한다 — `assertThat(outbox.getStatus()).isEqualTo(FAILED)` 처럼. 반환값이 없어도 상태 변화가 곧 결과다.

## 테스트 데이터 만들기

- **도메인 객체는 그 도메인의 `create(...)` 정적 팩토리나 `builder()` 로 만든다.** (예: `NotificationOutbox.create(1L, "token", "title", "body", "{}")`, `NotificationOutbox.builder().retryCount(4).status(PROCESSING).build()`). 생성자를 억지로 리플렉션하지 말고 도메인이 제공하는 생성 경로를 쓴다.
- 같은 셋업이 한 파일에서 3번 넘게 반복되면 그 파일 안에 `private` 헬퍼 메서드로 뽑는다. **아직 공용 테스트 픽스처/ObjectMother 인프라는 이 프로젝트에 없으니** 새로 만들지 말고 파일 로컬 헬퍼로 둔다.

## 이 프로젝트의 규칙 (형식)

- **위치**: `src/test/java` 아래 대상과 **같은 패키지**. (예: `...notification.application.service.OutboxStateTransitionerTest`)
- **네이밍**: 클래스는 `{대상}Test`. 메서드명은 **한글 서술형(`중복_수락_신청이면_예외`)을 기본으로 쓴다** — 조건·기대결과가 이름만으로 읽혀야 한다. 다만 이웃 테스트 파일이 이미 영문 `동작_조건`(`updateStatusFailure_recordsErrorMessage`)으로 통일돼 있으면 그 파일의 일관성을 깨지 말고 맞춘다. (기존 스타일 유지 > 한글 선호 > 영문.)
- **`@DisplayName`** 은 "~하면 ~한다" 식 한글 문장으로, 실패 로그만 봐도 뭐가 깨졌는지 읽히게 쓴다.
- **주석**: `// given` `// when` `// then` (또는 `// when & then`) 3블록 구조를 유지한다.
- **단언은 AssertJ 로 통일**: `assertThat(...).isEqualTo/isZero/isInstanceOf/...`. JUnit 의 `assertEquals` 를 쓰지 않는다.
- **예외**: `assertThatThrownBy(() -> ...).isInstanceOf(BaseException.class)` 후, 어떤 에러인지 중요하면 `getErrorCode()` 까지 단언한다. 어떤 `ErrorCode` 인지는 이 앱에서 곧 HTTP 응답이므로 대충 넘기지 않는다.

## 자주 쓰는 import (컴파일 실패 예방)

단위 테스트에서 반복적으로 빠뜨리기 쉬운 import 다. 필요한 것만 골라 쓰되, `any()`·`never()`·이벤트 publisher 를 쓸 때 import 를 함께 챙긴다 (누락 시 `cannot find symbol` 로 컴파일이 깨진다).

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;   // 이벤트 발행 검증 시

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;                 // any(), anyLong(), anyString()
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;                     // given(...).willReturn(...)
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;                      // then(mock).should()...
import static org.mockito.BDDMockito.willThrow;                 // willThrow(...).given(mock)....
```

예외 에러코드 단언은 이 형태를 쓴다:

```java
assertThatThrownBy(() -> sut.someMethod(...))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS));
```

가드(권한·중복) 실패 경로에서는 부수효과가 없어야 하므로 함께 단언한다:
`then(repo).should(never()).save(any());`, `then(eventPublisher).shouldHaveNoInteractions();`

## 검증 (작성 후 반드시)

테스트를 새로 쓰거나 고쳤으면 **실제로 돌려서 통과를 확인**한다. 단언 없는 테스트, 컴파일만 되는 테스트는 가치가 없다.

```bash
./gradlew test --tests "com.back.catchmate.notification.application.service.OutboxStateTransitionerTest"
```

- 새 테스트가 **처음부터 초록이면 의심하라** — 모킹이 실제 로직을 다 우회했거나 단언이 비었을 수 있다. 버그 재현 테스트라면 먼저 빨강을 확인한 뒤 고쳐서 초록으로 만든다.
- 전체 회귀는 `./gradlew test`. 아키텍처 규칙까지 보려면 `./gradlew check` (`archCheck` 포함).

## 통합 테스트 (무거우니 꼭 필요할 때만)

`@SpringBootTest` 는 진짜 스프링 컨텍스트+DB 가 있어야 증명되는 것에만 쓴다 — 동시성(`FOR UPDATE SKIP LOCKED`), 실제 SQL/QueryDSL 쿼리, 트랜잭션 경계, 스키마 제약 같은 것. 이 레포엔 `OutboxSkipLockedConcurrencyTest` 등 소수만 있다.

```java
@SpringBootTest(classes = CatchmateApplication.class, properties = "spring.profiles.active=local")
class SomethingIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    // ... 실제 DB 로 검증, 끝나면 테스트 데이터 정리(cleanup)
}
```

주의: `local` 프로파일과 접근 가능한 DB 가 필요하다. 이 조건을 보장할 수 없으면 통합 테스트를 새로 만들지 말고 **서비스 단위 테스트로 대체 가능한지** 먼저 검토한다. 통합 테스트는 반드시 자기가 만든 데이터를 스스로 정리한다.

## 절대 하지 말 것

- 검증 대상 **서비스 자신을 모킹**하기 (그럼 아무것도 테스트 안 함).
- **도메인 모델/DTO 를 모킹**해서 진짜 로직을 우회하기.
- `verify` 만 있고 상태/반환 **단언이 없는** 테스트 (호출 여부만 보면 회귀를 못 잡는다).
- 실패 경로를 빼고 **해피 패스만** 쓰기.
- 이 코드베이스의 "절대 변경 금지" 3패턴(2단계 이벤트 리스너·RedisNotificationPublisher 분리·엔티티별 soft delete)을 테스트를 이유로 합치거나 우회하도록 프로덕션 코드를 고치기.
