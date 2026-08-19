---
name: auth-security-review
description: >-
  catchmate 백엔드를 접근 통제·데이터 노출 관점에서 리뷰한다 — JWT 인증 필터와 SecurityConfig
  화이트리스트, STOMP CONNECT/SUBSCRIBE/SEND 인가(남의 채팅방 구독·전송 차단), @AuthUser 로 받은
  userId 와 리소스 소유권 검증(IDOR), 차단(Block)·soft delete 우회 조회, 예외 메시지·로그의 PII·
  JWT·FCM 토큰 노출, 시크릿 하드코딩, CORS·쿠키 속성. 다음 상황에 사용한다 — "보안 리뷰",
  "인가 빠진 데 없나", "권한 체크 봐줘", "남의 방 들어가지나", 컨트롤러·STOMP 인터셉터·시큐리티
  설정·조회 서비스를 수정한 뒤. 백엔드 인가/노출 축 전용이며, 전방위 리뷰는 review-board
  오케스트레이터가 호출한다.
---

# Auth / Security Review (접근 통제·노출)

이 스킬은 **"인증만 통과한 다른 사용자가 이 요청을 그대로 보내면 무엇을 읽거나 바꿀 수 있는가"** 를
묻는다. 답이 "남의 리소스"면 위반이다. 구조·계약은 `hexagonal-review`, 실행 순서는
`concurrency-review` 담당이므로 여기서 중복 지적하지 않는다.

이 축의 결함은 대개 **없는 코드**(빠진 검증)다. 그래서 있는 코드를 읽는 것만으로는 부족하고,
**진입점 → 인가 지점 → 데이터 접근**의 경로를 끝까지 따라가야 한다.

## 0. 스코프 잡기
1. 오케스트레이터가 준 스코프를 그대로 쓴다. 없으면 `git diff --name-only`(+ staged, 없으면 워킹트리).
2. 변경된 파일의 **요청 경로**를 확인한다. 컨트롤러/STOMP 핸들러가 스코프 밖이어도, 인가가 어디서
   이뤄지는지 확인하기 위해 읽는다. **지적은 스코프 안 파일에 대해서만** 한다.
3. 인가 지점 후보: `global/config/security/SecurityConfig`, `JwtAuthenticationFilter`,
   `StompAuthChannelInterceptor`, `global/authorization/resolver/AuthUserArgumentResolver`,
   그리고 각 서비스의 소유권 검증 로직.
4. **시크릿 파일은 열지 않는다** (`application-local.yml`, `firebase-adminsdk.json` — 권한 차단됨).
   코드가 시크릿을 **어떻게 참조하는지**만 본다. 실값은 리포트에 절대 옮기지 않는다.

---

## 체크리스트

### A. 인증 경계 (누가 들어오는가)
- 새 엔드포인트가 `SecurityConfig` 의 **permitAll 화이트리스트**에 무심코 들어가지 않았는가?
  화이트리스트 패턴이 `/api/**` 처럼 넓게 잡혀 의도치 않은 경로까지 열지 않는가?
- `@AuthUser` 로 주입된 userId 를 쓰는 대신, **요청 body/파라미터의 userId** 를 그대로 신뢰하지
  않는가? → 클라이언트가 남의 id 를 보내면 그대로 통과 (전형적 IDOR). 🔴
- 토큰 검증 실패·만료 경로가 **401 로 끊기는가**, 아니면 예외를 삼키고 익명으로 계속 진행하는가?
- 리프레시 토큰이 무효화(로그아웃·탈퇴) 후에도 사용 가능하지 않은가? (`RefreshToken` 은 물리 삭제가
  정상 — 삭제 경로가 실제로 호출되는지 확인)

### B. STOMP / WebSocket 인가 (이 프로젝트의 최대 노출면)
- CONNECT 시 JWT 를 검증하는가? 검증 결과(principal)를 세션에 심어 이후 프레임에서 쓰는가?
- **SUBSCRIBE 시 목적지 검증**이 있는가? `/sub/chat/room/{roomId}` 를 구독할 때 그 사용자가 그 방의
  멤버(`ChatRoomMember`)인지 확인하지 않으면 **아무나 남의 채팅을 실시간으로 도청**할 수 있다. 🔴
  공격 시나리오: "로그인한 사용자 B 가 roomId 를 1씩 증가시키며 SUBSCRIBE → A 의 대화 전량 수신".
- **SEND 시 발신자 검증**이 있는가? payload 의 senderId 를 신뢰하면 타인 사칭 전송이 가능하다. 🔴
- 방 나가기/강퇴/차단 이후에도 기존 구독이 살아있지 않은가? (구독 시점에만 검사하고 이후 해제
  경로가 없으면, 나간 사용자가 계속 수신한다)
- 에러 프레임(`ChatErrorResponse` 등)에 내부 정보(스택트레이스·SQL·다른 사용자 식별자)가 실려
  클라이언트로 나가지 않는가?

### C. 리소스 소유권 (IDOR)
- 단건 조회/수정/삭제에서 **"엔티티를 찾았다"와 "이 사용자 것이다"를 둘 다** 확인하는가?
  `findById(id)` 후 소유자 비교 없이 반환/수정하면 위반이다.
- 목록 조회의 where 절에 **요청자 필터**(작성자·멤버십)가 들어가는가?
- 관리자 전용 동작이 `{T}AdminQueryUseCase` 경로 + 실제 role 검사로 이중 보호되는가?
  정문 이름만 Admin 이고 role 검사가 없으면 위반이다.
- 차단(`Block`) 관계가 조회에 반영되는가? 차단한 사용자의 글/메시지가 그대로 보이면 정책 위반.
- soft delete 된 리소스(`@SQLRestriction`)를 **네이티브 쿼리·캐시·Redis** 경로로 우회 조회하지
  않는가? → 삭제된 게시글/탈퇴 회원 정보 노출.

### D. 데이터 노출 (응답·로그·예외)
- 응답 DTO 에 필요 이상의 개인정보(이메일·전화·소셜 ID·FCM 토큰·내부 PK 체계)가 들어가지 않는가?
- 로그에 JWT, 리프레시 토큰, FCM 토큰, 이메일 전문을 찍지 않는가? (`log.info("token={}", ...)` ❌)
- 예외 메시지가 사용자에게 내부 구조를 알려주지 않는가? 비즈니스 예외는
  `BaseException(ErrorCode.XXX)` 로 통일되어야 하며, 스택트레이스는 응답에 나가지 않는다.
- 인증 실패 응답이 **"없는 계정"과 "비밀번호 틀림"을 구분**해 알려주지 않는가? (계정 열거)

### E. 설정·시크릿
- 시크릿·API 키·비밀번호가 코드에 하드코딩되지 않았는가? (`@Value` + yml 이 원칙)
- 쿠키 속성(`CookieFactory`/`CookieProperties`)이 `HttpOnly`, `Secure`, `SameSite` 를 유지하는가?
  (ALB 가 HTTPS 를 종단하므로 `Secure` 를 끄는 변경은 특히 위험)
- CORS 설정이 `allowedOrigins("*")` + credentials 조합으로 열려있지 않은가?
- 새로 추가된 의존성·엔드포인트(actuator, swagger)가 운영 프로파일에서 노출되지 않는가?

---

## 리포트 형식
- **확실한 위반만** 보고한다 (오탐 0 목표). 애매하면 "확인 필요"로 분리.
- 각 항목: `[보안] 파일:라인` · **공격 시나리오 한 줄**(누가 무엇을 보낼 때 무엇이 뚫리는가) ·
  제안 수정. 시나리오를 못 쓰면 위반이 아니라 ❓ 다.
- 심각도: **🔴** 인가 우회·타인 데이터 접근·사칭·시크릿 노출 > **🟡** 과다 노출·로그 위생·설정 약화
  > **🟢** 방어 강화 제안.
- 토큰·비밀키·개인정보 **실값을 리포트에 옮기지 않는다** (파일:라인으로만 가리킨다).
- 끝에 한 줄 총평.

## 규칙 SSOT
- `.claude/ondemand-rules/backend-coding-conventions.md` #4·#7 — 예외 처리 · 하드코딩 금지
- `.claude/ondemand-rules/backend-patterns.md` — Soft Delete 분류표 · AOP 권한
- 관련 코드: `global/config/security/*`, `global/authorization/*`
