#!/usr/bin/env python3
"""
MVC 아키텍처 검증기 + 전환기 디스패처.

전환은 하루에 끝나지 않는다. 파일럿 컨텍스트가 MVC 로 옮겨간 시점에도
나머지 13개는 여전히 헥사고날이다. 이때 검증기를 통째로 갈아치우면
아직 안 옮긴 컨텍스트가 무방비가 되고, 반대로 그대로 두면 옮긴 컨텍스트가
매 편집마다 위반으로 차단된다. 그래서 이 훅은 **컨텍스트별로 어느 규칙을
적용할지 상태 파일을 보고 갈라준다.**

상태 파일: .claude/mvc-migration-state.json
    {
      "mode": "migrating",              # migrating | done | off
      "migrated": ["club", "report"],   # MVC 규칙을 적용할 컨텍스트
      "pilot": "club"
    }

  mode=migrating — migrated 목록의 컨텍스트는 MVC 규칙, 나머지는 헥사고날 규칙
                   (posttooluse-validate-arch.py 로 위임)
  mode=done      — 전 컨텍스트 MVC 규칙. 헥사고날 검증기는 더 호출하지 않는다
  mode=off       — 검증 없음 (탈출구. 상태 파일이 없을 때의 기본값도 아님 — 아래 참조)

상태 파일이 아예 없으면 헥사고날로 간주해 전부 위임한다. 하네스를 설치만
하고 전환을 시작하지 않은 저장소가 갑자기 규칙이 바뀌면 안 되기 때문이다.

검사 항목 (전부 ERROR=block):
  1. 계층 역류 — repository→service/controller, service→controller
  2. 엔티티 노출 — controller 가 entity import (MVC 에서 가장 흔한 붕괴 지점)
  3. cross-context 접근면 — 남의 repository/controller 직접 접근 금지
  4. 잔재 — 옮긴 컨텍스트에 UseCase/Port/Adapter 패키지가 남아있음
  5. 예외 삼킴 — catch (Exception ignored) {}

순환 의존은 파일 하나만 보고는 판정할 수 없다 → --scan 모드에서만 검사한다.
"""
import json
import os
import re
import sys

BASE = "com.back.catchmate"
SHARED = {"common", "global"}

PKG_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
IMP_RE = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)\s*;")
SWALLOW = re.compile(r"catch\s*\(\s*[\w.]*Exception\s+ignored\s*\)\s*\{\s*\}")

# 옮긴 컨텍스트에 남아있으면 안 되는 헥사고날 잔재 최상위 계층
LEGACY_LAYERS = ("adapter", "application", "domain")

# MVC 3계층에서 인정하는 레이어 (패키지 첫 segment)
LAYERS = ("controller", "service", "repository", "entity", "dto", "event", "config", "exception")


def project_dir(data=None):
    return (
        os.environ.get("CLAUDE_PROJECT_DIR")
        or (data or {}).get("cwd")
        or os.getcwd()
    )


def load_state(pdir):
    fp = os.path.join(pdir, ".claude", "mvc-migration-state.json")
    try:
        with open(fp, encoding="utf-8") as fh:
            st = json.load(fh)
    except (OSError, ValueError):
        return {"mode": "absent", "migrated": set()}
    return {
        "mode": st.get("mode", "migrating"),
        "migrated": set(st.get("migrated") or []),
    }


def discover_contexts(pdir):
    root = os.path.join(pdir, "src", "main", "java", *BASE.split("."))
    try:
        return {
            n for n in os.listdir(root)
            if os.path.isdir(os.path.join(root, n)) and n not in SHARED
        }
    except OSError:
        return set()


def split_pkg(pkg):
    """패키지에서 (컨텍스트, 레이어, 나머지) 를 뽑는다."""
    if not pkg.startswith(BASE + "."):
        return None, None, ""
    rest = pkg[len(BASE) + 1:]
    parts = rest.split(".")
    ctx = parts[0] if parts else None
    layer = parts[1] if len(parts) > 1 else ""
    return ctx, layer, ".".join(parts[1:])


def check_mvc(pkg, imports, content, contexts, migrated):
    """MVC 규칙 위반 목록을 돌려준다. 각 항목은 사람이 읽고 바로 고칠 수 있는 문장."""
    ctx, layer, rest = split_pkg(pkg)
    if ctx is None or ctx in SHARED:
        return []
    violations = []

    for lineno, imp in imports:
        if not imp.startswith(BASE + "."):
            continue
        ictx, ilayer, irest = split_pkg(imp)
        if ictx is None or ictx in SHARED:
            continue

        if ictx == ctx:
            # --- 같은 컨텍스트 안: 계층 방향 검사 ---
            if layer == "repository" and ilayer in ("service", "controller"):
                violations.append(
                    "L%d: repository 가 %s 를 import — 계층 역류. "
                    "repository 는 entity 와 dto 만 알아야 한다. (%s)"
                    % (lineno, ilayer, imp)
                )
            elif layer == "service" and ilayer == "controller":
                violations.append(
                    "L%d: service 가 controller 를 import — 계층 역류. "
                    "요청/응답 타입이 필요하면 dto 로 옮겨라. (%s)" % (lineno, imp)
                )
            elif layer == "entity" and ilayer in ("service", "controller", "repository"):
                violations.append(
                    "L%d: entity 가 %s 를 import — 엔티티는 아무 계층도 몰라야 한다. (%s)"
                    % (lineno, ilayer, imp)
                )
            elif layer == "controller" and ilayer == "entity":
                violations.append(
                    "L%d: controller 가 entity 를 직접 import — 엔티티가 HTTP 경계로 샌다. "
                    "dto 로 변환해서 주고받아라. (%s)" % (lineno, imp)
                )
            elif layer == "controller" and ilayer == "repository":
                violations.append(
                    "L%d: controller 가 repository 를 직접 import — service 를 건너뛴다. "
                    "트랜잭션 경계가 사라진다. (%s)" % (lineno, imp)
                )
            continue

        # --- 다른 컨텍스트: 접근면 검사 ---
        # 옮기지 않은 컨텍스트를 참조하는 것은 전환 과도기에 정상이다.
        if ictx not in migrated:
            continue
        if ilayer == "repository":
            violations.append(
                "L%d: 다른 컨텍스트(%s)의 repository 를 직접 import — 남의 트랜잭션·"
                "soft delete 규칙을 우회한다. %sService 를 주입해라. (%s)"
                % (lineno, ictx, ictx.capitalize(), imp)
            )
        elif ilayer == "controller":
            violations.append(
                "L%d: 다른 컨텍스트(%s)의 controller 를 import — HTTP 계층을 가로질렀다. "
                "service 를 호출해라. (%s)" % (lineno, ictx, imp)
            )

    # --- 옮긴 컨텍스트에 남은 헥사고날 잔재 ---
    if ctx in migrated:
        if layer in LEGACY_LAYERS:
            violations.append(
                "패키지 %s 가 헥사고날 잔재다. %s 는 이미 MVC 로 전환된 컨텍스트이므로 "
                "이 파일은 %s/{controller|service|repository|entity|dto} 아래로 옮겨야 한다."
                % (rest, ctx, ctx)
            )
        elif layer and layer not in LAYERS:
            violations.append(
                "패키지 계층 '%s' 는 MVC 3계층에 없는 이름이다. 허용: %s"
                % (layer, ", ".join(LAYERS))
            )

    if SWALLOW.search(content):
        violations.append(
            "catch (Exception ignored) {} — 예외 삼킴 금지. 로깅하거나 BaseException 으로 변환해라."
        )

    return violations


_LEGACY = {}


def load_legacy(pdir):
    """헥사고날 검증기를 in-process 로 로드한다.

    subprocess 로 통째로 위임하면 그 검증기의 cross-context 규칙이
    "상대는 아직 헥사고날" 을 전제하므로, 이미 MVC 로 옮겨진 컨텍스트를 향한
    import 를 전부 위반으로 막는다. 전환 중에는 그게 정상 코드다.
    → 모듈을 직접 불러 **import 목록을 걸러서** 넘긴다.
    """
    if "mod" in _LEGACY:
        return _LEGACY["mod"]
    fp = os.path.join(pdir, ".claude", "hooks", "posttooluse-validate-arch.py")
    mod = None
    if os.path.exists(fp):
        try:
            import importlib.util
            spec = importlib.util.spec_from_file_location("hexcheck", fp)
            mod = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(mod)
        except Exception:
            mod = None
    _LEGACY["mod"] = mod
    return mod


# 전환된 컨텍스트를 향해 열어주는 접근면. 나머지(repository·controller)는 계속 막는다.
MIGRATED_SURFACE = ("service", "dto", "entity", "event")


def check_transitional(pkg, imports, content, contexts, migrated, legacy):
    """아직 안 옮긴 컨텍스트의 파일 — 헥사고날 규칙 + 전환된 상대에 대한 예외.

    이 파일 자체는 여전히 헥사고날이므로 자기 구조는 옛 규칙으로 본다.
    다만 **이미 MVC 로 옮겨진 컨텍스트를 향한 import** 만은 옛 규칙에서 빼고
    MVC 접근면 규칙으로 따로 판정한다.
    """
    ctx, layer, _ = split_pkg(pkg)
    to_migrated = []
    rest = []
    for lineno, imp in imports:
        ictx, ilayer, _ = split_pkg(imp)
        if ictx and ictx != ctx and ictx in migrated and ictx not in SHARED:
            to_migrated.append((lineno, imp, ictx, ilayer))
        else:
            rest.append((lineno, imp))

    violations = []
    if legacy is not None:
        try:
            violations = list(legacy.check(pkg, rest, content, contexts))
        except Exception:
            violations = []

    # 전환된 상대를 부르는 것은 adapter/out/external 과 이벤트 리스너에서만.
    # 이 파일은 아직 헥사고날이므로 자기 FetchAdapter 를 창구로 써야 한다.
    for lineno, imp, ictx, ilayer in to_migrated:
        norm = pkg[len(BASE) + 1:] if pkg.startswith(BASE + ".") else ""
        in_adapter = ".adapter.out.external" in "." + norm or ".adapter.in.event" in "." + norm
        if not in_adapter:
            violations.append(
                "L%d: '%s' 가 이미 MVC 로 전환된 '%s' 를 직접 import — 이 컨텍스트는 아직 "
                "헥사고날이므로 자기 adapter/out/external 의 FetchAdapter 를 창구로 써야 한다. (%s)"
                % (lineno, ctx, ictx, imp)
            )
        elif ilayer not in MIGRATED_SURFACE:
            violations.append(
                "L%d: 전환된 컨텍스트 '%s' 의 %s 는 접근면이 아니다 — service·dto·entity·event "
                "만 허용된다. (%s)" % (lineno, ictx, ilayer or "(루트)", imp)
            )
    return violations


def find_cycles_scan(pdir, migrated):
    """--scan 전용: 옮긴 컨텍스트 사이의 Service 주입 순환을 검사한다.

    파일 하나만 보는 훅으로는 순환을 볼 수 없다. 순환은 전체 그래프의 성질이라
    배치 검사(빌드 게이트)에서만 판정한다.
    """
    from collections import defaultdict

    adj = defaultdict(set)
    src = os.path.join(pdir, "src", "main", "java", *BASE.split("."))
    for ctx in sorted(migrated):
        base = os.path.join(src, ctx)
        if not os.path.isdir(base):
            continue
        for dirpath, _, files in os.walk(base):
            for fn in files:
                if not fn.endswith(".java"):
                    continue
                fp = os.path.join(dirpath, fn)
                try:
                    with open(fp, encoding="utf-8", errors="replace") as fh:
                        text = fh.read()
                except OSError:
                    continue
                # Service 를 필드로 주입한 경우만 순환 후보다.
                for m in IMP_RE.finditer(text):
                    imp = m.group(1)
                    ictx, ilayer, _ = split_pkg(imp)
                    if not ictx or ictx == ctx or ictx in SHARED:
                        continue
                    if ilayer != "service" or ictx not in migrated:
                        continue
                    sym = imp.rsplit(".", 1)[-1]
                    if re.search(r"private\s+final\s+" + re.escape(sym) + r"\s+\w+\s*;", text):
                        adj[ctx].add(ictx)

    # 작은 그래프이므로 단순 DFS 로 순환 경로를 찾는다.
    cycles = []
    seen_pairs = set()
    for start in sorted(adj):
        stack = [(start, [start])]
        while stack:
            node, path = stack.pop()
            for nxt in sorted(adj.get(node, ())):
                if nxt == start:
                    key = tuple(sorted(path))
                    if key not in seen_pairs:
                        seen_pairs.add(key)
                        cycles.append(path + [start])
                elif nxt not in path:
                    stack.append((nxt, path + [nxt]))
    return cycles


def scan_all(pdir):
    state = load_state(pdir)
    mode = state["mode"]
    contexts = discover_contexts(pdir)

    if mode == "off":
        print("[mvc-check] mode=off — 검사 건너뜀.")
        return 0
    if mode == "absent":
        migrated = set()
    elif mode == "done":
        migrated = set(contexts)
    else:
        migrated = state["migrated"]

    legacy = load_legacy(pdir)
    if legacy is None and migrated != set(contexts):
        print("[mvc-check] ⚠️ 헥사고날 검증기를 불러올 수 없어 미전환 컨텍스트를 검사하지 못했습니다.")

    total = 0
    files_with_issues = 0
    src = os.path.join(pdir, "src", "main", "java", *BASE.split("."))
    for ctx in sorted(contexts):
        base = os.path.join(src, ctx)
        if not os.path.isdir(base):
            continue
        is_migrated = ctx in migrated
        for dirpath, _, files in os.walk(base):
            for fn in sorted(files):
                if not fn.endswith(".java"):
                    continue
                fp = os.path.join(dirpath, fn)
                try:
                    with open(fp, encoding="utf-8", errors="replace") as fh:
                        content = fh.read()
                except OSError:
                    continue
                m = PKG_RE.search(content)
                if not m:
                    continue
                imports = [
                    (i, im.group(1))
                    for i, ln in enumerate(content.splitlines(), 1)
                    if (im := IMP_RE.match(ln))
                ]
                if is_migrated:
                    vio = check_mvc(m.group(1), imports, content, contexts, migrated)
                else:
                    vio = check_transitional(
                        m.group(1), imports, content, contexts, migrated, legacy)
                if vio:
                    files_with_issues += 1
                    total += len(vio)
                    print("\n✗ %s" % os.path.relpath(fp, pdir))
                    for v in vio:
                        print("    - %s" % v)

    cycles = find_cycles_scan(pdir, migrated)
    if cycles:
        print("\n✗ Service 주입 순환 — Spring 부팅이 실패한다:")
        for cyc in cycles:
            print("    - %s" % " → ".join(cyc))
        total += len(cycles)

    if total:
        print("\n[mvc-check] 위반 %d건 (%d개 파일). "
              "전환 %d/%d 컨텍스트. 규칙: 전환분은 ondemand-rules/mvc-architecture.md, "
              "미전환분은 backend-architecture.md"
              % (total, files_with_issues, len(migrated), len(contexts)))
        return 1
    print("[mvc-check] 통과 — 전환 %d/%d 컨텍스트, 위반 없음."
          % (len(migrated), len(contexts)))
    return 0


def main():
    if "--scan" in sys.argv:
        sys.exit(scan_all(project_dir()))

    raw = sys.stdin.read()
    try:
        data = json.loads(raw)
    except Exception:
        sys.exit(0)

    tool_input = data.get("tool_input") or {}
    path = tool_input.get("file_path") or tool_input.get("path") or ""
    if not isinstance(path, str) or not path.endswith(".java"):
        sys.exit(0)
    norm = path.replace("\\", "/")
    if "/src/main/java/" not in norm or "/build/" in norm:
        sys.exit(0)

    pdir = project_dir(data)
    state = load_state(pdir)
    mode = state["mode"]

    if mode == "off":
        sys.exit(0)

    try:
        with open(path, encoding="utf-8") as fh:
            content = fh.read()
    except OSError:
        sys.exit(0)

    m = PKG_RE.search(content)
    if not m or not m.group(1).startswith(BASE):
        sys.exit(0)
    pkg = m.group(1)
    ctx, _, _ = split_pkg(pkg)

    contexts = discover_contexts(pdir)
    if mode == "done":
        migrated = contexts
    elif mode == "absent":
        migrated = set()
    else:
        migrated = state["migrated"]

    imports = [
        (i, im.group(1))
        for i, ln in enumerate(content.splitlines(), 1)
        if (im := IMP_RE.match(ln))
    ]

    if ctx in migrated:
        violations = check_mvc(pkg, imports, content, contexts, migrated)
        rule = ("이 컨텍스트(%s)는 이미 MVC 로 전환됐으므로 헥사고날이 아니라 "
                "MVC 규칙(.claude/ondemand-rules/mvc-architecture.md)이 적용됩니다" % ctx)
    else:
        # 아직 안 옮긴 컨텍스트 — 헥사고날 규칙 + 전환된 상대에 대한 예외.
        violations = check_transitional(
            pkg, imports, content, contexts, migrated, load_legacy(pdir))
        rule = ("이 컨텍스트(%s)는 아직 헥사고날이므로 기존 규칙"
                "(.claude/ondemand-rules/backend-architecture.md)이 적용됩니다" % ctx)

    if not violations:
        sys.exit(0)

    reason = (
        "[MVC 검증 훅] %s 에서 아키텍처 규칙 위반 %d건. %s:\n\n - %s"
        % (os.path.basename(path), len(violations), rule, "\n - ".join(violations))
    )
    print(json.dumps({"decision": "block", "reason": reason}))
    sys.exit(0)


if __name__ == "__main__":
    main()
