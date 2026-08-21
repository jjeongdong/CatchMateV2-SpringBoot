---
name: mvc-guardrail-switch
description: >-
  헥사고날을 강제하던 이 저장소의 가드레일 5종(PostToolUse 검증 훅 · gradle archCheck ·
  Stop 빌드 게이트 · PreToolUse 룰 주입 · SessionStart 절대금지 주입)을 **MVC 규칙으로 갈아끼운다.**
  전환은 하루에 안 끝나므로 컨텍스트별 상태 파일(.claude/mvc-migration-state.json)을 기준으로
  옮긴 컨텍스트엔 MVC 규칙을, 안 옮긴 컨텍스트엔 기존 헥사고날 규칙을 적용하는 이중 모드로 둔다.
  다음 상황에 사용한다 — "가드레일 바꿔줘", "훅이 MVC 를 막아", "archCheck 가 계속 실패해",
  "전환 모드 켜줘", "{컨텍스트} 전환 완료 표시", 마이그레이션 시작 직전과 각 컨텍스트 전환 직후,
  전체 전환 완료 후 헥사고날 자산 정리. mvc-migration 오케스트레이터가 Phase 1 과 각 라운드
  끝에서 호출한다.
---

# 가드레일 전환

이 저장소는 헥사고날을 **5중으로 강제**한다. 손대지 않고 MVC 로 옮기려 하면 첫 편집부터 막힌다.

| 장치 | 파일 | MVC 편집 시 |
|---|---|---|
| PostToolUse 검증 훅 | `posttooluse-validate-arch.py` | 편집을 `decision: block` 으로 차단 |
| gradle `archCheck` (`check` 의존) | `build.gradle:138` | 빌드 실패 |
| Stop 빌드 게이트 | `stop-build-gate.py` | 턴 종료마다 실패 |
| PreToolUse 룰 주입 | `pretooluse-inject-rules.py` | 헥사고날 규칙을 컨텍스트에 주입 |
| SessionStart 절대금지 주입 | `sessionstart-guardrails.py` | 매 세션 헥사고날 전제 주입 |

**끄지 않는다.** 파일럿 하나를 옮긴 시점에도 나머지 13개는 여전히 헥사고날이다. 전부 끄면
아직 안 옮긴 컨텍스트가 무방비가 되고, 그대로 두면 옮긴 컨텍스트가 매번 차단된다.
그래서 **컨텍스트별로 갈라주는 이중 모드**로 둔다.

---

## 상태 파일이 전환의 스위치다

`.claude/mvc-migration-state.json` 하나가 모든 장치의 기준점이다.

```json
{
  "mode": "migrating",
  "migrated": ["club"],
  "pilot": "club",
  "note": "migrated 에 올라간 컨텍스트만 MVC 규칙. 나머지는 헥사고날 규칙 유지."
}
```

| `mode` | 동작 |
|---|---|
| 파일 없음 | 전부 헥사고날. 하네스를 설치만 하고 전환을 시작하지 않은 상태 |
| `migrating` | `migrated` 목록은 MVC 규칙, 나머지는 헥사고날 검증기로 위임 |
| `done` | 전 컨텍스트 MVC. 헥사고날 검증기를 더 호출하지 않는다 |
| `off` | 검증 없음. **탈출구다 — 오래 켜두지 마라** |

컨텍스트 하나를 다 옮겼으면 `migrated` 에 이름을 추가한다. 그 순간부터 그 컨텍스트는
MVC 규칙으로 검사받고, 다른 컨텍스트가 그 컨텍스트의 `repository`·`controller` 를 직접
import 하는 것도 막힌다.

### 미전환 컨텍스트가 전환된 컨텍스트를 부를 때

전환 라운드의 대부분은 **옮긴 컨텍스트가 아니라 그것을 부르던 컨텍스트를 고치는 일**이다.
club 하나를 옮기면 아홉 개 컨텍스트의 FetchAdapter 가 `ClubService` 를 주입하도록 바뀐다.
이 아홉은 아직 헥사고날이다.

그래서 검증기는 **편집 파일의 컨텍스트와 import 대상 컨텍스트를 따로 본다:**

| 편집 파일 | import 대상 | 판정 |
|---|---|---|
| 미전환 `adapter/out/external` | 전환된 ctx 의 `service`·`dto`·`entity`·`event` | ✅ 허용 |
| 미전환 `adapter/out/external` | 전환된 ctx 의 `repository`·`controller` | ❌ 접근면 아님 |
| 미전환 `application/service` | 전환된 ctx 의 무엇이든 | ❌ 자기 FetchAdapter 를 창구로 써야 함 |
| 미전환 어디든 | **미전환** ctx | 기존 헥사고날 규칙 그대로 |

미전환 컨텍스트는 여전히 자기 `FetchPort`/`FetchAdapter` 를 창구로 유지한다. 어댑터 **안의
주입 타입과 반환 타입만** 새 MVC 타입으로 갈아끼우는 것이지, 어댑터를 걷어내는 게 아니다.
어댑터 제거는 그 컨텍스트 자신의 라운드에서 한다.

> 이 규칙이 없으면 헥사고날 검증기가 `club.service.ClubService` import 를 전부 위반으로
> 막아 라운드가 시작조차 되지 않는다. club 파일럿 계획 단계에서 발견해 반영했다.

---

## Step 1 — 전환 시작 (마이그레이션 최초 1회)

### 1-1. 상태 파일 생성

```bash
cat > .claude/mvc-migration-state.json <<'JSON'
{
  "mode": "migrating",
  "migrated": [],
  "pilot": "club"
}
JSON
```

`migrated` 는 비워서 시작한다. 아직 옮긴 것이 없으므로 이 시점의 동작은 **전부 헥사고날**과
같다. 안전하다.

### 1-2. PostToolUse 훅을 디스패처로 교체

`.claude/settings.json` 의 PostToolUse command 를 바꾼다:

```
.claude/hooks/posttooluse-validate-arch.py  →  .claude/hooks/mvc-validate-arch.py
```

`mvc-validate-arch.py` 는 상태 파일을 보고 **안 옮긴 컨텍스트의 파일이면 기존 검증기를
subprocess 로 호출해 위임**한다. 기존 검증기 파일은 지우지 않는다 — 전환이 끝날 때까지 계속 쓴다.

### 1-3. gradle archCheck 를 디스패처로 교체

`build.gradle:142` 의 스크립트 경로를 바꾼다:

```groovy
def script = file('.claude/hooks/mvc-validate-arch.py')
```

`--scan` 인터페이스와 종료 코드 규약이 동일하므로 이 한 줄이면 된다. Stop 빌드 게이트는
`./gradlew compileJava archCheck` 를 부르므로 **자동으로 함께 전환된다** — 따로 고칠 것이 없다.

### 1-4. 확인

```bash
python3 .claude/hooks/mvc-validate-arch.py --scan   # "위임" 메시지 + 헥사고날 통과가 나와야 정상
./gradlew archCheck
```

---

## Step 2 — 컨텍스트 하나를 옮긴 직후

```bash
python3 - <<'PY'
import json, pathlib
p = pathlib.Path('.claude/mvc-migration-state.json')
st = json.loads(p.read_text())
ctx = 'club'                      # ← 방금 옮긴 컨텍스트
if ctx not in st['migrated']:
    st['migrated'].append(ctx)
p.write_text(json.dumps(st, ensure_ascii=False, indent=2) + '\n')
print(st)
PY

python3 .claude/hooks/mvc-validate-arch.py --scan   # 이제 이 컨텍스트는 MVC 규칙으로 검사된다
```

**이 순서를 지켜라.** 옮기기 전에 `migrated` 에 올리면 아직 헥사고날인 파일이 전부 위반으로
잡혀 노이즈만 쏟아진다. 옮기고 나서 올린다.

---

## Step 3 — 룰 주입 전환

`pretooluse-inject-rules.py` 는 `.claude/ondemand-rules/*.md` 3개를 세션당 1회 주입한다.
파일 목록이 스크립트 안에 하드코딩(`RULE_FILES`)돼 있다.

전환 중에는 **MVC 룰을 추가**하되 헥사고날 룰을 빼지 않는다. 두 규칙이 공존하는 기간이므로
에이전트가 둘 다 알아야 한다. `RULE_FILES` 에 `mvc-architecture.md` 를 추가하고, 그 파일 상단이
"어느 컨텍스트에 어느 규칙이 적용되는지" 를 먼저 설명하게 한다.

`sessionstart-guardrails.py` 의 `GUARDRAILS` 텍스트는 **거의 그대로 둔다.** 절대 금지 3개
(Outbox 2단계 · Redis 퍼블리셔 분리 · soft delete)는 아키텍처가 아니라 런타임 동작이라
MVC 로 가도 그대로 유효하다. 다만 문구 중 "헥사고날" 을 전제하는 표현이 있으면 다듬고,
전환 중이라는 사실 한 줄을 덧붙인다.

---

## Step 4 — 전환 완료 후 정리

**전 컨텍스트가 `migrated` 에 올라간 뒤에만** 한다.

1. `mode` 를 `"done"` 으로 바꾼다. 이제 헥사고날 검증기를 호출하지 않는다.
2. `./gradlew archCheck` 와 전체 테스트를 돌려 통과를 확인한다.
3. 확인된 뒤 헥사고날 자산을 정리한다:
   - `.claude/hooks/posttooluse-validate-arch.py` — 삭제
   - `.claude/hooks/arch-audit.py` — 삭제
   - `.claude/ondemand-rules/backend-architecture.md` — MVC 판으로 대체
   - `.claude/agents/hexagonal-reviewer.md` → `mvc-reviewer.md` 로 재작성
   - `.claude/skills/hexagonal-review/` → `mvc-review/` 로 재작성
   - `.claude/skills/cross-context-access/` — 삭제 (FetchPort 체인이 없어졌다)
   - `.claude/skills/review-board/` — 아키텍처 축 참조를 새 이름으로 갱신
   - `CLAUDE.md` — 아키텍처 절 전면 개정
4. 마이그레이션 하네스 자체(`mvc-migration`·`hexagonal-to-mvc-mapping`·`mvc-converter` 등)도
   역할이 끝났으므로 삭제한다. `dep-graph.py` 는 순환 감시용으로 **남겨둘 가치가 있다** —
   `mvc-review` 스킬로 옮긴다.

**3단계는 되돌리기 어렵다.** 전환 결과에 확신이 서기 전에는 하지 마라. 2번이 통과하고
실제로 배포해서 며칠 굴려본 뒤에 해도 늦지 않다.

---

## 막혔을 때

**"훅이 계속 차단한다"** — 상태 파일의 `migrated` 에 그 컨텍스트가 있는지 확인한다. 있는데도
막히면 `python3 .claude/hooks/mvc-validate-arch.py --scan` 을 직접 돌려 어떤 규칙에 걸리는지 본다.
대개 진짜 위반이다.

**"급하게 진행해야 한다"** — `mode` 를 `"off"` 로 두면 검증이 전부 꺼진다. 다만 이 상태에서
쌓인 위반은 나중에 한꺼번에 갚아야 하고, 그 시점엔 어느 편집이 원인인지 알 수 없다.
쓰더라도 **한 턴 안에서 켜고 끈다.**

**"되돌리고 싶다"** — 상태 파일을 지우고 `settings.json`·`build.gradle` 의 두 경로를
`posttooluse-validate-arch.py` 로 되돌리면 완전히 원상복구된다. Step 4 를 하기 전이라면
헥사고날 자산이 전부 살아있다.
