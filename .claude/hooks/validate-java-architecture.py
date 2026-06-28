#!/usr/bin/env python3
"""
PostToolUse 훅: Edit/Write/MultiEdit 로 .java 파일을 수정/생성한 직후,
헥사고날 아키텍처 + 0-import 규칙(SSOT: .claude/ondemand-rules/*.md)을
정적으로 검증한다. 위반이 있으면 decision=block 으로 사유를 돌려보내
같은 턴에서 Claude 가 즉시 고치도록 유도한다.

설계 의도:
  - inject-java-rules.py 가 규칙을 "주입"한다면, 이 훅은 규칙을 "검출"한다.
  - import 방향 + 패키지 경로만으로 결정론적으로 판단 가능한 항목만 검사한다
    (애매한 의미론적 규칙은 사람/리뷰에 맡긴다 → 오탐 0 목표).
  - src/main/java 의 com.back.catchmate.* 만 대상. 테스트/생성코드는 건너뛴다.

검사 항목 (전부 ERROR=block):
  1. domain 이 Spring/JPA/Jackson import       (도메인 순수성)
  2. domain 이 자기 application/adapter import   (의존성 역류)
  3. web/websocket 이 자기 application.service   (정문 우회: 구현체 직주입)
  4. application.service 가 자기 adapter import   (안→밖 역류)
  5. persistence 가 자기 application.service / adapter.in (역류)
  6. cross-context import (다른 컨텍스트) — Fetch Port 우회.
     adapter/out/external·adapter/in/event 만 상대 port.in/dto/event 허용.
  7. catch (Exception ignored) {}                (예외 삼킴 금지)
"""
import json
import os
import re
import sys

BASE = "com.back.catchmate"
# 공유 패키지: 어디서나 import 허용 (컨텍스트 아님)
SHARED = {"common", "global"}

INFRA_IN_DOMAIN = re.compile(
    r"^(org\.springframework|jakarta\.persistence|javax\.persistence|com\.fasterxml\.jackson)\."
)
SWALLOW = re.compile(r"catch\s*\(\s*[\w.]*Exception\s+ignored\s*\)\s*\{\s*\}")
PKG_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
IMP_RE = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)\s*;")


def discover_contexts(project_dir: str) -> set:
    """src/main/java/com/back/catchmate 하위 디렉터리 = 컨텍스트 목록."""
    root = os.path.join(project_dir, "src", "main", "java", *BASE.split("."))
    try:
        return {
            n for n in os.listdir(root)
            if os.path.isdir(os.path.join(root, n)) and n not in SHARED
        }
    except OSError:
        # 디스커버리 실패 시 정적 목록으로 폴백.
        return {
            "admin", "auth", "board", "bookmark", "chat", "club", "enroll",
            "game", "inquiry", "notice", "notification", "oauth", "report", "user",
        }


def layer_of(pkg: str, ctx: str) -> str:
    """패키지에서 컨텍스트를 뺀 나머지로 레이어를 식별."""
    rest = pkg[len(BASE) + 1 + len(ctx):].lstrip(".") if ctx else ""
    if rest.startswith("adapter.in.web"):
        return "web"
    if rest.startswith("adapter.in.websocket"):
        return "websocket"
    if rest.startswith("adapter.in.event"):
        return "event-listener"
    if rest.startswith("adapter.out.persistence"):
        return "persistence"
    if rest.startswith("adapter.out.external"):
        return "external"
    if rest.startswith("adapter"):
        return "adapter-out"
    if rest.startswith("application.service"):
        return "service"
    if rest.startswith("application.port.in"):
        return "port-in"
    if rest.startswith("application.port.out"):
        return "port-out"
    if rest.startswith("application.dto"):
        return "dto"
    if rest.startswith("application.event"):
        return "app-event"
    if rest.startswith("domain"):
        return "domain"
    return "other"


def context_of(imp: str, contexts: set):
    """import 의 소속 컨텍스트(또는 shared)를 반환. 프로젝트 밖이면 None."""
    if not imp.startswith(BASE + "."):
        return None
    seg = imp[len(BASE) + 1:].split(".", 1)[0]
    if seg in SHARED:
        return "shared"
    if seg in contexts:
        return seg
    return None


def check(pkg: str, imports, content, contexts):
    """위반 리스트(문자열) 반환."""
    if not pkg.startswith(BASE):
        return []
    ctx_seg = pkg[len(BASE) + 1:].split(".", 1)[0] if pkg != BASE else ""

    # global = 컴포지션 루트(config 와이어링·scheduler·AOP) → 어댑터/컨텍스트 참조 정상.
    # 구조/cross-context 검사를 건너뛰고 예외 삼킴(#7)만 본다.
    if ctx_seg == "global":
        return _check_swallow(content)
    # common = leaf 여야 함 → 어떤 컨텍스트도 import 금지(자기 common 제외).
    if ctx_seg == "common":
        out = []
        for line, imp in imports:
            if imp.startswith(BASE + ".") and not imp.startswith(BASE + ".common."):
                out.append("L%d  common 격리 위반: common 이 %s 를 import. "
                           "common 은 leaf — 다른 패키지에 의존 금지. (architecture 의존성 방향)"
                           % (line, imp))
        return out + _check_swallow(content)

    is_shared = ctx_seg == ""  # base 직속 파일(예: CatchmateApplication)
    ctx = "" if is_shared else ctx_seg
    layer = layer_of(pkg, ctx) if not is_shared else "shared"

    out = []
    for line, imp in imports:
        own = (not is_shared) and imp.startswith("%s.%s." % (BASE, ctx))
        target = context_of(imp, contexts)

        # 1. 도메인 순수성
        if layer == "domain" and INFRA_IN_DOMAIN.match(imp):
            out.append("L%d  도메인 순수성 위반: domain 패키지가 인프라(%s)를 import. "
                       "domain/* 은 순수 Java(+Lombok)만. (conventions #8)" % (line, imp))
            continue

        # 2. 도메인 의존성 역류 (자기 application/adapter)
        if layer == "domain" and own and (
            (".application." in imp) or (".adapter." in imp)
        ):
            out.append("L%d  의존성 역류: domain 이 자기 application/adapter(%s)를 import. "
                       "domain 은 안쪽이라 바깥을 모름. (architecture 의존성 방향)" % (line, imp))
            continue

        # 3. web/websocket → 자기 application.service 구현체
        if layer in ("web", "websocket") and own and ".application.service." in imp:
            out.append("L%d  정문 우회: Controller/WebSocket 이 service 구현체(%s)를 import. "
                       "application.port.in 의 UseCase 인터페이스로 진입. (architecture #1·#5)" % (line, imp))
            continue

        # 4. application.service → 자기 adapter
        if layer == "service" and own and ".adapter." in imp:
            out.append("L%d  의존성 역류: service 가 자기 adapter(%s)를 import. "
                       "service 는 port(out)만 알아야 함. (architecture #4)" % (line, imp))
            continue

        # 5. persistence → 자기 service / adapter.in
        if layer == "persistence" and own and (
            ".application.service." in imp or ".adapter.in." in imp
        ):
            out.append("L%d  의존성 역류: persistence 어댑터가 %s 를 import. "
                       "out 어댑터는 port.out + 자기 domain.model 만. (conventions #12)" % (line, imp))
            continue

        # 6. cross-context (Fetch Port 우회)
        if target and target not in ("shared",) and not own:
            allowed_here = layer in ("external", "event-listener", "shared")
            ok = False
            if allowed_here:
                if (".application.port.in." in imp
                        or ".application.dto." in imp
                        or (layer == "event-listener" and ".application.event." in imp)):
                    ok = True
            if not ok:
                hint = ("이 레이어는 cross-context import 자체가 금지. "
                        "port/out 의 XxxFetchPort → adapter/out/external 의 FetchAdapter → "
                        "상대 application.port.in UseCase 체인으로." if not allowed_here
                        else "external/event 어댑터는 상대 port.in UseCase / dto(record) / event 만 허용 "
                             "(상대 domain·service·adapter·port.out 금지).")
                out.append("L%d  Cross-context 위반: '%s' 컨텍스트가 '%s' 의 %s 를 직접 import. %s "
                           "(architecture #3·#6, 0-import)" % (line, ctx or "shared", target, imp, hint))
                continue

    return out + _check_swallow(content)


def _check_swallow(content: str):
    """7. catch(... ignored){} 예외 삼킴 검출 (모든 레이어 공통)."""
    out = []
    for i, ln in enumerate(content.splitlines(), 1):
        if SWALLOW.search(ln):
            out.append("L%d  예외 삼킴 금지: catch(... ignored){} 패턴. "
                       "BaseException 처리 또는 Optional 사용. (conventions #4)" % i)
    return out


def main() -> None:
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)

    tool_input = data.get("tool_input") or {}
    path = tool_input.get("file_path") or tool_input.get("path") or ""
    if not isinstance(path, str) or not path.endswith(".java"):
        sys.exit(0)
    # main 소스만 검사 (테스트/생성코드 제외)
    norm = path.replace("\\", "/")
    if "/src/main/java/" not in norm or "/build/" in norm:
        sys.exit(0)

    try:
        with open(path, "r", encoding="utf-8") as fh:
            content = fh.read()
    except OSError:
        sys.exit(0)

    m = PKG_RE.search(content)
    if not m or not m.group(1).startswith(BASE):
        sys.exit(0)
    pkg = m.group(1)

    imports = []
    for i, ln in enumerate(content.splitlines(), 1):
        im = IMP_RE.match(ln)
        if im:
            imports.append((i, im.group(1)))

    project_dir = (
        os.environ.get("CLAUDE_PROJECT_DIR") or data.get("cwd") or os.getcwd()
    )
    contexts = discover_contexts(project_dir)

    violations = check(pkg, imports, content, contexts)
    if not violations:
        sys.exit(0)

    fname = os.path.basename(path)
    reason = (
        "[아키텍처 검증 훅] %s 에서 백엔드 규칙 위반 %d건 발견. "
        "수정한 코드가 헥사고날/0-import 규칙(.claude/ondemand-rules/*)을 어겼습니다. "
        "아래를 고친 뒤 진행하세요:\n\n - %s"
        % (fname, len(violations), "\n - ".join(violations))
    )
    print(json.dumps({"decision": "block", "reason": reason}))
    sys.exit(0)


if __name__ == "__main__":
    main()
