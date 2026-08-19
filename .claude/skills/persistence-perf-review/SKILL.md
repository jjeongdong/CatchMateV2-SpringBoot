---
name: persistence-perf-review
description: >-
  catchmate 백엔드를 쿼리 비용·영속성 관점에서 리뷰한다 — N+1(지연 로딩 루프)과 fetch join 누락,
  QueryDSL 쿼리 형태, 페이징·count 쿼리, 인덱스를 못 타는 검색 조건, 반복문 안의 단건 조회/저장
  (배치 미사용), 커넥션 점유 시간, Redis 왕복(단건 반복 vs multiGet/파이프라인), 대량 팬아웃의
  쿼리 증폭. 다음 상황에 사용한다 — "성능 리뷰", "N+1 있나", "쿼리 좀 봐줘", "느린 것 같아",
  Repository/QueryDSL Impl/조회 서비스/팬아웃·배치 로직을 수정한 뒤. 단발 요청이면 이 스킬만 쓰고,
  전방위 리뷰는 review-board 오케스트레이터가 호출한다.
---

# Persistence / Performance Review (쿼리 비용)

이 스킬은 **"이 코드가 요청 1건에 DB·Redis 를 몇 번 때리는가"** 만 본다. 트랜잭션 경계·락·커밋
타이밍은 `concurrency-review`, 구조·계약은 `hexagonal-review` 담당이다.

판정 기준은 "느려 보인다"가 아니라 **비용을 수식으로 쓸 수 있는가**다. 각 지적에는 반드시
`요청 1회 → 쿼리 1 + N회 (N = 채팅방 인원)` 같은 **크기**가 붙어야 한다. 크기를 못 쓰면 지적하지 않는다.

## 0. 스코프 잡기
1. 오케스트레이터가 준 스코프를 그대로 쓴다. 없으면 `git diff --name-only`(+ staged, 없으면 워킹트리).
2. **엔티티 매핑부터 확인한다.** 서비스 코드만 보고 N+1 을 단정하지 않는다. 연관 필드의
   `@ManyToOne`/`@OneToMany` fetch 전략과 `@SQLRestriction` 을 엔티티 파일에서 직접 읽는다.
   (현재 이 프로젝트엔 `FetchType.EAGER` 가 0건 — 전부 지연 로딩 전제.)
3. `docs/` 의 부하테스트 문서(`*loadtest*`, `*load-test*`, `write-behind*`)를 확인한다. 이미 측정·
   튜닝이 끝난 항목이면 그 결론과 어긋나는 제안을 하지 않는다.

---

## 체크리스트

### A. N+1 · 연관 로딩
- 조회 결과 리스트를 **루프 돌며 연관 객체 getter** 를 호출하지 않는가? → 1 + N 쿼리.
  (도메인 변환 `from(Entity)` 안에서 `entity.getUser().getNickName()` 을 부르는 형태가 전형적)
- 목록 API 인데 fetch join / `@EntityGraph` / 별도 in-절 조회 중 아무것도 없지 않은가?
- **컬렉션** fetch join 과 페이징을 같이 쓰지 않는가? → 하이버네이트가 전체를 메모리로 올린다
  (`HHH000104`). 페이징이 필요하면 ID 페이징 후 in-절 로딩이다.
- 여러 컬렉션을 동시에 fetch join 하지 않는가? → 카티션 곱.

### B. 쿼리 형태 (QueryDSL)
- `where` 조건이 **인덱스를 탈 수 있는 형태**인가?
  - ❌ 컬럼에 함수/연산 적용(`DATE(created_at) = ?`), 앞 와일드카드 `LIKE '%x'`, 타입 불일치 캐스팅
  - ✅ 범위 조건(`created_at >= ? AND < ?`), 좌측 접두 일치
- 복합 인덱스를 쓰는 조회라면 **선행 컬럼부터** 조건에 들어가는가? (인덱스 정의는 엔티티의
  `@Table(indexes=...)` 또는 마이그레이션에서 확인 — 없으면 "인덱스 미확인"으로 ❓ 처리)
- `count` 쿼리가 별도로 최적화됐는가? 정렬·조인이 그대로 붙은 count 는 페이지마다 풀스캔이다.
- 무한 스크롤/채팅 히스토리에서 **offset 페이징**을 쓰지 않는가? 깊은 offset 은 뒤로 갈수록 선형
  악화 → 커서(no-offset) 페이징 대상.
- `select` 가 필요한 컬럼만 뽑는가, 아니면 엔티티 전체를 끌어오는가? (목록·집계는 projection)

### C. 반복문 안의 I/O (팬아웃 증폭)
- 루프 안에서 `repository.save()` / `findById()` / Redis 단건 호출을 하지 않는가?
  → `saveAll`, in-절 조회, `multiGet`/파이프라인으로 묶을 대상. 알림·채팅 팬아웃은 인원 수만큼
  증폭되므로 이 축에서 가장 비싼 결함이다.
- 배치 저장 시 `hibernate.jdbc.batch_size` 가 실제로 먹는 형태인가? (IDENTITY 전략이면 배치 무효)
- Redis 왕복 횟수가 인원 수에 비례하지 않는가? (`RedisUserOnlineStatusMultiGet...` 처럼 이미
  multiGet 으로 개선된 선례가 있다 — 새 코드가 그 패턴을 따르는지 본다)

### D. 커넥션·자원 점유
- 트랜잭션(=커넥션 점유) 구간에 **불필요한 조회/변환/외부 대기**가 들어있지 않은가?
  (외부 호출 자체의 배치는 `concurrency-review` 담당 — 여기선 "구간이 길다"는 비용만 본다)
- 읽기 전용 조회에 `readOnly = true` 가 빠지지 않았는가? (더티체킹 스냅샷 비용)
- 인스턴스당 pool=15 (2대=30) 전제에서, 이 경로가 커넥션을 오래 쥐면 몇 rps 에서 포화되는지
  한 줄로 추정한다.

### E. 캐시 · 소프트 삭제
- 매 요청 반복 조회되는 불변에 가까운 데이터(구단·경기 등)를 캐싱 없이 매번 DB 에서 읽지 않는가?
- `@SQLRestriction("deleted_at IS NULL")` 이 걸린 엔티티에 **네이티브 쿼리**로 접근하면 필터가
  적용되지 않는다 → 삭제 데이터 노출 + 카운트 불일치. 네이티브/QueryDSL 에서 조건 누락 확인.
- 캐시를 도입하자는 제안은 **무효화 시점**을 함께 쓸 수 있을 때만 한다. 못 쓰면 ❓.

---

## 리포트 형식
- **확실한 위반만** 보고한다 (오탐 0 목표). 애매하면 "확인 필요"로 분리.
- 각 항목: `[성능] 파일:라인` · **비용 크기**(쿼리 N회 / 왕복 N회 / offset 깊이) · 제안 수정.
- 심각도: **🔴** 사용자 수·인원 수에 비례해 증폭되는 비용(팬아웃 N+1, 루프 내 단건 I/O)
  > **🟡** 고정 비용의 낭비(불필요 조회, readOnly 누락, count 미최적화) > **🟢** 개선 여지.
- 끝에 한 줄 총평. 실측 문서와 대조했으면 그 사실을 명시한다.

## 규칙 SSOT
- `.claude/ondemand-rules/backend-patterns.md` — QueryDSL 복잡 쿼리 · Soft Delete · Redis
- `.claude/ondemand-rules/backend-coding-conventions.md` #5 — 트랜잭션 경계
- 실측 근거: `docs/chat-delivery-loadtest-*.md`, `docs/chat-read-loadtest-results.md`,
  `docs/chat-read-write-behind-analysis.md`
