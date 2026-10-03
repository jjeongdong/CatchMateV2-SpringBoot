---
name: auth-security-review
description: >-
  catchmate 백엔드를 접근 통제·데이터 노출 관점에서 리뷰한다 — JWT 인증 필터와 SecurityConfig 허용 경로,
  STOMP CONNECT/SUBSCRIBE/SEND 인가(남의 채팅방 구독·전송·사칭 차단), @AuthUser userId 와 리소스 소유권
  검증(IDOR), 관리자 API 권한, 차단(Block)·soft delete 우회 조회, 응답·로그·에러 프레임의 개인정보·토큰
  노출, 시크릿 하드코딩, 쿠키·CORS 설정. 다음 상황에 사용한다 — "보안 리뷰", "인가 빠진 데 없나",
  "권한 체크 봐줘", "남의 방 들어가지나", Controller·STOMP 인터셉터·보안 설정·조회 서비스를 고친 뒤.
  전방위 리뷰는 review-board 오케스트레이터가 호출한다.
---

# Auth / Security Review (접근 통제·노출)

이 스킬은 **"인증만 통과한 다른 사용자가 이 요청을 그대로 보내면 무엇을 읽거나 바꿀 수 있는가"** 를
묻는다. 답이 "남의 리소스"면 위반이다. 구조·계약은 `architecture-review`, 실행 순서는
`concurrency-review` 담당이다.

이 축의 결함은 대개 **없는 코드**(빠진 검증)다. 있는 코드를 읽는 것만으로는 부족하고
**진입점 → 인가 지점 → 데이터 접근** 경로를 끝까지 따라가야 한다.

## 0. 스코프 잡기
1. 오케스트레이터가 준 스코프를 그대로 쓴다. 없으면 `git diff --name-only`(+ staged, 없으면 `main...HEAD`).
2. 변경 파일의 **요청 경로**를 확인한다. 인가 지점이 스코프 밖이어도 읽는다. **지적은 스코프 안 파일에만** 한다.
3. 인가 지점:
   - HTTP: `global/config/security/SecurityConfig`, `JwtAuthenticationFilter`, `AccessTokenVerifier`,
     `global/authorization/resolver/AuthUserArgumentResolver`
   - STOMP: `global/config/security/StompAuthChannelInterceptor`, `StompSubscriptionAuthorizer`,
     `chat/presentation/ChatRoomSubscriptionAuthorizer`, `chat/presentation/ChatStompController`
   - 리소스 소유권: 엔티티 도메인 메서드(규칙상 권한 검사는 엔티티에 있다 — `accept(requesterId, writerId)` 형태)
4. **시크릿 파일은 열지 않는다** — `src/main/resources/application-local.yml`, `application-dev.yml`,
   `firebase-adminsdk.json`. 코드가 시크릿을 **어떻게 참조하는지**(`@Value`/`@ConfigurationProperties`)만 본다.

---

## 체크리스트

### A. 인증 경계
- 새 엔드포인트가 `SecurityConfig` 허용 경로에 무심코 포함되지 않았는가? 패턴이 넓어 의도치 않은 경로까지 열지 않는가?
- `@AuthUser Long userId` 대신 **요청 body/파라미터의 userId** 를 신뢰하지 않는가? → 전형적 IDOR. 🔴
- 토큰 검증 실패·만료가 401 로 끊기는가, 예외를 삼키고 익명으로 진행하는가?
- 로그아웃·탈퇴 후 리프레시 토큰(`RedisRefreshTokenRepository`)이 실제로 무효화되는가?

### B. STOMP / WebSocket 인가 (이 프로젝트의 최대 노출면)
- CONNECT 에서 JWT 를 검증하고 principal 을 세션에 심는가?
- **SUBSCRIBE 목적지 검증** — 그 사용자가 그 방의 멤버인지 확인하는가? 화이트리스트 밖 목적지를 거부하는가?
  공격 시나리오: "사용자 B 가 roomId 를 1씩 늘려 SUBSCRIBE → A 의 대화 실시간 수신". 🔴
- **SEND 검증** — 발신자를 principal 에서 꺼내는가? payload 의 senderId 를 믿으면 사칭. 목적지가 `/pub` 로
  제한되어 브로커 목적지로 직접 보낼 수 없는가? 🔴
- 인가 분기가 **모든 메시지 경로**에서 실행되는가? (`StompCommand` 는 브로커 릴레이 경로에서 null 일 수 있어
  `SimpMessageType` 기준으로 바꾼 이력이 있다 — 새 분기가 같은 함정에 빠지지 않는지 확인)
- 방 나가기·강퇴·차단 후에도 기존 구독이 살아있지 않은가? 멤버십 캐시(`RedisChatMembershipCache`)가
  변경 시 무효화되는가?
- 에러 프레임에 내부 정보(스택트레이스·SQL·타인 식별자)가 실리지 않는가?

### C. 리소스 소유권 (IDOR)
- 단건 조회·수정·삭제에서 "찾았다"와 "이 사용자 것이다"를 **둘 다** 확인하는가?
- 목록 조회 where 절에 요청자 필터(작성자·멤버십)가 들어가는가?
- 관리자 API(`/api/admin/**`, `Admin{Bc}Controller`)가 이름뿐 아니라 **실제 role 검사**로 보호되는가?
- 차단(`Block`) 관계가 조회에 반영되는가?
- soft delete 리소스를 네이티브 쿼리·캐시·Redis 경로로 우회 조회하지 않는가?

### D. 데이터 노출
- 응답 Result 에 필요 이상의 개인정보(이메일·소셜 ID·FCM 토큰)가 들어가지 않는가?
- 로그에 토큰·이메일·전화번호·FCM 토큰·채팅 내용을 찍지 않는가? (`code-style.md` 로깅 5)
- 에러 응답이 `{code, message}` 외 내부 정보를 내보내지 않는가? 계정 열거가 가능한 구분된 응답이 없는가?

### E. 설정·시크릿
- 시크릿·API 키가 코드에 하드코딩되지 않았는가?
- 쿠키 속성(`CookieFactory`/`CookieProperties`)이 `HttpOnly`·`Secure`·`SameSite` 를 유지하는가?
- CORS 가 `*` + credentials 조합으로 열려있지 않은가? actuator·swagger 가 운영 프로필에서 노출되지 않는가?

---

## 리포트 형식
- **확실한 위반만** 보고한다 (오탐 0 목표). 애매하면 "확인 필요"로 분리.
- 각 항목: `[보안] 파일:라인` · **공격 시나리오 한 줄**(누가 무엇을 보낼 때 무엇이 뚫리는가) · 제안 수정.
  시나리오를 못 쓰면 위반이 아니라 ❓ 다.
- 심각도: **🔴** 인가 우회·타인 데이터 접근·사칭·시크릿 노출 > **🟡** 과다 노출·로그 위생·설정 약화 > **🟢** 방어 강화.
- 토큰·비밀키·개인정보 **실값을 리포트에 옮기지 않는다** (파일:라인으로만 가리킨다).
- 끝에 한 줄 총평.
