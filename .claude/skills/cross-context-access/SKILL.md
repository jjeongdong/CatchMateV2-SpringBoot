---
name: cross-context-access
description: >-
  catchmate 헥사고날 백엔드에서 한 Bounded Context 가 다른 컨텍스트의 데이터·동작을
  필요로 할 때 Fetch Port 체인을 올바르게 배선한다. 다음 상황에 사용한다 — 한 컨텍스트
  (board, chat, enroll, user, notification, club, game, bookmark, report, inquiry, notice,
  auth, oauth, admin) 의 service 가 다른 컨텍스트의 정보/처리를 호출해야 할 때,
  "board에서 user 정보 가져와", "enroll에서 게임 정보 필요해", "다른 컨텍스트 호출",
  "FetchPort/FetchAdapter 추가", cross-context 의존이 등장할 때. FetchPort → FetchAdapter →
  상대 Internal UseCase 체인과 0-import DTO 격리(아키텍처 규칙 #2·#3)를 강제한다.
---

# Cross-Context Access (Fetch Port 체인)

한 컨텍스트가 다른 컨텍스트의 데이터/동작을 쓰려면 **항상** 아래 체인을 거친다. Service 끼리
직접 의존하거나, 상대 Repository/Reader/Entity/도메인을 import 하면 **안 된다** (규칙 #2·#3).

```
{caller} service
   └─→ {caller}/port/out/external/{Target}FetchPort        (인터페이스, 호출자 DTO 반환)
            ↑ implements
        {caller}/adapter/out/external/{Caller}{Target}FetchAdapter
            └─→ {target}/port/in/{Target}Internal{Query|Command}UseCase   (상대 정문)
                     반환: {Target}InternalResponse  →  {Caller}{Target}Info 로 매핑
```

## 진입할 상대 정문 고르기 (호출 목적별)

| 목적 | 진입 UseCase | 위치 |
|---|---|---|
| 읽기 | `{Target}InternalQueryUseCase` | `{target}/application/port/in/` |
| 쓰기 | `{Target}InternalCommandUseCase` | `{target}/application/port/in/` |
| admin 컨텍스트 전용 읽기 | `{Target}AdminQueryUseCase` | `{target}/application/port/in/` |

- ❌ Controller 전용 `Client` 계열로 진입 금지. ❌ 상대 `Service`/`Repository`/`Reader`/JPA Entity 직접 주입 금지.
- ✅ 정문은 `application.port.in` 의 UseCase 인터페이스뿐.

## 0-import 규칙 (격리 경계)

FetchPort 시그니처와 호출자 코드에는 **상대 컨텍스트의 도메인 모델·enum·Entity 가 절대 노출되면 안 된다.**
- 입력은 `Long`/`List<Long>`/`String` 같은 원시 식별자.
- 출력은 **호출자 자신의** record DTO (`{caller}/application/port/out/dto/{Caller}{Target}Info`).
- 상대의 enum 이 필요하면 호출자 DTO 에서 `String` 으로 받는다.
- 상대 `{Target}InternalResponse` 를 import 하는 곳은 **오직 FetchAdapter 한 곳**뿐 (거기서 호출자 DTO 로 매핑).

---

## 작업 절차

### 1. 호출자 DTO record 생성 (격리 경계)
`{caller}/application/port/out/dto/{Caller}{Target}Info.java`
```java
package com.back.catchmate.{caller}.application.port.out.dto;

public record {Caller}{Target}Info(
        Long userId,
        String nickName,
        String profileImageUrl
        // 필요한 필드만. 상대 enum 은 String 으로.
) {
}
```

### 2. FetchPort 인터페이스 생성
`{caller}/application/port/out/external/{Target}FetchPort.java`
```java
package com.back.catchmate.{caller}.application.port.out.external;

import com.back.catchmate.{caller}.application.port.out.dto.{Caller}{Target}Info;

import java.util.List;

public interface {Target}FetchPort {
    {Caller}{Target}Info get{Target}(Long {target}Id);

    List<{Caller}{Target}Info> get{Target}s(List<Long> {target}Ids);
}
```

### 3. FetchAdapter 구현 생성 (유일하게 상대 컨텍스트를 import 하는 곳)
`{caller}/adapter/out/external/{Caller}{Target}FetchAdapter.java`
```java
package com.back.catchmate.{caller}.adapter.out.external;

import com.back.catchmate.{caller}.application.port.out.dto.{Caller}{Target}Info;
import com.back.catchmate.{caller}.application.port.out.external.{Target}FetchPort;
import com.back.catchmate.{target}.application.dto.response.{Target}InternalResponse;
import com.back.catchmate.{target}.application.port.in.{Target}InternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class {Caller}{Target}FetchAdapter implements {Target}FetchPort {
    private final {Target}InternalQueryUseCase {target}InternalQueryUseCase;

    @Override
    public {Caller}{Target}Info get{Target}(Long {target}Id) {
        return to{Caller}{Target}Info({target}InternalQueryUseCase.get{Target}({target}Id));
    }

    @Override
    public List<{Caller}{Target}Info> get{Target}s(List<Long> {target}Ids) {
        return {target}InternalQueryUseCase.get{Target}s({target}Ids).stream()
                .map(this::to{Caller}{Target}Info)
                .toList();
    }

    private {Caller}{Target}Info to{Caller}{Target}Info({Target}InternalResponse response) {
        return new {Caller}{Target}Info(
                response.userId(),
                response.nickName(),
                response.profileImageUrl()
        );
    }
}
```

### 4. 호출자 service 에서 FetchPort 주입
Adapter 가 아니라 **FetchPort 인터페이스**를 주입한다.
```java
private final {Target}FetchPort {target}FetchPort;
```

### 5. 상대 정문에 메서드가 없으면 — 상대 컨텍스트에 추가
필요한 read/write 메서드가 `{Target}Internal*UseCase` 에 없으면 거기에 추가하고 그 service 구현체도 채운다.
반환 타입은 상대의 `{Target}InternalResponse` record (절대 상대 Entity/도메인 노출 금지). 상대 컨텍스트
파일을 만질 땐 그 컨텍스트의 정문/0-import 규칙을 동일하게 지킨다.

---

## 완료 검증 (필수)
1. FetchPort/호출자 코드에 상대 도메인·enum·Entity import 가 **없는지** 확인.
2. 상대 import 는 FetchAdapter 한 곳에만 있는지 확인.
3. `./gradlew archCheck` 실행 — 통과해야 한다. (PostToolUse 훅도 같은 규칙을 검출하지만, 사람 기준 최종 게이트는 archCheck.)

## 실제 레퍼런스 (이 레포에 존재)
- DTO: `board/application/port/out/dto/BoardUserInfo`
- Port: `board/application/port/out/external/UserFetchPort`
- Adapter: `board/adapter/out/external/BoardUserFetchAdapter`
- 상대 정문: `user/application/port/in/UserInternalQueryUseCase`

상세 규칙 SSOT: `.claude/ondemand-rules/backend-architecture.md` (#2 Cross-context, #3 0-import).
